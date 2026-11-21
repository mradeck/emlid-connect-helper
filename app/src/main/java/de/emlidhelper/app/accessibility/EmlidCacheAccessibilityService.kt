package de.emlidhelper.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import de.emlidhelper.app.MainActivity
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicReference

enum class CacheClearOutcome {
    CLEARED,
    ALREADY_EMPTY,
    TIMED_OUT,
    SERVICE_NOT_ENABLED,
    CANCELLED
}

data class CacheAutomationTask(
    val clearCache: Boolean,
    val forceStop: Boolean,
    val armedAtMs: Long = System.currentTimeMillis(),
    val deferred: CompletableDeferred<CacheClearOutcome> = CompletableDeferred()
) {
    @Volatile
    var forceStopTriggered: Boolean = false

    @Volatile
    var forceStopConfirmed: Boolean = false

    @Volatile
    var storageOpened: Boolean = false

    @Volatile
    var cacheCleared: Boolean = false
}

object AccessibilityBridge {
    private val _isServiceConnected = MutableStateFlow(false)
    val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

    val activeTask = AtomicReference<CacheAutomationTask?>(null)

    var logCallback: ((String, Boolean) -> Unit)? = null

    fun setConnected(connected: Boolean) {
        _isServiceConnected.value = connected
    }

    fun isEnabledInSettings(context: Context): Boolean {
        if (_isServiceConnected.value) return true
        val expectedComponent = ComponentName(context, EmlidCacheAccessibilityService::class.java)
            .flattenToString()
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.split(':').any {
            it.equals(expectedComponent, ignoreCase = true) ||
                it.endsWith("/${EmlidCacheAccessibilityService::class.java.name}", ignoreCase = true)
        }
    }

    fun armTask(clearCache: Boolean, forceStop: Boolean): CacheAutomationTask {
        val task = CacheAutomationTask(clearCache = clearCache, forceStop = forceStop)
        activeTask.getAndSet(task)?.deferred?.complete(CacheClearOutcome.CANCELLED)
        return task
    }

    fun disarm() {
        activeTask.getAndSet(null)?.deferred?.complete(CacheClearOutcome.CANCELLED)
    }
}

/**
 * Strictly scoped Accessibility Service:
 * - Only receives events from com.android.settings
 * - Only acts while explicitly armed by the user pressing "Emlid Reset" (auto-expires after 18s)
 * - Hard safety guard: NEVER clicks any button containing "Daten" or "Data" ("Daten löschen" / "Clear data")
 */
class EmlidCacheAccessibilityService : AccessibilityService() {

    private var lastClickMs: Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        AccessibilityBridge.setConnected(true)
        AccessibilityBridge.logCallback?.invoke("Bedienungshilfe (Auto-Cache) verbunden", false)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        AccessibilityBridge.setConnected(false)
        return super.onUnbind(intent)
    }

    override fun onInterrupt() {
        // No-op
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val task = AccessibilityBridge.activeTask.get() ?: return
        if (task.deferred.isCompleted) {
            AccessibilityBridge.activeTask.compareAndSet(task, null)
            return
        }

        // Hard safety timeout: 18 seconds max
        if (System.currentTimeMillis() - task.armedAtMs > 18_000L) {
            completeTask(task, CacheClearOutcome.TIMED_OUT)
            return
        }

        // Small throttle between clicks so One UI animations settle cleanly
        val now = System.currentTimeMillis()
        if (now - lastClickMs < 450L) return

        val root = rootInActiveWindow ?: return

        try {
            processSettingsScreen(root, task)
        } catch (e: Exception) {
            AccessibilityBridge.logCallback?.invoke("Auto-Cache Hinweis: ${e.message}", true)
        }
    }

    private fun processSettingsScreen(root: AccessibilityNodeInfo, task: CacheAutomationTask) {
        // Step 1 (optional): Force Stop (only when Shizuku did not already force-stop)
        if (task.forceStop && !task.forceStopConfirmed) {
            // If confirmation dialog is open, tap OK / Force stop
            if (task.forceStopTriggered) {
                val confirmNode = findNodeByExactOrContains(
                    root,
                    allowedTerms = listOf("ok", "stopp erzwingen", "force stop", "beenden"),
                    forbiddenTerms = FORBIDDEN_DATA_TERMS,
                    preferButton = true
                )
                if (confirmNode != null && confirmNode.isEnabled && clickNodeOrParent(confirmNode)) {
                    lastClickMs = System.currentTimeMillis()
                    task.forceStopConfirmed = true
                    AccessibilityBridge.logCallback?.invoke("Auto-Klick: Stopp erzwingen bestätigt", false)
                    if (!task.clearCache) {
                        finishAndReturnToHelper(task, CacheClearOutcome.CLEARED)
                    }
                    return
                }
            } else {
                val forceStopNode = findNodeByExactOrContains(
                    root,
                    allowedTerms = listOf("stopp erzwingen", "force stop"),
                    forbiddenTerms = FORBIDDEN_DATA_TERMS
                )
                if (forceStopNode != null) {
                    if (!isEffectivelyEnabled(forceStopNode)) {
                        // Already force-stopped!
                        task.forceStopTriggered = true
                        task.forceStopConfirmed = true
                        AccessibilityBridge.logCallback?.invoke("Emlid Flow war bereits gestoppt", false)
                    } else if (clickNodeOrParent(forceStopNode)) {
                        lastClickMs = System.currentTimeMillis()
                        task.forceStopTriggered = true
                        AccessibilityBridge.logCallback?.invoke("Auto-Klick: Stopp erzwingen", false)
                        return
                    }
                }
            }
        }

        if (!task.clearCache) {
            if (!task.forceStop || task.forceStopConfirmed) {
                finishAndReturnToHelper(task, CacheClearOutcome.CLEARED)
            }
            return
        }

        // Step 2: Look for "Cache leeren" / "Clear cache" directly (in case Storage screen is already visible)
        val clearCacheNode = findClearCacheNodeSafely(root)
        if (clearCacheNode != null) {
            task.storageOpened = true
            if (!isEffectivelyEnabled(clearCacheNode)) {
                AccessibilityBridge.logCallback?.invoke("Cache von Emlid Flow war bereits 0 B", false)
                finishAndReturnToHelper(task, CacheClearOutcome.ALREADY_EMPTY)
                return
            }
            if (clickNodeOrParent(clearCacheNode)) {
                lastClickMs = System.currentTimeMillis()
                task.cacheCleared = true
                AccessibilityBridge.logCallback?.invoke("Auto-Klick: Cache leeren erfolgreich!", false)
                finishAndReturnToHelper(task, CacheClearOutcome.CLEARED)
                return
            }
        }

        // Step 3: Otherwise tap "Speicher" / "Storage" on Samsung One UI App Info screen
        if (!task.storageOpened) {
            val storageNode = findNodeByExactOrContains(
                root,
                allowedTerms = listOf("speicher", "storage", "speicherplatz", "speicher und cache", "storage & cache"),
                forbiddenTerms = FORBIDDEN_DATA_TERMS + listOf("daten löschen", "speicher löschen", "clear storage")
            )
            if (storageNode != null && clickNodeOrParent(storageNode)) {
                lastClickMs = System.currentTimeMillis()
                task.storageOpened = true
                AccessibilityBridge.logCallback?.invoke("Auto-Klick: Speicher geöffnet", false)
            } else {
                // Scroll down once if "Speicher" is below the fold on App Info
                scrollFirstScrollableContainer(root)
                lastClickMs = System.currentTimeMillis()
            }
        }
    }

    /**
     * Ultra-strict matcher for "Cache leeren" / "Clear cache".
     * Rejects anything that mentions "Daten", "Data", "Speicher löschen", "Clear storage".
     */
    private fun findClearCacheNodeSafely(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val candidates = mutableListOf<AccessibilityNodeInfo>()
        collectNodes(root, candidates)
        return candidates.firstOrNull { node ->
            val combined = "${node.text ?: ""} ${node.contentDescription ?: ""}".trim().lowercase()
            if (combined.isBlank()) return@firstOrNull false
            val matchesCache = combined.contains("cache leeren") || combined.contains("clear cache")
            val containsForbidden = FORBIDDEN_DATA_TERMS.any { combined.contains(it) }
            matchesCache && !containsForbidden
        }
    }

    private fun findNodeByExactOrContains(
        root: AccessibilityNodeInfo,
        allowedTerms: List<String>,
        forbiddenTerms: List<String>,
        preferButton: Boolean = false
    ): AccessibilityNodeInfo? {
        val nodes = mutableListOf<AccessibilityNodeInfo>()
        collectNodes(root, nodes)

        val matches = nodes.filter { node ->
            val text = "${node.text ?: ""} ${node.contentDescription ?: ""}".trim().lowercase()
            if (text.isBlank()) return@filter false
            if (forbiddenTerms.any { text.contains(it) }) return@filter false
            allowedTerms.any { term -> text == term || text.startsWith("$term ") }
        }

        if (preferButton) {
            matches.firstOrNull {
                it.className?.toString()?.contains("Button", ignoreCase = true) == true
            }?.let { return it }
        }
        return matches.firstOrNull()
    }

    private fun collectNodes(node: AccessibilityNodeInfo, out: MutableList<AccessibilityNodeInfo>) {
        out.add(node)
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { collectNodes(it, out) }
        }
    }

    private fun isEffectivelyEnabled(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = node
        var depth = 0
        while (current != null && depth < 4) {
            if (!current.isEnabled) return false
            if (current.isClickable) return current.isEnabled
            current = current.parent
            depth++
        }
        return node.isEnabled
    }

    private fun clickNodeOrParent(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = node
        var depth = 0
        while (current != null && depth < 5) {
            if (current.isClickable && current.isEnabled) {
                return current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            current = current.parent
            depth++
        }
        return false
    }

    private fun scrollFirstScrollableContainer(root: AccessibilityNodeInfo) {
        val nodes = mutableListOf<AccessibilityNodeInfo>()
        collectNodes(root, nodes)
        nodes.firstOrNull { it.isScrollable }?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
    }

    private fun finishAndReturnToHelper(task: CacheAutomationTask, outcome: CacheClearOutcome) {
        completeTask(task, outcome)
        // Return cleanly to our Helper UI
        runCatching {
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(intent)
        }
    }

    private fun completeTask(task: CacheAutomationTask, outcome: CacheClearOutcome) {
        AccessibilityBridge.activeTask.compareAndSet(task, null)
        task.deferred.complete(outcome)
    }

    companion object {
        private val FORBIDDEN_DATA_TERMS = listOf(
            "daten löschen",
            "clear data",
            "speicher löschen",
            "clear storage",
            "deinstallieren",
            "uninstall"
        )
    }
}
