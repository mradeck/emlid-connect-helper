package de.emlidhelper.app.reset

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import de.emlidhelper.app.accessibility.AccessibilityBridge
import de.emlidhelper.app.accessibility.CacheClearOutcome
import de.emlidhelper.app.data.ResetConfig
import de.emlidhelper.app.data.ResetPhase
import de.emlidhelper.app.data.ResetProgress
import de.emlidhelper.app.data.ShizukuState
import de.emlidhelper.app.monitor.BluetoothMonitor
import de.emlidhelper.app.monitor.SamsungSettingsMonitor
import de.emlidhelper.app.shizuku.ShizukuController
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeoutOrNull

class ResetOrchestrator(
    private val context: Context,
    private val shizuku: ShizukuController,
    private val bluetoothMonitor: BluetoothMonitor,
    private val settingsMonitor: SamsungSettingsMonitor,
    private val onRefreshStatus: suspend () -> Unit,
    private val onLog: (String, Boolean) -> Unit
) {
    private val _progress = MutableStateFlow(ResetProgress())
    val progress: StateFlow<ResetProgress> = _progress.asStateFlow()

    @Volatile
    private var manualContinueSignal: CompletableDeferred<Unit>? = null

    fun confirmManualStep() {
        manualContinueSignal?.complete(Unit)
    }

    fun cancelReset() {
        AccessibilityBridge.disarm()
        manualContinueSignal?.cancel()
        manualContinueSignal = null
        _progress.value = ResetProgress(phase = ResetPhase.IDLE)
        onLog("Reset vom Benutzer abgebrochen", false)
    }

    fun dismissCompletedBanner() {
        if (!_progress.value.isRunning) {
            _progress.value = ResetProgress(phase = ResetPhase.IDLE)
        }
    }

    suspend fun runFullReset(config: ResetConfig) {
        val shizukuReady = shizuku.state.value == ShizukuState.READY
        val accessibilityReady = AccessibilityBridge.isEnabledInSettings(context)
        val emlidPkg = settingsMonitor.findEmlidPackageName()
        val totalSteps = listOf(
            config.forceStopEmlid && emlidPkg != null,
            config.clearCache && emlidPkg != null,
            config.toggleAirplaneMode,
            true, // verify BT clear
            config.launchEmlidAfter && emlidPkg != null
        ).count { it }.coerceAtLeast(1)

        var step = 0
        onLog(
            "Starte Emlid-Reset (Shizuku=${if (shizukuReady) "AN" else "AUS"}, " +
                "Bedienungshilfe=${if (accessibilityReady) "AN" else "AUS"}, " +
                "Wartezeit=${config.airplaneWaitSeconds}s)",
            false
        )

        try {
            // ── STEP 1: Force Stop Emlid Flow ──
            var alreadyForceStopped = false
            if (config.forceStopEmlid && emlidPkg != null) {
                step++
                if (shizukuReady) {
                    _progress.value = ResetProgress(
                        phase = ResetPhase.FORCE_STOPPING_EMLID,
                        currentStep = step,
                        totalSteps = totalSteps,
                        title = "Emlid Flow wird zwangsbeendet…",
                        detail = "Beende hängende Hintergrund-Sitzung per Shizuku."
                    )
                    val res = shizuku.forceStopPackage(emlidPkg)
                    if (res.isSuccess) {
                        alreadyForceStopped = true
                        onLog("Emlid Flow erfolgreich gestoppt", false)
                    } else {
                        onLog("Force-Stop via Shizuku fehlgeschlagen: ${res.output}", true)
                    }
                    delay(450)
                }
            }

            // ── STEP 2: Clear ONLY Emlid Flow Cache (and Force Stop via Accessibility if needed) ──
            val needManualOrAutoForceStop = config.forceStopEmlid && !alreadyForceStopped && emlidPkg != null
            if ((config.clearCache || needManualOrAutoForceStop) && emlidPkg != null) {
                if (config.clearCache) step++
                if (accessibilityReady) {
                    _progress.value = ResetProgress(
                        phase = ResetPhase.CLEARING_CACHE_AUTO,
                        currentStep = step.coerceAtLeast(1),
                        totalSteps = totalSteps,
                        title = "Cache von Emlid Flow wird geleert…",
                        detail = "Bedienungshilfe tippt automatisch auf „Speicher“ → „Cache leeren“ (niemals Daten!)."
                    )
                    val task = AccessibilityBridge.armTask(
                        clearCache = config.clearCache,
                        forceStop = needManualOrAutoForceStop
                    )
                    settingsMonitor.openEmlidAppInfo(emlidPkg)
                    val outcome = withTimeoutOrNull(18_000L) { task.deferred.await() }
                        ?: CacheClearOutcome.TIMED_OUT
                    AccessibilityBridge.disarm()

                    when (outcome) {
                        CacheClearOutcome.CLEARED -> onLog("Emlid Flow Cache erfolgreich geleert", false)
                        CacheClearOutcome.ALREADY_EMPTY -> onLog("Emlid Flow Cache war bereits 0 B", false)
                        CacheClearOutcome.TIMED_OUT -> onLog("Auto-Cache Zeitüberschreitung – fahre fort", true)
                        else -> onLog("Auto-Cache beendet ($outcome)", false)
                    }
                    delay(500)
                } else {
                    // Graceful Fallback (Variante A): Open Emlid Flow App Info & wait for user
                    _progress.value = ResetProgress(
                        phase = ResetPhase.WAITING_MANUAL_CACHE,
                        currentStep = step.coerceAtLeast(1),
                        totalSteps = totalSteps,
                        title = "Emlid Flow Cache manuell leeren",
                        detail = "Bedienungshilfe ist nicht aktiv. Tippe in der App-Info auf „Stopp erzwingen“ + „Speicher → Cache leeren“ und kehre dann zurück.",
                        isGuidedFallback = true
                    )
                    settingsMonitor.openEmlidAppInfo(emlidPkg)
                    awaitManualConfirmation()
                }
            }

            // ── STEP 3: Airplane Mode Reset (Auto via Shizuku OR Live Guided Watchdog) ──
            if (config.toggleAirplaneMode) {
                step++
                val waitSec = config.airplaneWaitSeconds
                if (shizukuReady) {
                    _progress.value = ResetProgress(
                        phase = ResetPhase.ENABLING_AIRPLANE_MODE,
                        currentStep = step,
                        totalSteps = totalSteps,
                        title = "Flugmodus wird eingeschaltet…",
                        detail = "Bluetooth- und WLAN-Sockets werden sauber zurückgesetzt."
                    )
                    val onRes = shizuku.setAirplaneMode(true)
                    if (!onRes.isSuccess) {
                        onLog("Shizuku Flugmodus EIN fehlgeschlagen: ${onRes.output}", true)
                        runGuidedAirplaneMode(step, totalSteps, waitSec)
                    } else {
                        delay(800)
                        runCountdown(
                            step = step,
                            totalSteps = totalSteps,
                            seconds = waitSec,
                            guided = false,
                            detail = "Funkmodule ruhen – hängende BLE-Sessions werden verworfen."
                        )

                        _progress.value = ResetProgress(
                            phase = ResetPhase.DISABLING_AIRPLANE_MODE,
                            currentStep = step,
                            totalSteps = totalSteps,
                            title = "Flugmodus wird ausgeschaltet…",
                            detail = "Bluetooth und WLAN werden wieder aktiviert."
                        )
                        shizuku.setAirplaneMode(false)
                        delay(1500)

                        // Ensure BT & Wi-Fi come back up cleanly
                        if (!bluetoothMonitor.inspect(shizuku).isEnabled) {
                            shizuku.setBluetoothEnabled(true)
                        }
                        waitForRadiosRecovery(step, totalSteps)
                    }
                } else {
                    runGuidedAirplaneMode(step, totalSteps, waitSec)
                }
            }

            // ── STEP 4: Verify Bluetooth is clear ──
            step++
            _progress.value = ResetProgress(
                phase = ResetPhase.VERIFYING_BLUETOOTH_CLEAR,
                currentStep = step.coerceAtMost(totalSteps),
                totalSteps = totalSteps,
                title = "Prüfe Bluetooth- & WLAN-Status…",
                detail = "Kontrolliere, ob alle alten Verbindungen getrennt sind."
            )
            onRefreshStatus()
            val btStatus = bluetoothMonitor.inspect(shizuku)
            val btSummary = if (btStatus.connectedDevices.isEmpty()) {
                "Alle Bluetooth-Verbindungen getrennt – Kanal ist frei!"
            } else {
                "Noch verbunden: ${btStatus.connectedDevices.joinToString { it.name }}"
            }
            onLog("Prüfung nach Reset: $btSummary", btStatus.connectedDevices.isNotEmpty())
            delay(600)

            // ── STEP 5: Launch Emlid Flow ──
            if (config.launchEmlidAfter && emlidPkg != null) {
                step++
                _progress.value = ResetProgress(
                    phase = ResetPhase.LAUNCHING_EMLID,
                    currentStep = totalSteps,
                    totalSteps = totalSteps,
                    title = "Starte Emlid Flow…",
                    detail = btSummary
                )
                delay(500)
                settingsMonitor.launchEmlidFlow()
                onLog("Emlid Flow gestartet", false)
            }

            vibrateSuccess()
            _progress.value = ResetProgress(
                phase = ResetPhase.COMPLETED,
                currentStep = totalSteps,
                totalSteps = totalSteps,
                title = "Reset abgeschlossen!",
                detail = btSummary
            )
            onRefreshStatus()
        } catch (e: kotlinx.coroutines.CancellationException) {
            AccessibilityBridge.disarm()
            _progress.value = ResetProgress(phase = ResetPhase.IDLE)
            throw e
        } catch (e: Exception) {
            AccessibilityBridge.disarm()
            onLog("Reset-Fehler: ${e.message}", true)
            _progress.value = ResetProgress(
                phase = ResetPhase.FAILED,
                title = "Reset unterbrochen",
                detail = e.message ?: "Unbekannter Fehler",
                errorMessage = e.message
            )
        }
    }

    private suspend fun runGuidedAirplaneMode(step: Int, totalSteps: Int, waitSec: Int) {
        if (!bluetoothMonitor.isAirplaneModeOn()) {
            _progress.value = ResetProgress(
                phase = ResetPhase.WAITING_MANUAL_AIRPLANE_ON,
                currentStep = step,
                totalSteps = totalSteps,
                title = "Bitte Flugmodus EINschalten",
                detail = "Ziehe die Samsung-Schnelleinstellungen herunter und aktiviere den Flugmodus. Der Timer startet danach automatisch.",
                isGuidedFallback = true
            )
            while (!bluetoothMonitor.isAirplaneModeOn()) {
                delay(350)
            }
            onLog("Flugmodus EIN erkannt – starte $waitSec-Sekunden-Timer", false)
        }

        runCountdown(
            step = step,
            totalSteps = totalSteps,
            seconds = waitSec,
            guided = true,
            detail = "Flugmodus ist AN. Warte bis der Countdown abläuft…"
        )
        vibratePrompt()

        _progress.value = ResetProgress(
            phase = ResetPhase.WAITING_MANUAL_AIRPLANE_OFF,
            currentStep = step,
            totalSteps = totalSteps,
            title = "Jetzt Flugmodus wieder AUSschalten!",
            detail = "Wartezeit abgelaufen. Schalte den Flugmodus aus – die App erkennt es automatisch.",
            isGuidedFallback = true
        )
        while (bluetoothMonitor.isAirplaneModeOn()) {
            delay(350)
        }
        onLog("Flugmodus AUS erkannt – warte auf Funkmodule", false)
        waitForRadiosRecovery(step, totalSteps)
    }

    private suspend fun runCountdown(
        step: Int,
        totalSteps: Int,
        seconds: Int,
        guided: Boolean,
        detail: String
    ) {
        for (remaining in seconds downTo 1) {
            _progress.value = ResetProgress(
                phase = ResetPhase.AIRPLANE_COUNTDOWN,
                currentStep = step,
                totalSteps = totalSteps,
                title = "Flugmodus aktiv (${remaining}s)",
                detail = detail,
                countdownRemainingSeconds = remaining,
                countdownTotalSeconds = seconds,
                isGuidedFallback = guided
            )
            delay(1000)
        }
    }

    private suspend fun waitForRadiosRecovery(step: Int, totalSteps: Int) {
        _progress.value = ResetProgress(
            phase = ResetPhase.WAITING_RADIOS_RECOVERY,
            currentStep = step,
            totalSteps = totalSteps,
            title = "Warte auf Bluetooth & WLAN…",
            detail = "Funkmodule werden nach dem Flugmodus wieder hochgefahren."
        )
        repeat(14) {
            val bt = bluetoothMonitor.inspect(shizuku)
            if (bt.isEnabled && !bt.isAirplaneModeOn) {
                delay(900)
                return
            }
            delay(500)
        }
    }

    private suspend fun awaitManualConfirmation() {
        val deferred = CompletableDeferred<Unit>()
        manualContinueSignal = deferred
        try {
            deferred.await()
        } finally {
            manualContinueSignal = null
        }
    }

    private fun vibratePrompt() {
        vibratePattern(longArrayOf(0, 180, 120, 180))
    }

    private fun vibrateSuccess() {
        vibratePattern(longArrayOf(0, 100, 80, 160))
    }

    private fun vibratePattern(timings: LongArray) {
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            vibrator?.vibrate(VibrationEffect.createWaveform(timings, -1))
        }
    }
}
