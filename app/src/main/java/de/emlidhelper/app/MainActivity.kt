package de.emlidhelper.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import de.emlidhelper.app.data.CheckSeverity
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
            EmlidHelperTheme {
                EmlidHelperRoot()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmlidHelperRoot(viewModel: HelperViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
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

    // Auto-refresh live BT/Wi-Fi/Samsung status whenever user returns from Settings
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
                    Text(
                        text = when (selectedTab) {
                            0 -> "Emlid Connect Helper"
                            1 -> "Samsung S24 Check"
                            else -> "Automation & Log"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                    label = { Text("Dashboard") }
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
                    label = { Text("Samsung") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.AutoFixHigh, contentDescription = null) },
                    label = { Text("Automation") }
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
                    onRefresh = viewModel::refreshAll,
                    onStartReset = viewModel::startFullReset,
                    onCancelReset = viewModel::cancelReset,
                    onConfirmManualStep = viewModel::confirmManualResetStep,
                    onDismissResetResult = viewModel::dismissResetResult,
                    onUpdateResetConfig = viewModel::updateResetConfig,
                    onQuickResetBluetooth = viewModel::quickResetBluetoothOnly,
                    onOpenBluetoothSettings = {
                        viewModel.handleSettingAction(de.emlidhelper.app.data.SettingActionType.OPEN_BLUETOOTH_SETTINGS)
                    },
                    onOpenWifiSettings = {
                        viewModel.handleSettingAction(de.emlidhelper.app.data.SettingActionType.OPEN_WIFI_SETTINGS)
                    },
                    onOpenVpnSettings = {
                        viewModel.handleSettingAction(de.emlidhelper.app.data.SettingActionType.OPEN_VPN_SETTINGS)
                    },
                    onOpenAirplaneSettings = viewModel::openAirplaneModeSettings,
                    onLaunchEmlidFlow = viewModel::launchEmlidFlow,
                    onRequestPermissions = requestRuntimePermissions,
                    onGoToAutomationTab = { selectedTab = 2 },
                    onGoToSamsungTab = { selectedTab = 1 }
                )

                1 -> SamsungSettingsScreen(
                    state = state,
                    onRefresh = viewModel::refreshAll,
                    onAction = viewModel::handleSettingAction
                )

                else -> AutomationScreen(
                    state = state,
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
