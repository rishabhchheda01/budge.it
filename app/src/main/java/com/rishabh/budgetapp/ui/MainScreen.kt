package com.rishabh.budgetapp.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rishabh.budgetapp.BudgetPeriod
import com.rishabh.budgetapp.BudgetSummary
import com.rishabh.budgetapp.ThemeMode
import com.rishabh.budgetapp.budgetDate
import com.rishabh.budgetapp.currencyFormat
import com.rishabh.budgetapp.data.Transaction
import com.rishabh.budgetapp.ui.theme.KdamThmorPro
import com.rishabh.budgetapp.ui.theme.progressColor
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

// Colors, fonts and corner sizes live in ui/theme/Theme.kt. Change them there.

@Composable
fun MainScreen(
    budgetPeriod: BudgetPeriod,
    budgetAmount: Double,
    isBudgetSet: Boolean,
    summary: BudgetSummary,
    transactions: List<Transaction>,
    onBudgetPeriodChange: (BudgetPeriod) -> Unit,
    onConfirmBudget: (Double) -> Unit,
    onAddTransaction: (label: String, amount: Double) -> Unit,
    onUpdateTransaction: (Transaction) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit,
    onRestoreTransaction: (Transaction) -> Unit,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onClearTransactions: () -> Unit,
    rollover: LocalTime = LocalTime.MIDNIGHT,
    onRolloverChange: (LocalTime) -> Unit = {}
) {
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    // The settings page is just a second view inside this screen: a true/false switch.
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val inSettings = showSettings && isBudgetSet
    BackHandler(enabled = inSettings) { showSettings = false } // the phone's back button closes settings

    // Delete right away, then offer "Undo" for a few seconds.
    val deleteWithUndo: (Transaction) -> Unit = { transaction ->
        onDeleteTransaction(transaction)
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = "Transaction deleted",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) onRestoreTransaction(transaction)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (!inSettings) {
                AddTransactionButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 32.dp)
                )
            }
        }
    ) { padding ->
        // Three possible views. Crossfade fades smoothly from one to the next.
        // MAIN keeps the logo and today's budget fixed; only the transactions list scrolls.
        val view = when {
            inSettings -> ScreenView.SETTINGS
            isBudgetSet -> ScreenView.MAIN
            else -> ScreenView.SETUP
        }
        val tapToDismissKeyboard = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { focusManager.clearFocus() })
        }

        Crossfade(
            targetState = view,
            animationSpec = tween(durationMillis = 300),
            label = "screen"
        ) { current ->
            if (current == ScreenView.MAIN) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .then(tapToDismissKeyboard)
                        .padding(horizontal = 24.dp)
                        .padding(top = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Fixed part: does not move.
                    AppLogo()
                    BudgetHero(
                        budgetPeriod = budgetPeriod,
                        summary = summary,
                        onOpenSettings = { showSettings = true }
                    )
                    // Scrolling part: takes all the remaining height.
                    TransactionsSection(
                        transactions = transactions,
                        onEdit = { editingTransaction = it },
                        onDelete = deleteWithUndo,
                        rollover = rollover,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .then(tapToDismissKeyboard)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (current == ScreenView.SETTINGS) {
                        SettingsContent(
                            budgetPeriod = budgetPeriod,
                            budgetAmount = budgetAmount,
                            onBudgetPeriodChange = onBudgetPeriodChange,
                            onConfirmBudget = onConfirmBudget,
                            themeMode = themeMode,
                            onThemeModeChange = onThemeModeChange,
                            onClearTransactions = onClearTransactions,
                            rollover = rollover,
                            onRolloverChange = onRolloverChange,
                            onBack = { showSettings = false }
                        )
                    } else {
                        AppLogo()
                        BudgetSetupForm(
                            budgetPeriod = budgetPeriod,
                            budgetAmount = budgetAmount,
                            onBudgetPeriodChange = onBudgetPeriodChange,
                            onConfirmBudget = onConfirmBudget
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        TransactionDialog(
            title = "Add transaction",
            onDismiss = { showAddDialog = false },
            onConfirm = { label, amount ->
                onAddTransaction(label, amount)
                showAddDialog = false
            }
        )
    }

    editingTransaction?.let { transaction ->
        TransactionDialog(
            title = "Edit transaction",
            initialLabel = transaction.label,
            initialAmount = formatAmountForEditing(transaction.amount),
            onDismiss = { editingTransaction = null },
            onConfirm = { label, amount ->
                onUpdateTransaction(transaction.copy(label = label, amount = amount))
                editingTransaction = null
            }
        )
    }
}

private enum class ScreenView { MAIN, SETTINGS, SETUP }

@Composable
private fun AppLogo() {
    Text(
        text = "budge.it",
        fontFamily = KdamThmorPro,
        fontSize = 56.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun AddTransactionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Colors come from the theme: black with light text in light mode, the reverse in dark mode.
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Text(
            text = "Add transaction",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun BudgetSetupForm(
    budgetPeriod: BudgetPeriod,
    budgetAmount: Double,
    onBudgetPeriodChange: (BudgetPeriod) -> Unit,
    onConfirmBudget: (Double) -> Unit
) {
    // The text being typed lives here; the amount is only saved when "Set budget" is pressed.
    var amountText by remember(budgetAmount) { mutableStateOf(formatAmountForEditing(budgetAmount)) }
    val quietText = MaterialTheme.colorScheme.onSurfaceVariant
    val perLabel = if (budgetPeriod == BudgetPeriod.DAILY) "per day" else "per week"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Set your budget",
            fontSize = 28.sp,
            fontWeight = FontWeight.Light
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "How much do you want to spend?",
            fontSize = 15.sp,
            color = quietText
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Daily / Weekly switch: one pill with the chosen side filled in.
        SegmentedToggle(
            options = listOf("Daily" to BudgetPeriod.DAILY, "Weekly" to BudgetPeriod.WEEKLY),
            selected = budgetPeriod,
            onSelect = onBudgetPeriodChange
        )

        Spacer(modifier = Modifier.height(48.dp))

        AmountField(
            value = amountText,
            onValueChange = { amountText = it }
        )
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .padding(top = 8.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = perLabel, fontSize = 15.sp, color = quietText)

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = { amountText.toDoubleOrNull()?.let(onConfirmBudget) },
            enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0.0,
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(52.dp)
        ) {
            Text("Set budget", fontSize = 18.sp, fontWeight = FontWeight.Medium)
        }
    }
}

/**
 * A big, borderless number entry: the currency symbol and the amount in the Kdam font.
 * Used by both the budget setup and the add/edit transaction card.
 */
@Composable
internal fun AmountField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null
) {
    val symbol = remember { currencyFormat().currency?.symbol ?: "$" }
    val quietText = MaterialTheme.colorScheme.onSurfaceVariant
    val numberStyle = TextStyle(
        fontFamily = KdamThmorPro,
        fontSize = 48.sp,
        color = MaterialTheme.colorScheme.onBackground
    )

    BasicTextField(
        value = value,
        onValueChange = { input ->
            // Only digits and a single decimal point.
            onValueChange(
                input.filterIndexed { index, c ->
                    c.isDigit() || (c == '.' && input.indexOf('.') == index)
                }
            )
        },
        textStyle = numberStyle,
        singleLine = true,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier
            .fillMaxWidth()
            .testTag("amountField") // lets automated UI tests find this field
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = symbol, style = numberStyle.copy(fontSize = 28.sp, color = quietText))
                Spacer(modifier = Modifier.width(6.dp))
                // IntrinsicSize.Min makes this box exactly as wide as the number, so the whole
                // "$ + number" group sits in the middle instead of the number hugging the left.
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.width(IntrinsicSize.Min)
                ) {
                    // Faint "0" shown until you start typing.
                    if (value.isEmpty()) {
                        Text(text = "0", style = numberStyle.copy(color = quietText.copy(alpha = 0.5f)))
                    }
                    innerTextField()
                }
            }
        }
    )
}

/** The centerpiece: how much is left, big, with the progress bar underneath. */
@Composable
private fun BudgetHero(
    budgetPeriod: BudgetPeriod,
    summary: BudgetSummary,
    onOpenSettings: () -> Unit
) {
    val currency = remember { currencyFormat() }
    val quietText = MaterialTheme.colorScheme.onSurfaceVariant
    val periodLabel = if (budgetPeriod == BudgetPeriod.DAILY) "DAILY BUDGET" else "TODAY'S BUDGET"
    // Animating the progress makes the bar (and its color) glide when the numbers change.
    // Change the 900 (milliseconds) to make the glide faster or slower.
    val animatedProgress by animateFloatAsState(
        targetValue = summary.progress,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "progress"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = periodLabel,
                fontSize = 13.sp,
                letterSpacing = 1.5.sp,
                color = quietText,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = quietText,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Shows a minus sign when you are over budget, e.g. -$12.00
        AutoSizeText(
            text = currency.format(summary.left),
            maxFontSize = 72.sp,
            fontFamily = KdamThmorPro,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(32.dp))

        LinearProgressIndicator(
            progress = { animatedProgress },
            color = progressColor(animatedProgress),
            trackColor = MaterialTheme.colorScheme.outlineVariant,
            gapSize = 0.dp,
            drawStopIndicator = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50))
        )

        Spacer(modifier = Modifier.height(12.dp))

        // How much of today's limit you have used.
        Text(
            text = "${currency.format(summary.spent)} of ${currency.format(summary.limit)} today",
            fontSize = 15.sp,
            color = quietText
        )

        // Weekly budgets also show what is left of the whole week.
        if (summary.weeklyLeft != null && summary.daysLeft != null) {
            val dayWord = if (summary.daysLeft == 1) "day" else "days"
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${currency.format(summary.weeklyLeft)} left this week · ${summary.daysLeft} $dayWord left",
                fontSize = 15.sp,
                color = quietText
            )
        }
    }
}

/**
 * Every stored transaction (the last 2 weeks), newest first, grouped by day.
 * A LazyColumn is a scrolling list that only draws the rows on screen. animateItem() makes rows
 * slide, fade in and fade out when you add or delete one, instead of jumping.
 */
@Composable
private fun TransactionsSection(
    transactions: List<Transaction>,
    onEdit: (Transaction) -> Unit,
    onDelete: (Transaction) -> Unit,
    rollover: LocalTime,
    modifier: Modifier = Modifier
) {
    val currency = remember { currencyFormat() }
    val timeFormat = remember { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT) }
    val zone = remember { ZoneId.systemDefault() }
    val today = budgetDate(ZonedDateTime.now(), rollover)
    val quietText = MaterialTheme.colorScheme.onSurfaceVariant
    // Each transaction goes under the budget day it belongs to (a 2 am one counts toward the day before if
    // the day starts at 5 am).
    // The list is already newest-first, so grouping keeps the days in order too.
    val byDay = remember(transactions, rollover) {
        transactions.groupBy { budgetDate(Instant.ofEpochMilli(it.timestamp).atZone(zone), rollover) }
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(top = 24.dp, bottom = 16.dp)
    ) {
        if (transactions.isEmpty()) {
            item(key = "empty") {
                Text(
                    text = "No spending yet",
                    fontSize = 16.sp,
                    color = quietText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .animateItem()
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                )
            }
        }
        byDay.forEach { (date, dayTransactions) ->
            // "key" tells the list which row is which, so it can animate the right one.
            item(key = "day-$date") {
                Text(
                    text = dayLabel(date, today).uppercase(),
                    fontSize = 12.sp,
                    letterSpacing = 1.5.sp,
                    color = quietText,
                    modifier = Modifier
                        .animateItem()
                        .padding(top = 16.dp, bottom = 4.dp)
                )
            }
            itemsIndexed(dayTransactions, key = { _, t -> t.id }) { index, transaction ->
                Column(modifier = Modifier.animateItem()) {
                    if (index > 0) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    TransactionRow(
                        transaction = transaction,
                        time = Instant.ofEpochMilli(transaction.timestamp).atZone(zone).format(timeFormat),
                        currency = currency,
                        onEdit = onEdit,
                        onDelete = onDelete
                    )
                }
            }
        }
    }
}

// "Today", "Yesterday", or a date like "Mon, Oct 5".
private fun dayLabel(date: LocalDate, today: LocalDate): String = when (date) {
    today -> "Today"
    today.minusDays(1) -> "Yesterday"
    else -> date.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
}

/** One flat row. Tap the row to edit it; tap the bin to delete it. */
@Composable
private fun TransactionRow(
    transaction: Transaction,
    time: String,
    currency: NumberFormat,
    onEdit: (Transaction) -> Unit,
    onDelete: (Transaction) -> Unit
) {
    val quietText = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit(transaction) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.label,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium
            )
            Text(text = time, fontSize = 13.sp, color = quietText)
        }
        Text(
            text = "-${currency.format(transaction.amount)}",
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium
        )
        IconButton(onClick = { onDelete(transaction) }) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete transaction",
                tint = quietText,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// The add/edit card. Shared by the app and the widget's quick-add.
@Composable
fun TransactionDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (label: String, amount: Double) -> Unit,
    initialLabel: String = "",
    initialAmount: String = ""
) {
    var label by remember { mutableStateOf(initialLabel) }
    var amountText by remember { mutableStateOf(initialAmount) }
    val amount = amountText.toDoubleOrNull()
    val canSave = label.isNotBlank() && amount != null && amount > 0
    val quietText = MaterialTheme.colorScheme.onSurfaceVariant
    val focusRequester = remember { FocusRequester() }

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
                // Open the keyboard on the amount straight away. This must live INSIDE the dialog's
                // content: the amount field only exists once the dialog is on screen, and asking for
                // focus any earlier crashes. runCatching means the worst case is "no auto-keyboard".
                LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

                Text(
                    text = title.uppercase(),
                    fontSize = 13.sp,
                    letterSpacing = 1.5.sp,
                    color = quietText
                )

                Spacer(modifier = Modifier.height(24.dp))

                AmountField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    focusRequester = focusRequester
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    placeholder = { Text("What was it for?") },

                    singleLine = true,
                    shape = RoundedCornerShape(50),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (canSave) onConfirm(label, amount!!)
                    }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("descriptionField")
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { if (canSave) onConfirm(label, amount!!) },
                    enabled = canSave,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text("Save", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = quietText)
                }
            }
        }
    }
}

// Turns the saved budget back into text for the amount field: 0 -> "", 50.0 -> "50", 12.5 -> "12.5".
internal fun formatAmountForEditing(amount: Double): String = when {
    amount <= 0.0 -> ""
    amount % 1.0 == 0.0 -> amount.toLong().toString()
    else -> amount.toString()
}

/**
 * A single-line Text that shrinks its font size until it fits the available width,
 * instead of wrapping. Re-measures whenever [text] or the layout width changes, so it
 * stays correctly sized across different screens/card widths.
 */
@Composable
private fun AutoSizeText(
    text: String,
    maxFontSize: TextUnit,
    modifier: Modifier = Modifier,
    minFontSize: TextUnit = 16.sp,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    textAlign: TextAlign = TextAlign.Center,
    color: Color = Color.Unspecified
) {
    var fontSize by remember(text, maxFontSize) { mutableStateOf(maxFontSize) }
    var readyToDraw by remember(text, maxFontSize) { mutableStateOf(false) }

    Text(
        text = text,
        fontSize = fontSize,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        textAlign = textAlign,
        color = color,
        maxLines = 1,
        softWrap = false,
        modifier = modifier.drawWithContent { if (readyToDraw) drawContent() },
        onTextLayout = { result ->
            if (result.didOverflowWidth && fontSize.value > minFontSize.value) {
                fontSize = (fontSize.value - 2f).coerceAtLeast(minFontSize.value).sp
            } else {
                readyToDraw = true
            }
        }
    )
}

/** A pill-shaped switch with several choices; the chosen one is filled in. Used for Daily/Weekly and the theme. */
@Composable
internal fun <T> SegmentedToggle(
    options: List<Pair<String, T>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.outlineVariant)
            .padding(4.dp)
            .selectableGroup()
    ) {
        options.forEach { (label, value) ->
            SegmentOption(
                label = label,
                selected = value == selected,
                onClick = { onSelect(value) }
            )
        }
    }
}

/** One choice inside a SegmentedToggle. Filled when selected, see-through when not. */
@Composable
private fun SegmentOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    // The fill fades in and out when you switch sides.
    val fill by animateColorAsState(if (selected) colors.primary else Color.Transparent, label = "fill")

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(fill)
            .selectable(selected = selected, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = if (selected) colors.onPrimary else colors.onBackground
        )
    }
}
