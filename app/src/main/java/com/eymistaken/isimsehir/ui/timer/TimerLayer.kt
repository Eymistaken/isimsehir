package com.eymistaken.isimsehir.ui.timer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.eymistaken.isimsehir.model.TimerState
import com.eymistaken.isimsehir.ui.components.tapNoRipple
import com.eymistaken.isimsehir.ui.theme.InkDeep
import kotlin.math.roundToInt

private const val OPEN_MILLIS = 260
private const val CLOSE_MILLIS = 200

/**
 * Yüzen hap ile büyük panelin ortak katmanı. İkisini bir arada tutmasının
 * sebebi geçiş: panel, hapın ekranda durduğu noktadan büyüyerek açılıyor —
 * bunun için hapın konumunu ikisinin de bilmesi gerekiyor.
 */
@Composable
fun TimerLayer(
    timer: TimerState,
    floatingEnabled: Boolean,
    panelOpen: Boolean,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    onStart: (Int) -> Unit,
    onTogglePause: () -> Unit,
    onCancel: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val boundsWidth = with(density) { maxWidth.toPx() }
        val boundsHeight = with(density) { maxHeight.toPx() }
        val pillWidth = with(density) { PILL_WIDTH.toPx() }
        val pillHeight = with(density) { PILL_HEIGHT.toPx() }
        val margin = with(density) { 20.dp.toPx() }

        // Başlangıç: sağ üst köşe. Sürükleme bunu güncelliyor.
        var offsetX by remember(boundsWidth) {
            mutableFloatStateOf(boundsWidth - pillWidth - margin)
        }
        var offsetY by remember(boundsHeight) { mutableFloatStateOf(margin) }

        // Panelin hangi noktadan büyüyeceği. Yüzen hap kapalıysa süre üst
        // bardaki göstergeden açılıyor, o da sağ üstte.
        val origin = if (!floatingEnabled) {
            TransformOrigin(0.9f, 0f)
        } else {
            TransformOrigin(
                pivotFractionX = ((offsetX + pillWidth / 2f) / boundsWidth).coerceIn(0f, 1f),
                pivotFractionY = if (offsetY + pillHeight / 2f < boundsHeight / 2f) 0f else 1f,
            )
        }

        AnimatedVisibility(
            visible = floatingEnabled && !panelOpen,
            modifier = Modifier.offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) },
            enter = fadeIn(tween(180)) + scaleIn(tween(220, easing = FastOutSlowInEasing), 0.7f),
            exit = fadeOut(tween(120)) + scaleOut(tween(160), 0.7f),
        ) {
            TimerPill(
                timer = timer,
                onOpen = onOpen,
                onDrag = { dx, dy ->
                    offsetX = (offsetX + dx).coerceIn(0f, boundsWidth - pillWidth)
                    offsetY = (offsetY + dy).coerceIn(0f, boundsHeight - pillHeight)
                },
                dragKey = boundsWidth to boundsHeight,
            )
        }

        // Karartma yalnızca soluyor; kart onun üstünde ayrıca ölçekleniyor.
        AnimatedVisibility(
            visible = panelOpen,
            enter = fadeIn(tween(OPEN_MILLIS)),
            exit = fadeOut(tween(CLOSE_MILLIS)),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(InkDeep.copy(alpha = 0.5f))
                    .tapNoRipple(onClick = onDismiss),
            )
        }

        AnimatedVisibility(
            visible = panelOpen,
            enter = fadeIn(tween(140)) +
                scaleIn(tween(OPEN_MILLIS, easing = FastOutSlowInEasing), 0.6f, origin),
            exit = fadeOut(tween(CLOSE_MILLIS)) +
                scaleOut(tween(CLOSE_MILLIS, easing = FastOutSlowInEasing), 0.6f, origin),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .imePadding(),
                contentAlignment = Alignment.Center,
            ) {
                TimerPanelCard(
                    timer = timer,
                    onStart = onStart,
                    onTogglePause = onTogglePause,
                    onCancel = onCancel,
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth(),
                )
            }
        }
    }
}
