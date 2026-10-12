package com.rishabh.budgetapp.widget

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.rishabh.budgetapp.ThemeMode
import com.rishabh.budgetapp.data.BudgetPreferences
import com.rishabh.budgetapp.data.TransactionRepository
import com.rishabh.budgetapp.ui.TransactionDialog
import com.rishabh.budgetapp.ui.theme.BudgetTheme
import com.rishabh.budgetapp.ui.theme.resolveDarkTheme
import kotlinx.coroutines.launch

// How the home screen behind the add card is softened so the card stands out. To taste:
private const val BLUR_RADIUS_DP = 20        // how blurry (Android 12+ only). Bigger = blurrier
private const val DIM_WITH_BLUR = 0.15f      // a light darkening on top of the blur (0 = none, 1 = black)
private const val DIM_WITHOUT_BLUR = 0.45f   // a stronger darkening when the phone cannot blur (older Android,
                                             // or blur turned off, e.g. by battery saver)

/**
 * Opened by the "+" button on the widget. It is see-through, so you just see the add card
 * floating over your home screen, blurred (or darkened, if the phone cannot blur) so the card stands out.
 * Save adds the transaction, refreshes the widget, and closes.
 */
class QuickAddActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        softenBackground()
        val preferences = BudgetPreferences(applicationContext)
        setContent {
            val themeMode by preferences.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            BudgetTheme(darkTheme = themeMode.resolveDarkTheme()) {
                TransactionDialog(
                    title = "Add transaction",
                    onDismiss = { finish() },
                    onConfirm = { label, amount ->
                        lifecycleScope.launch {
                            TransactionRepository(applicationContext).add(label, amount)
                            finish()
                        }
                    }
                )
            }
        }
    }

    /**
     * Blurs what is behind this screen (the home screen) when the phone can, and darkens it a little either
     * way. The darkening is drawn by this screen itself, so it works on every Android version.
     */
    private fun softenBackground() {
        val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && windowManager.isCrossWindowBlurEnabled
        if (canBlur) {
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            val params = window.attributes
            params.blurBehindRadius = (BLUR_RADIUS_DP * resources.displayMetrics.density).toInt()
            window.attributes = params
        }
        val dim = if (canBlur) DIM_WITH_BLUR else DIM_WITHOUT_BLUR
        window.setBackgroundDrawable(ColorDrawable(Color.argb((dim * 255).toInt(), 0, 0, 0)))
    }
}
