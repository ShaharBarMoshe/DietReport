package com.diet.dietreport.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            delay(3_000)
            viewModel.clearSaveSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .semantics { testTag = "settings_screen" }
            .verticalScroll(rememberScrollState()),
    ) {
        // Header
        Text(
            "Settings",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
        )

        // Schedule section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Schedule", style = MaterialTheme.typography.titleMedium)

                OutlinedTextField(
                    value = uiState.wakeTimeText,
                    onValueChange = viewModel::onWakeTimeTextChange,
                    label = { Text("Wake time (HH:MM)") },
                    isError = uiState.wakeTimeError != null,
                    supportingText = uiState.wakeTimeError?.let { error -> { Text(error) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { testTag = "wake_time_field" },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )

                OutlinedTextField(
                    value = uiState.bedTimeText,
                    onValueChange = viewModel::onBedTimeTextChange,
                    label = { Text("Bedtime (HH:MM)") },
                    isError = uiState.bedTimeError != null,
                    supportingText = uiState.bedTimeError?.let { error -> { Text(error) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { testTag = "bed_time_field" },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )

                OutlinedTextField(
                    value = uiState.delayText,
                    onValueChange = viewModel::onDelayTextChange,
                    label = { Text("First-meal delay (0-180 min)") },
                    isError = uiState.delayError != null,
                    supportingText = uiState.delayError?.let { error -> { Text(error) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { testTag = "delay_field" },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
            }
        }

        // Notifications section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Notification Sound", style = MaterialTheme.typography.titleMedium)
                SettingsValidator.ALLOWED_RINGTONES.forEach { ringtone ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        RadioButton(
                            selected = uiState.ringtone == ringtone,
                            onClick = { viewModel.onRingtoneChange(ringtone) },
                            modifier = Modifier.semantics {
                                testTag = "ringtone_radio_$ringtone"
                            },
                        )
                        Text(
                            text = ringtone.replaceFirstChar { it.titlecase() },
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }
            }
        }

        // Monitoring section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text("Activity monitor", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Track daily app usage",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = uiState.activityMonitorEnabled,
                    onCheckedChange = viewModel::onActivityMonitorToggle,
                    modifier = Modifier.semantics { contentDescription = "Activity monitor toggle" },
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = viewModel::save,
            enabled = !uiState.isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(50.dp)
                .semantics { testTag = "save_button" },
            shape = MaterialTheme.shapes.medium,
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Text("Save", modifier = Modifier.padding(start = 8.dp))
        }

        AnimatedVisibility(
            visible = uiState.saveSuccess,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text(
                    "Settings saved",
                    modifier = Modifier
                        .padding(16.dp)
                        .semantics { testTag = "save_success" },
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        uiState.error?.let { error ->
            val msg = when (error) {
                is com.diet.dietreport.AppError.DatabaseError -> error.message
                else -> error.toString()
            }
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text(
                    msg,
                    modifier = Modifier
                        .padding(16.dp)
                        .semantics { testTag = "settings_error" },
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
