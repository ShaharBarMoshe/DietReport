package com.diet.dietreport.reports

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ReportScreen(viewModel: ReportViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .semantics { testTag = "report_screen" },
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Period selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = uiState.period == ReportPeriod.WEEKLY,
                    onClick = { viewModel.setPeriod(ReportPeriod.WEEKLY) },
                    label = { Text("Weekly") },
                    modifier = Modifier.semantics { testTag = "period_weekly" },
                )
                FilterChip(
                    selected = uiState.period == ReportPeriod.MONTHLY,
                    onClick = { viewModel.setPeriod(ReportPeriod.MONTHLY) },
                    label = { Text("Monthly") },
                    modifier = Modifier.semantics { testTag = "period_monthly" },
                )
            }

            if (uiState.showClearConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { viewModel.onClearDismissed() },
                    title = { Text("Clear Last Week?") },
                    text = { Text("This will delete all meal logs and reset statuses to pending for every day since last Saturday. This cannot be undone.") },
                    confirmButton = {
                        TextButton(
                            onClick = { viewModel.onClearConfirmed() },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        ) { Text("Clear") }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.onClearDismissed() }) { Text("Cancel") }
                    },
                    modifier = Modifier.semantics { testTag = "clear_confirm_dialog" },
                )
            }

            if (uiState.clearSuccess) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { testTag = "clear_success_card" },
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Last week cleared",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { viewModel.onClearSuccessDismissed() }) { Text("Dismiss") }
                    }
                }
            }

            val data = uiState.data
            if (uiState.error != null) {
                val msg = (uiState.error as? com.diet.dietreport.AppError.DatabaseError)?.message
                    ?: uiState.error.toString()
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        msg,
                        modifier = Modifier
                            .padding(16.dp)
                            .semantics { testTag = "report_error" },
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            } else if (data == null) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                // Pie chart card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    ),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("Success vs Failure", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(12.dp))

                        val successColor = MaterialTheme.colorScheme.primary
                        val failColor = MaterialTheme.colorScheme.error
                        val emptyColor = MaterialTheme.colorScheme.surfaceVariant
                        val failCount = data.overallTotal - data.overallSuccess

                        Box(
                            modifier = Modifier.size(160.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Canvas(
                                modifier = Modifier
                                    .size(160.dp)
                                    .semantics { testTag = "pie_chart" },
                            ) {
                                val strokeWidth = 28f
                                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                                val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                                if (data.overallTotal == 0) {
                                    drawArc(
                                        color = emptyColor,
                                        startAngle = 0f,
                                        sweepAngle = 360f,
                                        useCenter = false,
                                        topLeft = topLeft,
                                        size = arcSize,
                                        style = Stroke(width = strokeWidth),
                                    )
                                } else {
                                    val successSweep = 360f * data.overallSuccess / data.overallTotal
                                    val failSweep = 360f - successSweep
                                    drawArc(
                                        color = successColor,
                                        startAngle = -90f,
                                        sweepAngle = successSweep,
                                        useCenter = false,
                                        topLeft = topLeft,
                                        size = arcSize,
                                        style = Stroke(width = strokeWidth),
                                    )
                                    if (failSweep > 0f) {
                                        drawArc(
                                            color = failColor,
                                            startAngle = -90f + successSweep,
                                            sweepAngle = failSweep,
                                            useCenter = false,
                                            topLeft = topLeft,
                                            size = arcSize,
                                            style = Stroke(width = strokeWidth),
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "${data.overallPercent}%",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.semantics { testTag = "overall_percent" },
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Legend
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Canvas(modifier = Modifier.size(12.dp)) {
                                    drawCircle(color = successColor)
                                }
                                Text(
                                    " On time: ${data.overallSuccess}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Canvas(modifier = Modifier.size(12.dp)) {
                                    drawCircle(color = failColor)
                                }
                                Text(
                                    " Missed: $failCount",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }

                // By Day
                if (data.byDay.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "By Day",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                            data.byDay.forEachIndexed { index, dayStat ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        dayStat.dateLabel,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f),
                                    )
                                    LinearProgressIndicator(
                                        progress = { dayStat.percent / 100f },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    )
                                    Text(
                                        text = "${dayStat.percent}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier
                                            .padding(start = 12.dp)
                                            .semantics { testTag = "day_row_$index" },
                                    )
                                }
                            }
                        }
                    }
                }

                // By Hour
                if (data.byHour.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "By Hour",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                            data.byHour.forEach { hourStat ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        "%02d:00".format(hourStat.hour),
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f),
                                    )
                                    LinearProgressIndicator(
                                        progress = { hourStat.percent / 100f },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    )
                                    Text(
                                        text = "${hourStat.percent}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier
                                            .padding(start = 12.dp)
                                            .semantics { testTag = "hour_row_${hourStat.hour}" },
                                    )
                                }
                            }
                        }
                    }
                }

                // Off-schedule meals (warning)
                if (data.offScheduleCount > 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                        ),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp),
                            )
                            Column {
                                Text(
                                    "Unscheduled Meals",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                                Text(
                                    "${data.offScheduleCount} meal${if (data.offScheduleCount != 1) "s" else ""} logged outside your schedule — these do not count toward your goal",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.semantics { testTag = "off_schedule_count" },
                                )
                            }
                        }
                    }
                }

                // Time-of-Day buckets
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Time of Day",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        data.buckets.forEachIndexed { index, bucket ->
                            val tag = when (index) {
                                0 -> "bucket_morning_pct"
                                1 -> "bucket_afternoon_pct"
                                else -> "bucket_evening_pct"
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    bucket.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f),
                                )
                                LinearProgressIndicator(
                                    progress = { bucket.percent / 100f },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                )
                                Text(
                                    text = "${bucket.percent}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier
                                        .padding(start = 12.dp)
                                        .semantics { testTag = tag },
                                )
                            }
                        }
                    }
                }
            }
        }

        // Share button
        Button(
            onClick = {
                val reportData = uiState.data ?: return@Button
                val text = buildShareText(uiState.period, reportData)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                context.startActivity(Intent.createChooser(intent, "Share report"))
            },
            enabled = uiState.data != null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .height(50.dp)
                .semantics { testTag = "share_button" },
            shape = MaterialTheme.shapes.medium,
        ) {
            Icon(
                Icons.Default.Share,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text("Share", modifier = Modifier.padding(start = 8.dp))
        }

        // Clear Last Week button
        OutlinedButton(
            onClick = { viewModel.onClearLastWeekClick() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .height(50.dp)
                .semantics { testTag = "clear_week_button" },
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
        ) {
            Text("Clear Last Week")
        }
    }
}
