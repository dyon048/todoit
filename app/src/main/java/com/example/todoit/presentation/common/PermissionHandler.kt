package com.example.todoit.presentation.common

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/** Returns true if ACCESS_FINE_LOCATION has already been granted. */
fun Context.hasLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

/**
 * Requests the full set of runtime permissions needed for location scoring and reminders:
 *  - ACCESS_FINE_LOCATION / ACCESS_COARSE_LOCATION
 *  - POST_NOTIFICATIONS (API 33+)
 *
 * [onLocationGranted] fires whenever ACCESS_FINE_LOCATION is (or becomes) granted.
 * Shows a rationale dialog before launching the system prompt.
 */
@Composable
fun AppPermissionsHandler(onLocationGranted: () -> Unit) {
    val context = LocalContext.current
    var showRationale by remember { mutableStateOf(false) }
    var permissionsRequested by remember { mutableStateOf(false) }

    val permissions = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            onLocationGranted()
        }
    }

    // If already granted on composition — fire immediately.
    LaunchedEffect(Unit) {
        if (context.hasLocationPermission()) {
            onLocationGranted()
        } else if (!permissionsRequested) {
            showRationale = true
        }
    }

    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            title = { Text("Permissions needed") },
            text = {
                Text(
                    "TodoIt uses your location to score nearby tasks higher and to send " +
                    "proximity reminders. Notification permission lets us deliver task alerts. " +
                    "No location data is sent off your device."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showRationale = false
                    permissionsRequested = true
                    launcher.launch(permissions.toTypedArray())
                }) { Text("Allow") }
            },
            dismissButton = {
                TextButton(onClick = { showRationale = false }) { Text("Not now") }
            },
        )
    }
}
