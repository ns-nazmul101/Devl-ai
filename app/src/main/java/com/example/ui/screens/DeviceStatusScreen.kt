package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberGold
import com.example.ui.theme.CyberSuccess
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonCrimson
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianCardBorder
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.viewmodel.AssistantViewModel

@Composable
fun DeviceStatusScreen(
    viewModel: AssistantViewModel,
    modifier: Modifier = Modifier
) {
    val status by viewModel.deviceStatus.collectAsStateWithLifecycle()
    val isRootAuthorized by viewModel.isRootAuthorized.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.refreshDeviceStatus()
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Device Diagnostics",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
                Text(
                    text = "Hardware telemetry & system status",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondaryDark
                )
            }

            IconButton(
                onClick = { viewModel.refreshDeviceStatus() },
                modifier = Modifier.testTag("refresh_device_status_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh Status",
                    tint = CyberCyan
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (status == null) {
            Text("Loading diagnostics...", color = TextSecondaryDark)
            return
        }

        val s = status!!

        // Top Row: Battery & Root Status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Battery Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, ObsidianCardBorder, RoundedCornerShape(16.dp))
                    .testTag("battery_status_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = if (s.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                            contentDescription = null,
                            tint = if (s.batteryLevel > 20) CyberSuccess else CyberGold,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "${s.batteryLevel}%",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (s.isCharging) "Charging" else "On Battery",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondaryDark
                    )
                }
            }

            // Root Status Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, ObsidianCardBorder, RoundedCornerShape(16.dp))
                    .testTag("root_status_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (isRootAuthorized) NeonCrimson else ElectricViolet),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "#",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        }
                        Text(
                            text = if (isRootAuthorized) "Rooted" else "Standard",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isRootAuthorized) NeonCrimson else CyberCyan
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isRootAuthorized) "SU Privileges Active" else "Sandbox Mode",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondaryDark
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // RAM Usage Card
        StatusProgressCard(
            title = "RAM Memory",
            icon = Icons.Default.Memory,
            subtitle = "${s.ramUsedMb} MB used / ${s.ramTotalMb} MB total",
            progress = if (s.ramTotalMb > 0) (s.ramUsedMb.toFloat() / s.ramTotalMb.toFloat()) else 0f,
            barColor = ElectricViolet
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Storage Usage Card
        val storagePct = if (s.storageTotalGb > 0) (s.storageUsedGb.toFloat() / s.storageTotalGb.toFloat()) else 0f
        StatusProgressCard(
            title = "Internal Storage",
            icon = Icons.Default.Storage,
            subtitle = "${s.storageUsedGb} GB used / ${s.storageTotalGb} GB total",
            progress = storagePct,
            barColor = CyberCyan
        )

        Spacer(modifier = Modifier.height(12.dp))

        // System Specifications
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianCard),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ObsidianCardBorder, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        tint = NeonCrimson,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Device Specs",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimaryDark
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                SpecRow("Device Model", "${s.manufacturer} ${s.deviceModel}")
                SpecRow("Android OS", "${s.androidVersion} (API ${s.apiLevel})")
                SpecRow("Network Type", s.networkType)
                SpecRow("IP Address", s.ipAddress)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Controls
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimaryDark
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.sendTextCommand("Toggle flashlight") },
                colors = ButtonDefaults.buttonColors(containerColor = ObsidianCard),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, ObsidianCardBorder, RoundedCornerShape(12.dp))
            ) {
                Icon(Icons.Default.FlashlightOn, contentDescription = null, tint = CyberGold, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Flashlight", fontSize = 12.sp, color = TextPrimaryDark)
            }

            Button(
                onClick = { viewModel.sendTextCommand("Mute volume") },
                colors = ButtonDefaults.buttonColors(containerColor = ObsidianCard),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, ObsidianCardBorder, RoundedCornerShape(12.dp))
            ) {
                Icon(Icons.AutoMirrored.Filled.VolumeMute, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Mute", fontSize = 12.sp, color = TextPrimaryDark)
            }

            Button(
                onClick = { viewModel.sendTextCommand("Open Wi-Fi settings") },
                colors = ButtonDefaults.buttonColors(containerColor = ObsidianCard),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, ObsidianCardBorder, RoundedCornerShape(12.dp))
            ) {
                Icon(Icons.Default.Wifi, contentDescription = null, tint = ElectricViolet, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Wi-Fi", fontSize = 12.sp, color = TextPrimaryDark)
            }
        }
    }
}

@Composable
fun StatusProgressCard(
    title: String,
    icon: ImageVector,
    subtitle: String,
    progress: Float,
    barColor: Color
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianCard),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ObsidianCardBorder, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = icon, contentDescription = null, tint = barColor, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimaryDark
                    )
                }
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = barColor
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = barColor,
                trackColor = ObsidianCardBorder
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondaryDark
            )
        }
    }
}

@Composable
fun SpecRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextSecondaryDark)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = TextPrimaryDark)
    }
}
