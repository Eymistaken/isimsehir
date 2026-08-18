package com.eymistaken.isimsehir.ui.timer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.eymistaken.isimsehir.model.DURATION_PRESETS
import com.eymistaken.isimsehir.model.TimerState
import com.eymistaken.isimsehir.ui.components.tapNoRipple
import com.eymistaken.isimsehir.ui.theme.AppText
import com.eymistaken.isimsehir.ui.theme.Cream
import com.eymistaken.isimsehir.ui.theme.Ink
import com.eymistaken.isimsehir.ui.theme.InkDeep
import com.eymistaken.isimsehir.ui.theme.LocalAccent
import com.eymistaken.isimsehir.ui.theme.OnCream45
import com.eymistaken.isimsehir.ui.theme.OnCream50
import com.eymistaken.isimsehir.ui.theme.OnCream70
import kotlin.math.roundToInt

/**
 * Sürüklenebilir zamanlayıcı hapı. Tasarımdaki 7. ekranın üst köşedeki
 * accent rozeti; parmakla ekranda taşınabilir, dokununca panel açılır.
 */
@Composable
fun FloatingTimerPill(
    timer: TimerState,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = LocalAccent.current
    val density = LocalDensity.current

    BoxWithConstraints(modifier.fillMaxSize()) {
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val maxHeightPx = with(density) { maxHeight.toPx() }
        val pillWidthPx = with(density) { 108.dp.toPx() }
        val pillHeightPx = with(density) { 44.dp.toPx() }

        // Başlangıç: sağ üst köşe, 20dp kenar boşluğuyla.
        var offsetX by remember {
            mutableFloatStateOf(maxWidthPx - pillWidthPx - with(density) { 20.dp.toPx() })
        }
        var offsetY by remember { mutableFloatStateOf(with(density) { 20.dp.toPx() }) }

        Row(
            Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .width(108.dp)
                .height(44.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(accent)
                .pointerInput(maxWidthPx, maxHeightPx) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        offsetX = (offsetX + drag.x).coerceIn(0f, maxWidthPx - pillWidthPx)
                        offsetY = (offsetY + drag.y).coerceIn(0f, maxHeightPx - pillHeightPx)
                    }
                }
                .tapNoRipple(onClick = onOpen),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(timer.display, style = AppText.timerPanelPill, color = Ink)
        }
    }
}

/**
 * Zamanlayıcı paneli. Sayaç işlemiyorsa hazır süreler, işliyorsa kalan süre
 * ve durdur/iptal görünür — web sürümündeki popup zamanlayıcının karşılığı.
 */
@Composable
fun TimerPanel(
    timer: TimerState,
    onStart: (Int) -> Unit,
    onTogglePause: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
) {
    val accent = LocalAccent.current

    Box(
        Modifier
            .fillMaxSize()
            .background(InkDeep.copy(alpha = 0.5f))
            .tapNoRipple(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Cream)
                .tapNoRipple {}
                .padding(22.dp),
        ) {
            Text(
                "KALAN SÜRE",
                style = AppText.eyebrow(10, 0.20),
                color = OnCream50,
            )
            Text(
                timer.display,
                style = AppText.timerHuge,
                color = Ink,
                modifier = Modifier.padding(top = 6.dp, bottom = 4.dp),
            )

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Ink.copy(alpha = 0.12f)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(timer.progress.coerceIn(0f, 1f))
                        .height(4.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(accent),
                )
            }

            Spacer(Modifier.height(18.dp))

            if (timer.running || (timer.remainingSeconds < timer.totalSeconds && !timer.finished)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SolidCreamButton(
                        label = if (timer.running) "Durdur" else "Sürdür",
                        modifier = Modifier.weight(1f),
                        onClick = onTogglePause,
                    )
                    OutlineCreamButton(
                        label = "İptal",
                        modifier = Modifier.weight(1f),
                        onClick = onCancel,
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            Text(
                "HAZIR SÜRELER",
                style = AppText.eyebrow(10, 0.20),
                color = OnCream45,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DURATION_PRESETS.forEach { preset ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.5.dp, Ink.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                            .tapNoRipple { onStart(preset.seconds) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            preset.label,
                            style = AppText.buttonLabel(12, 0.0),
                            color = OnCream70,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SolidCreamButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(46.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(Ink)
            .tapNoRipple(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label.uppercase(TR), style = AppText.buttonLabel(12, 0.14), color = Cream)
    }
}

@Composable
private fun OutlineCreamButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(percent = 50)
    Box(
        modifier
            .height(46.dp)
            .clip(shape)
            .background(Color.Transparent)
            .border(1.5.dp, Ink.copy(alpha = 0.20f), shape)
            .tapNoRipple(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label.uppercase(TR), style = AppText.buttonLabel(12, 0.14), color = OnCream70)
    }
}

private val TR: java.util.Locale = java.util.Locale.forLanguageTag("tr-TR")
