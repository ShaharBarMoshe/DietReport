package com.diet.dietreport.reports

import android.content.Intent
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
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
                // Overall stats card
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
                        Text("Overall Success", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${data.overallPercent}%",
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.semantics { testTag = "overall_percent" },
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "${data.overallSuccess} of ${data.overallTotal} meals logged on time",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { data.overallPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
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
    }
}
