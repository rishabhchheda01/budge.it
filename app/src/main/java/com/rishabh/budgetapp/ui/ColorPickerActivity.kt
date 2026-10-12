package com.rishabh.budgetapp.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.rishabh.budgetapp.ThemeMode
import com.rishabh.budgetapp.data.BudgetPreferences
import com.rishabh.budgetapp.ui.theme.BudgetTheme
import com.rishabh.budgetapp.ui.theme.WidgetBackground
import com.rishabh.budgetapp.ui.theme.resolveDarkTheme
import com.rishabh.budgetapp.widget.BudgetWidget
import com.rishabh.budgetapp.widget.widgetStates
import kotlinx.coroutines.launch

/**
 * The widget color screen. The window is see-through to your wallpaper (see Theme.BudgetApp.Wallpaper in
 * res/values/themes.xml), so you can judge the color against it. From top to bottom:
 *  - a "Done" button: saves the color and closes (the phone's back button closes WITHOUT saving),
 *  - a look-alike of the widget that changes color as you pick,
 *  - a strip of colors with arrows.
 */
class ColorPickerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val preferences = BudgetPreferences(applicationContext)
        val states = widgetStates(applicationContext)

        setContent {
            val themeMode by preferences.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            BudgetTheme(darkTheme = themeMode.resolveDarkTheme()) {
                // null = still loading. The color you tapped (if any) wins over the saved one.
                val saved by preferences.widgetColor.collectAsState(initial = null)
                val state by states.collectAsState(initial = null)
                var pickedArgb by rememberSaveable { mutableStateOf<Int?>(null) }
                val selected: Color = pickedArgb?.let { Color(it) } ?: saved ?: WidgetBackground
                // The real widget's size on this phone, so the look-alike is exactly as big (null = no widget yet).
                var widgetSize by remember { mutableStateOf<DpSize?>(null) }
                LaunchedEffect(Unit) { widgetSize = BudgetWidget.currentSize(applicationContext) }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.systemBars)
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top: the Done button.
                    Button(
                        onClick = {
                            lifecycleScope.launch {
                                preferences.setWidgetColor(selected)
                                BudgetWidget.refreshAll(applicationContext)
                                finish()
                            }
                        },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .width(140.dp)
                            .height(48.dp)
                    ) {
                        Text("Done", fontSize = 17.sp, fontWeight = FontWeight.Medium)
                    }

                    // Middle: the look-alike widget, centered in the space between.
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        state?.let { loaded ->
                            val real = widgetSize
                            if (real != null) {
                                WidgetPreview(
                                    summary = loaded.summary,
                                    background = selected,
                                    width = real.width,
                                    height = real.height
                                )
                            } else {
                                WidgetPreview(summary = loaded.summary, background = selected)
                            }
                        }
                    }

                    // Bottom: the colors.
                    ColorStrip(
                        selected = selected,
                        startColor = saved,
                        onSelect = { pickedArgb = it.toArgb() },
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                }
            }
        }
    }
}
