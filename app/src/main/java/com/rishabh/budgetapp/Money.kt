package com.rishabh.budgetapp

import java.text.NumberFormat
import java.util.Locale

/**
 * Every amount in the app and the widget is formatted here, so the currency lives in ONE place.
 * It is US dollars for now. To use another currency later, change Locale.US (for example to
 * Locale.UK for pounds) and every screen follows.
 */
fun currencyFormat(): NumberFormat = NumberFormat.getCurrencyInstance(Locale.US)
