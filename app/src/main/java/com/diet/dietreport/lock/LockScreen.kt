package com.diet.dietreport.lock

import android.app.Activity
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

@Composable
fun LockScreen(
    viewModel: LockViewModel,
    onNavigateToHome: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val activity = LocalContext.current as? Activity

    // Keep screen on during lock period
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Hide system bars (fullscreen immersive)
    DisposableEffect(Unit) {
        val controller = activity?.window?.insetsController
        controller?.hide(android.view.WindowInsets.Type.systemBars())
        controller?.systemBarsBehavior =
            WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        onDispose {
            controller?.show(android.view.WindowInsets.Type.systemBars())
        }
    }

    // Navigate to Home when done (timer expired or unlocked early)
    LaunchedEffect(uiState.done) {
        if (uiState.done) onNavigateToHome()
    }

    // Detect app backgrounding via ACTIVITY lifecycle ON_STOP.
    // We must observe the Activity lifecycle, not LocalLifecycleOwner, because inside a
    // NavHost composable LocalLifecycleOwner is the NavBackStackEntry — which fires ON_STOP
    // when the entry is popped during intra-app navigation (timer expiry → navigate to Home).
    // Observing the Activity lifecycle ensures ON_STOP only fires when the user actually
    // backgrounds the app (Home/Recents press, notification tap, etc.).
    val componentActivity = LocalContext.current as? ComponentActivity
    DisposableEffect(componentActivity) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                viewModel.onAppBackgrounded()
            }
        }
        componentActivity?.lifecycle?.addObserver(observer)
        onDispose { componentActivity?.lifecycle?.removeObserver(observer) }
    }

    // Back button triggers the same "unlock early" path
    BackHandler { viewModel.onUnlockEarly() }

    // Format remaining time as MM:SS
    val totalSeconds = ((uiState.remainingMs + 999L) / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val countdownText = "%02d:%02d".format(minutes, seconds)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .semantics { testTag = "lock_screen" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = countdownText,
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { testTag = "countdown_text" },
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Your phone is locked. Tapping Unlock will mark this meal as FAILED.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = viewModel::onUnlockEarly,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
            modifier = Modifier.semantics { testTag = "unlock_button" },
        ) {
            Text("Unlock (mark as failed)")
        }
    }
}
