package de.emlidhelper.app

import android.Manifest
import android.app.Activity
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import de.emlidhelper.app.config.AppMetadata
import de.emlidhelper.app.data.AppLanguage
import de.emlidhelper.app.data.AppThemeMode
import de.emlidhelper.app.data.CheckSeverity
import de.emlidhelper.app.data.SettingActionType
import de.emlidhelper.app.i18n.stringsFor
import de.emlidhelper.app.ui.HelperViewModel
import de.emlidhelper.app.ui.screens.AutomationScreen
import de.emlidhelper.app.ui.screens.DashboardScreen
import de.emlidhelper.app.ui.screens.SamsungSettingsScreen
import de.emlidhelper.app.ui.theme.EmlidHelperTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EmlidHelperAppRoot()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmlidHelperAppRoot(viewModel: HelperViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val systemDark = isSystemInDarkTheme()
    val isDark = when (state.themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> systemDark
    }
    val strings = stringsFor(state.language)

    // Keep status/nav bar icons legible when switching Light <-> Dark
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !isDark
                isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    EmlidHelperTheme(darkTheme = isDark) {
        var selectedTab by rememberSaveable { mutableIntStateOf(0) }

        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) {
            viewModel.refreshAll()
        }

        val requestRuntimePermissions = {
            val perms = buildList {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    add(Manifest.permission.BLUETOOTH_CONNECT)
                    add(Manifest.permission.BLUETOOTH_SCAN)
                }
                add(Manifest.permission.ACCESS_FINE_LOCATION)
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    add(Manifest.permission.NEARBY_WIFI_DEVICES)
                }
            }.toTypedArray()
            permissionLauncher.launch(perms)
        }

        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    viewModel.refreshAll()
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        val samsungIssues = state.samsungChecks.count { it.severity == CheckSeverity.ERROR }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = when (selectedTab) {
                                    0 -> AppMetadata.APP_NAME
                                    1 -> strings.titleDeviceCheck
                                    else -> strings.titleAutomation
                                },
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = AppMetadata.APP_VERSION_CODE,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    actions = {
                        // Crisp SVG Flag EN/DE toggle chip
                        val isDe = state.language == AppLanguage.DE
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .clickable { viewModel.toggleLanguage() }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Image(
                                    painter = painterResource(
                                        id = if (isDe) R.drawable.ic_flag_de else R.drawable.ic_flag_gb
                                    ),
                                    contentDescription = if (isDe) strings.switchToEnglish else strings.switchToGerman,
                                    modifier = Modifier.size(width = 20.dp, height = 15.dp)
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = if (isDe) "DE" else "EN",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Light / Dark theme toggle
                        IconButton(onClick = { viewModel.toggleTheme(isDark) }) {
                            Icon(
                                imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = if (isDark) strings.switchToLightMode else strings.switchToDarkMode
                            )
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                        label = { Text(strings.tabDashboard) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (samsungIssues > 0) {
                                        Badge { Text("$samsungIssues") }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Checklist, contentDescription = null)
                            }
                        },
                        label = { Text(strings.tabDeviceCheck) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.AutoFixHigh, contentDescription = null) },
                        label = { Text(strings.tabAutomation) }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedTab) {
                    0 -> DashboardScreen(
                        state = state,
                        strings = strings,
                        isDark = isDark,
                        onRefresh = viewModel::refreshAll,
                        onStartReset = viewModel::startFullReset,
                        onCancelReset = viewModel::cancelReset,
                        onConfirmManualStep = viewModel::confirmManualResetStep,
                        onDismissResetResult = viewModel::dismissResetResult,
                        onUpdateResetConfig = viewModel::updateResetConfig,
                        onQuickResetBluetooth = viewModel::quickResetBluetoothOnly,
                        onBrowseReceiverIp = viewModel::browseReceiverIp,
                        onForgetReachWifi = viewModel::forgetSavedReachWifiNetworks,
                        onDismissWifiForgetFeedback = viewModel::dismissWifiForgetFeedback,
                        onOpenBluetoothSettings = {
                            viewModel.handleSettingAction(SettingActionType.OPEN_BLUETOOTH_SETTINGS)
                        },
                        onOpenWifiSettings = {
                            viewModel.handleSettingAction(SettingActionType.OPEN_WIFI_SETTINGS)
                        },
                        onOpenVpnSettings = {
                            viewModel.handleSettingAction(SettingActionType.OPEN_VPN_SETTINGS)
                        },
                        onOpenAirplaneSettings = viewModel::openAirplaneModeSettings,
                        onLaunchEmlidFlow = viewModel::launchEmlidFlow,
                        onRequestPermissions = requestRuntimePermissions,
                        onGoToAutomationTab = { selectedTab = 2 },
                        onGoToSamsungTab = { selectedTab = 1 }
                    )

                    1 -> SamsungSettingsScreen(
                        state = state,
                        strings = strings,
                        onRefresh = viewModel::refreshAll,
                        onAction = viewModel::handleSettingAction
                    )

                    else -> AutomationScreen(
                        state = state,
                        strings = strings,
                        onRequestShizukuPermission = viewModel::requestShizukuPermission,
                        onOpenShizukuApp = viewModel::openShizukuApp,
                        onOpenAccessibilitySettings = viewModel::openAccessibilitySettings,
                        onRequestPermissions = requestRuntimePermissions,
                        onRefresh = viewModel::refreshAll,
                        onClearLogs = viewModel::clearLogs
                    )
                }
            }
        }
    }
}
