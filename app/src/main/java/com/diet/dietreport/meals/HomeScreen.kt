package com.diet.dietreport.meals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.diet.dietreport.AppError
import com.diet.dietreport.data.db.ReminderSlot
import com.diet.dietreport.data.db.SlotStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToLogMeal: (slotId: Long) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .semantics { testTag = "home_screen" },
    ) {
        // Error banner
        uiState.error?.let { error ->
            val msg = (error as? AppError.SchedulerError)?.message ?: error.toString()
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier
                            .weight(1f)
                            .semantics { testTag = "scheduler_error" },
                    )
                    TextButton(onClick = viewModel::clearError) { Text("Dismiss") }
                }
            }
        }

        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Today's Meals", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "${uiState.slots.count { it.status == SlotStatus.SUCCESS }} of ${uiState.slots.size} logged",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Next meal banner
        val now = uiState.nowMs
        val nextPending = uiState.slots.firstOrNull { it.status == SlotStatus.PENDING }
        if (nextPending != null) {
            val isInWindow = now >= nextPending.scheduledAt - HomeViewModel.LOG_WINDOW_BEFORE_MS &&
                now <= nextPending.scheduledAt + HomeViewModel.LOG_WINDOW_AFTER_MS
            val (bannerText, isUrgent) = if (isInWindow) {
                "Log your meal now!" to true
            } else {
                "Next meal at ${timeFormat.format(Date(nextPending.scheduledAt))}" to false
            }
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isUrgent)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.secondaryContainer,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .semantics { testTag = "next_meal_banner" },
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = if (isUrgent)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = bannerText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isUrgent) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isUrgent)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.semantics { testTag = "next_meal_banner_text" },
                    )
                }
            }
        }

        if (uiState.slots.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .semantics { testTag = "empty_state" },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outlineVariant,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "No meals scheduled for today",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(uiState.slots, key = { it.id }) { slot ->
                    SlotCard(slot = slot, now = now, onNavigateToLogMeal = onNavigateToLogMeal)
                }
            }
        }
    }
}

@Composable
private fun SlotCard(slot: ReminderSlot, now: Long, onNavigateToLogMeal: (Long) -> Unit) {
    val isPending = slot.status == SlotStatus.PENDING
    val isInWindow = now >= slot.scheduledAt - HomeViewModel.LOG_WINDOW_BEFORE_MS &&
        now <= slot.scheduledAt + HomeViewModel.LOG_WINDOW_AFTER_MS
    val isClickable = isPending && isInWindow

    Card(
        onClick = { if (isClickable) onNavigateToLogMeal(slot.id) },
        enabled = isClickable,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { testTag = "slot_row_${slot.id}" },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (slot.status) {
                SlotStatus.SUCCESS -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                SlotStatus.FAILED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val icon = when (slot.status) {
                SlotStatus.SUCCESS -> Icons.Default.CheckCircle
                SlotStatus.FAILED -> Icons.Default.Close
                else -> Icons.Outlined.Schedule
            }
            val iconTint = when (slot.status) {
                SlotStatus.SUCCESS -> MaterialTheme.colorScheme.primary
                SlotStatus.FAILED -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(28.dp),
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = timeFormat.format(Date(slot.scheduledAt)),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = when {
                        slot.status == SlotStatus.SUCCESS -> "Logged"
                        slot.status == SlotStatus.FAILED -> "Missed"
                        isInWindow -> "Tap to log meal"
                        else -> "Opens at ${timeFormat.format(Date(slot.scheduledAt - HomeViewModel.LOG_WINDOW_BEFORE_MS))}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            StatusChip(slot = slot)
        }
    }
}

@Composable
private fun StatusChip(slot: ReminderSlot) {
    val label = when (slot.status) {
        SlotStatus.SUCCESS -> "Success"
        SlotStatus.FAILED -> "Failed"
        else -> "Pending"
    }
    SuggestionChip(
        onClick = {},
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.semantics { testTag = "slot_chip_${slot.id}" },
            )
        },
        enabled = false,
        shape = RoundedCornerShape(20.dp),
    )
}
