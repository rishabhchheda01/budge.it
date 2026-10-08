package com.rishabh.budgetapp.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

// Battery saver can pause the app's midnight refresh, so the widget would show yesterday's numbers.
// Android lets an app ask to be "exempt" (not restricted). Battery saver itself stays ON for
// everything else on the phone. This file holds the check and the popup that asks.

/** True when battery saver is on AND this app is not exempt yet, so the popup is worth showing. */
fun Context.needsBatteryExemption(): Boolean {
    val power = getSystemService(Context.POWER_SERVICE) as PowerManager
    return power.isPowerSaveMode && !power.isIgnoringBatteryOptimizations(packageName)
}

/** Opens Android's own "let this app run in the background?" question for this app. */
fun Context.requestBatteryExemption() {
    val ask = Intent(
        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
        Uri.parse("package:$packageName")
    )
    // If a phone refuses the direct question, fall back to the general battery settings list.
    runCatching { startActivity(ask) }.onFailure {
        runCatching { startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
    }
}

@Composable
fun BatterySaverDialog(onAllow: () -> Unit, onNotNow: () -> Unit) {
    AlertDialog(
        onDismissRequest = onNotNow,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = { Text("Battery saver is on") },
        text = {
            Text(
                "Battery saver can stop the widget from updating at midnight. " +
                    "Allow budge.it to run in the background so it stays correct. " +
                    "Battery saver stays on for everything else."
            )
        },
        confirmButton = { TextButton(onClick = onAllow) { Text("Allow") } },
        dismissButton = {
            TextButton(onClick = onNotNow) {
                Text("Not now", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}
