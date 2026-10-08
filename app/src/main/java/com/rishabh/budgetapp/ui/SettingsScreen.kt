package com.rishabh.budgetapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rishabh.budgetapp.BudgetPeriod
import com.rishabh.budgetapp.ThemeMode
import java.time.LocalTime

/** The settings page: edit the budget, pick a theme, clear your transactions. */
@Composable
internal fun SettingsContent(
    budgetPeriod: BudgetPeriod,
    budgetAmount: Double,
    onBudgetPeriodChange: (BudgetPeriod) -> Unit,
    onConfirmBudget: (Double) -> Unit,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onClearTransactions: () -> Unit,
    rollover: LocalTime,
    onRolloverChange: (LocalTime) -> Unit,
    onBack: () -> Unit
) {
    // The text being typed lives here; the amount is only saved when "Save budget" is pressed.
    var amountText by remember(budgetAmount) { mutableStateOf(formatAmountForEditing(budgetAmount)) }
    var showClearConfirm by remember { mutableStateOf(false) }
    // The wheels' choice. It is only saved when "Save time" is pressed.
    var pickedHour by remember(rollover) { mutableStateOf(rollover.hour) }
    var pickedMinute by remember(rollover) { mutableStateOf(rollover.minute) }
    val pickedTime = LocalTime.of(pickedHour, pickedMinute)
    val quietText = MaterialTheme.colorScheme.onSurfaceVariant
    val typedAmount = amountText.toDoubleOrNull()
    val canSave = typedAmount != null && typedAmount > 0 && typedAmount != budgetAmount
    val perLabel = if (budgetPeriod == BudgetPeriod.DAILY) "per day" else "per week"

    Column(modifier = Modifier.fillMaxWidth()) {
        // The title is centered; the back arrow sits on the left edge.
        Box(modifier = Modifier.fillMaxWidth()) {
            IconButton(
                onClick = onBack,
                // A negative offset moves the arrow left. Make it more negative to go further left.
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (-12).dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Settings",
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // ---- Budget ----
        SectionLabel("BUDGET")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SegmentedToggle(
                options = listOf("Daily" to BudgetPeriod.DAILY, "Weekly" to BudgetPeriod.WEEKLY),
                selected = budgetPeriod,
                onSelect = onBudgetPeriodChange
            )
            Spacer(modifier = Modifier.height(32.dp))
            AmountField(value = amountText, onValueChange = { amountText = it })
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .padding(top = 8.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = perLabel, fontSize = 15.sp, color = quietText)
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { typedAmount?.let(onConfirmBudget) },
                enabled = canSave,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(52.dp)
            ) {
                Text("Save budget", fontSize = 18.sp, fontWeight = FontWeight.Medium)
            }
        }

        // ---- Day starts ----
        Spacer(modifier = Modifier.height(48.dp))
        SectionLabel("DAY STARTS")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TimeRoller(
                hour = pickedHour,
                minute = pickedMinute,
                onHourChange = { pickedHour = it },
                onMinuteChange = { pickedMinute = it }
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "A new day (and week) begins at %02d:%02d. Spending before then counts toward the day before."
                    .format(pickedHour, pickedMinute),
                fontSize = 13.sp,
                color = quietText,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { onRolloverChange(pickedTime) },
                enabled = pickedTime != rollover,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(52.dp)
            ) {
                Text("Save time", fontSize = 18.sp, fontWeight = FontWeight.Medium)
            }
        }

        // ---- Theme ----
        Spacer(modifier = Modifier.height(48.dp))
        SectionLabel("THEME")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SegmentedToggle(
                options = listOf(
                    "System" to ThemeMode.SYSTEM,
                    "Light" to ThemeMode.LIGHT,
                    "Dark" to ThemeMode.DARK
                ),
                selected = themeMode,
                onSelect = onThemeModeChange
            )
        }

        // ---- Data ----
        Spacer(modifier = Modifier.height(48.dp))
        SectionLabel("DATA")
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = { showClearConfirm = true },
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(
                text = "Clear all transactions",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Your budget and settings stay.",
            fontSize = 13.sp,
            color = quietText
        )
        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showClearConfirm) {
        ConfirmDialog(
            title = "Clear all transactions?",
            message = "This deletes every transaction and can't be undone.",
            confirmLabel = "Clear",
            onConfirm = {
                onClearTransactions()
                showClearConfirm = false
            },
            onDismiss = { showClearConfirm = false }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        letterSpacing = 1.5.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** A small "are you sure?" card in the same style as the add-transaction card. */
@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = title, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onConfirm,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(confirmLabel, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
