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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assistant
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberGold
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonCrimson
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianCardBorder
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.viewmodel.AssistantViewModel
import com.example.voice.AssistantLanguage

@Composable
fun SettingsScreen(
    viewModel: AssistantViewModel,
    modifier: Modifier = Modifier
) {
    val aiName by viewModel.aiName.collectAsStateWithLifecycle()
    val currentLanguage by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val femaleVoiceEnabled by viewModel.femaleVoiceEnabled.collectAsStateWithLifecycle()
    val autoSpeakEnabled by viewModel.autoSpeakEnabled.collectAsStateWithLifecycle()
    val isRootAvailable by viewModel.isRootAvailable.collectAsStateWithLifecycle()
    val isRootAuthorized by viewModel.isRootAuthorized.collectAsStateWithLifecycle()

    var editingName by remember { mutableStateOf(false) }
    var nameField by remember(aiName) { mutableStateOf(aiName) }

    var pitchSlider by remember { mutableFloatStateOf(viewModel.voiceManager.pitch) }
    var rateSlider by remember { mutableFloatStateOf(viewModel.voiceManager.speechRate) }

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
        Column {
            Text(
                text = "Assistant Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )
            Text(
                text = "Customize persona, voice speech, and security",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondaryDark
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // AI Persona & Name Card
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
                        imageVector = Icons.Default.Assistant,
                        contentDescription = null,
                        tint = NeonCrimson,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Assistant Identity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimaryDark
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (editingName) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = nameField,
                            onValueChange = { nameField = it },
                            label = { Text("AI Name Placeholder") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ai_name_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCrimson,
                                unfocusedBorderColor = ObsidianCardBorder,
                                focusedTextColor = TextPrimaryDark,
                                unfocusedTextColor = TextPrimaryDark
                            ),
                            singleLine = true
                        )
                        IconButton(
                            onClick = {
                                viewModel.setAiName(nameField)
                                editingName = false
                            },
                            modifier = Modifier.testTag("save_ai_name_button")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Save", tint = CyberCyan)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Current Name", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                            Text(text = aiName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = NeonCrimson)
                        }
                        IconButton(
                            onClick = { editingName = true },
                            modifier = Modifier.testTag("edit_ai_name_button")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Name", tint = TextSecondaryDark)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Language & Voice Settings
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
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = ElectricViolet,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Language & Speech",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimaryDark
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Language Selector
                Text(text = "Voice & Assistant Language", style = MaterialTheme.typography.bodyMedium, color = TextSecondaryDark)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistantLanguage.values().forEach { lang ->
                        val isSelected = currentLanguage == lang
                        Button(
                            onClick = { viewModel.setLanguage(lang) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) ElectricViolet else ObsidianBg
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(
                                    1.dp,
                                    if (isSelected) ElectricViolet else ObsidianCardBorder,
                                    RoundedCornerShape(12.dp)
                                )
                                .testTag("lang_btn_${lang.code}")
                        ) {
                            Text(
                                text = lang.displayName,
                                color = if (isSelected) Color.White else TextSecondaryDark,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Female Voice Option Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Female Voice Profile",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "Uses female-leaning pitch and preferred female TTS voice engine",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryDark
                        )
                    }
                    Switch(
                        checked = femaleVoiceEnabled,
                        onCheckedChange = { viewModel.setFemaleVoice(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = NeonCrimson
                        ),
                        modifier = Modifier.testTag("female_voice_switch")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Auto Speak Response
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Read Aloud Responses",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "Automatically speak responses after commands",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryDark
                        )
                    }
                    Switch(
                        checked = autoSpeakEnabled,
                        onCheckedChange = { viewModel.setAutoSpeak(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = ElectricViolet
                        ),
                        modifier = Modifier.testTag("auto_speak_switch")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Voice Pitch Slider
                Text(
                    text = "Voice Pitch (${String.format("%.2f", pitchSlider)}x)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondaryDark
                )
                Slider(
                    value = pitchSlider,
                    onValueChange = {
                        pitchSlider = it
                        viewModel.voiceManager.pitch = it
                    },
                    valueRange = 0.8f..1.6f,
                    colors = SliderDefaults.colors(
                        thumbColor = CyberCyan,
                        activeTrackColor = CyberCyan,
                        inactiveTrackColor = ObsidianCardBorder
                    ),
                    modifier = Modifier.testTag("pitch_slider")
                )

                // Speech Rate Slider
                Text(
                    text = "Speech Rate (${String.format("%.2f", rateSlider)}x)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondaryDark
                )
                Slider(
                    value = rateSlider,
                    onValueChange = {
                        rateSlider = it
                        viewModel.voiceManager.speechRate = it
                    },
                    valueRange = 0.7f..1.5f,
                    colors = SliderDefaults.colors(
                        thumbColor = ElectricViolet,
                        activeTrackColor = ElectricViolet,
                        inactiveTrackColor = ObsidianCardBorder
                    ),
                    modifier = Modifier.testTag("rate_slider")
                )

                // Voice Test Button
                Button(
                    onClick = {
                        val testPhrase = if (currentLanguage == AssistantLanguage.BENGALI) {
                            "নমস্কার! আমি $aiName, আপনার এআই ভয়েস সহকারী।"
                        } else {
                            "Hello! I am $aiName, your personal AI voice assistant."
                        }
                        viewModel.speakMessage(testPhrase)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ObsidianBg),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ObsidianCardBorder, RoundedCornerShape(12.dp))
                        .testTag("test_voice_button")
                ) {
                    Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Test Voice Audio", color = TextPrimaryDark)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Security & Root Privileges
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
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = CyberGold,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Root & System Privileges",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimaryDark
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Root binary detection: ${if (isRootAvailable) "Detected on device" else "Not detected"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondaryDark
                )
                Text(
                    text = "Authorization status: ${if (isRootAuthorized) "Authorized (uid=0)" else "Not authorized"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isRootAuthorized) CyberCyan else TextSecondaryDark
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { viewModel.requestRoot() },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isRootAuthorized) ObsidianBg else NeonCrimson),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("request_root_button")
                ) {
                    Text(
                        text = if (isRootAuthorized) "Re-verify SU Authorization" else "Request Superuser Privileges",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
