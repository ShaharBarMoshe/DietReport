package com.diet.dietreport.meals

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.diet.dietreport.data.db.LogSource
import java.io.File

private const val TAG = "DR/Meals"

@Composable
fun LogMealScreen(
    viewModel: LogMealViewModel,
    onNavigateToLock: (slotId: Long) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.isConfirmed) {
        if (uiState.isConfirmed) onNavigateToLock(viewModel.slotId)
    }

    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
        }
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        try {
            val dest = File(context.filesDir, "meals/meal_${System.currentTimeMillis()}.jpg")
                .also { it.parentFile?.mkdirs() }
            context.contentResolver.openInputStream(uri)
                ?.use { input -> dest.outputStream().use { input.copyTo(it) } }
            viewModel.onPhotoCaptured(dest.absolutePath, LogSource.GALLERY)
        } catch (e: Exception) {
            viewModel.onCameraError(e)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics { testTag = "log_meal_screen" },
    ) {
        if (uiState.error != null) {
            val msg = (uiState.error as? com.diet.dietreport.AppError.DatabaseError)?.message
                ?: uiState.error.toString()
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.BottomCenter),
            ) {
                Text(
                    msg,
                    modifier = Modifier
                        .padding(16.dp)
                        .semantics { testTag = "log_meal_error" },
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        if (uiState.photoPath != null) {
            PhotoPreview(
                photoPath = uiState.photoPath!!,
                onConfirm = viewModel::confirm,
                onRetake = viewModel::clearPhoto,
            )
        } else if (hasCameraPermission) {
            CameraPreview(
                controller = controller,
                context = context,
                onPhotoTaken = { path -> viewModel.onPhotoCaptured(path, LogSource.CAMERA) },
                onCameraError = viewModel::onCameraError,
                onGalleryClick = { galleryLauncher.launch("image/*") },
            )
        } else {
            PermissionRequest(
                onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                onGalleryClick = { galleryLauncher.launch("image/*") },
            )
        }
    }
}

@Composable
private fun PhotoPreview(
    photoPath: String,
    onConfirm: () -> Unit,
    onRetake: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        val bitmap = remember(photoPath) {
            BitmapFactory.decodeFile(photoPath)?.asImageBitmap()
        }
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Meal photo preview",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .semantics { testTag = "photo_preview" },
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .semantics { testTag = "photo_preview" },
                contentAlignment = Alignment.Center,
            ) {
                Text("Unable to load image", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onRetake,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .semantics { testTag = "retake_button" },
                shape = MaterialTheme.shapes.medium,
            ) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Retake", modifier = Modifier.padding(start = 8.dp))
            }

            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .semantics { testTag = "confirm_button" },
                shape = MaterialTheme.shapes.medium,
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Confirm", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun CameraPreview(
    controller: LifecycleCameraController,
    context: Context,
    onPhotoTaken: (String) -> Unit,
    onCameraError: (Exception) -> Unit,
    onGalleryClick: () -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    Column(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).also {
                    it.controller = controller
                    controller.bindToLifecycle(lifecycleOwner)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .semantics { testTag = "camera_preview" },
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FilledTonalButton(
                onClick = onGalleryClick,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .semantics { testTag = "pick_gallery_button" },
                shape = MaterialTheme.shapes.medium,
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Gallery", modifier = Modifier.padding(start = 8.dp))
            }

            Button(
                onClick = {
                    val dest = File(context.filesDir, "meals/meal_${System.currentTimeMillis()}.jpg")
                        .also { it.parentFile?.mkdirs() }
                    controller.takePicture(
                        ImageCapture.OutputFileOptions.Builder(dest).build(),
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                onPhotoTaken(dest.absolutePath)
                            }
                            override fun onError(e: ImageCaptureException) {
                                Log.e(TAG, "Photo capture failed", e)
                                onCameraError(e)
                            }
                        },
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .semantics { testTag = "take_photo_button" },
                shape = MaterialTheme.shapes.medium,
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Capture", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun PermissionRequest(
    onRequestPermission: () -> Unit,
    onGalleryClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outlineVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Camera permission needed to take meal photos",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 32.dp),
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRequestPermission,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .height(50.dp),
            shape = MaterialTheme.shapes.medium,
        ) { Text("Grant Camera Permission") }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = onGalleryClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .height(50.dp)
                .semantics { testTag = "pick_gallery_button" },
            shape = MaterialTheme.shapes.medium,
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("Pick from Gallery", modifier = Modifier.padding(start = 8.dp))
        }
    }
}
