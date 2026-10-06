package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Assistant
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Assistant
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.ActionConfirmationDialog
import com.example.ui.screens.ActionLogsScreen
import com.example.ui.screens.AssistantScreen
import com.example.ui.screens.DeviceStatusScreen
import com.example.ui.screens.PermissionsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DevilAiTheme
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonCrimson
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianCardBorder
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.viewmodel.AssistantViewModel

enum class AppNavDestination(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    ASSISTANT("assistant", "Assistant", Icons.Filled.Assistant, Icons.Outlined.Assistant),
    DEVICE("device", "Device", Icons.Filled.PhoneAndroid, Icons.Outlined.PhoneAndroid),
    PERMISSIONS("permissions", "Permissions", Icons.Filled.Security, Icons.Outlined.Security),
    LOGS("logs", "Logs", Icons.Filled.Assessment, Icons.Outlined.Assessment),
    SETTINGS("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DevilAiTheme {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp(viewModel: AssistantViewModel = viewModel()) {
    val context = LocalContext.current
    var currentDestination by remember { mutableStateOf(AppNavDestination.ASSISTANT) }
    val pendingConfirmation by viewModel.pendingConfirmation.collectAsStateWithLifecycle()

    // Runtime Permission Launcher for Microphone & Notifications
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshPermissions()
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val ungranted = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (ungranted.isNotEmpty()) {
            permissionLauncher.launch(ungranted.toTypedArray())
        }
    }

    // BackHandler to return to Assistant screen
    if (currentDestination != AppNavDestination.ASSISTANT) {
        BackHandler {
            currentDestination = AppNavDestination.ASSISTANT
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = ObsidianBg,
        bottomBar = {
            NavigationBar(
                containerColor = ObsidianCard,
                contentColor = TextPrimaryDark,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("main_navigation_bar")
            ) {
                AppNavDestination.values().forEach { destination ->
                    val selected = currentDestination == destination
                    NavigationBarItem(
                        selected = selected,
                        onClick = { currentDestination = destination },
                        icon = {
                            Icon(
                                imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                                contentDescription = destination.title,
                                tint = if (selected) NeonCrimson else TextSecondaryDark
                            )
                        },
                        label = {
                            Text(
                                text = destination.title,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) NeonCrimson else TextSecondaryDark
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = NeonCrimson.copy(alpha = 0.15f),
                            selectedIconColor = NeonCrimson,
                            unselectedIconColor = TextSecondaryDark,
                            selectedTextColor = NeonCrimson,
                            unselectedTextColor = TextSecondaryDark
                        ),
                        modifier = Modifier.testTag("nav_tab_${destination.route}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentDestination) {
                AppNavDestination.ASSISTANT -> AssistantScreen(viewModel = viewModel)
                AppNavDestination.DEVICE -> DeviceStatusScreen(viewModel = viewModel)
                AppNavDestination.PERMISSIONS -> PermissionsScreen(viewModel = viewModel)
                AppNavDestination.LOGS -> ActionLogsScreen(viewModel = viewModel)
                AppNavDestination.SETTINGS -> SettingsScreen(viewModel = viewModel)
            }

            // Security Confirmation Dialog
            pendingConfirmation?.let { conf ->
                ActionConfirmationDialog(
                    confirmation = conf,
                    onConfirm = { viewModel.confirmPendingAction() },
                    onCancel = { viewModel.cancelPendingAction() }
                )
            }
        }
    }
}
