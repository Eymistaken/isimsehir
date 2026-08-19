package com.eymistaken.isimsehir.ui.theme

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.eymistaken.isimsehir.model.AccentColor
import com.eymistaken.isimsehir.ui.haptics.Haptics
import com.eymistaken.isimsehir.ui.haptics.LocalHaptics

/**
 * Vurgu rengi tek bir ayar; tasarımda dört seçenek var ve tüm ekranlar
 * bunu okuyor. Renk teması derinlemesine geçtiği için CompositionLocal.
 */
val LocalAccent = staticCompositionLocalOf { AccentColor.Amber.color }

@Composable
fun IsimSehirTheme(
    accent: Color = AccentColor.Amber.color,
    haptics: Haptics = Haptics.None,
    content: @Composable () -> Unit,
) {
    val scheme = darkColorScheme(
        primary = accent,
        onPrimary = Ink,
        background = Ink,
        onBackground = Cream,
        surface = Panel,
        onSurface = Cream,
        error = Danger,
        onError = Cream,
    )
    CompositionLocalProvider(
        LocalAccent provides accent,
        LocalHaptics provides haptics,
        LocalTextStyle provides AppText.body.copy(color = Cream),
    ) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
