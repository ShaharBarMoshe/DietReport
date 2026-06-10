package com.diet.dietreport.meals

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.diet.dietreport.data.db.MealLog
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateTimeFormat = SimpleDateFormat("MMM d, yyyy  HH:mm", Locale.getDefault())

@Composable
fun PhotoGalleryScreen(viewModel: PhotoGalleryViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var photoToDelete by remember { mutableStateOf<MealLog?>(null) }
    var expandedPhoto by remember { mutableStateOf<MealLog?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .semantics { testTag = "photo_gallery_screen" },
    ) {
        Text(
            "Meal Photos",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )

        if (uiState.photos.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .semantics { testTag = "gallery_empty_state" },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outlineVariant,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "No photos yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(uiState.photos, key = { it.id }) { log ->
                    PhotoCard(
                        log = log,
                        onDelete = { photoToDelete = log },
                        onTap = { expandedPhoto = log },
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    photoToDelete?.let { log ->
        AlertDialog(
            onDismissRequest = { photoToDelete = null },
            title = { Text("Delete Photo") },
            text = { Text("This photo will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePhoto(log)
                    photoToDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { photoToDelete = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    // Full-screen photo viewer
    expandedPhoto?.let { log ->
        Dialog(
            onDismissRequest = { expandedPhoto = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { expandedPhoto = null },
                contentAlignment = Alignment.Center,
            ) {
                val file = File(log.photoPath)
                val bitmap = remember(log.photoPath) {
                    if (file.exists()) BitmapFactory.decodeFile(log.photoPath) else null
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Meal photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit,
                    )
                }
                // Close button
                IconButton(
                    onClick = { expandedPhoto = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                // Timestamp at bottom
                Text(
                    text = dateTimeFormat.format(Date(log.loggedAt)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp),
                )
            }
        }
    }
}

@Composable
private fun PhotoCard(
    log: MealLog,
    onDelete: () -> Unit,
    onTap: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { testTag = "photo_card_${log.id}" },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        onClick = onTap,
    ) {
        Box {
            val file = File(log.photoPath)
            val bitmap = remember(log.photoPath) {
                if (file.exists()) {
                    val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                    BitmapFactory.decodeFile(log.photoPath, opts)
                } else null
            }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Meal photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.BrokenImage,
                        contentDescription = "Missing photo",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(40.dp),
                    )
                }
            }

            // Delete button overlay
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp),
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete photo",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }

        // Timestamp
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = dateTimeFormat.format(Date(log.loggedAt)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
