package com.eymistaken.isimsehir.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Dp

/**
 * Satır sarmalı basit yerleşim: kategori chip'leri ve kullanılan harfler için.
 * Compose Foundation'ın deneysel `FlowRow`una bağlanmamak için elle yazıldı.
 */
@Composable
fun FlowRowSimple(
    horizontalGap: Dp,
    verticalGap: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val hGap = horizontalGap.roundToPx()
        val vGap = verticalGap.roundToPx()
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0)) }

        var x = 0
        var y = 0
        var rowHeight = 0
        val xs = IntArray(placeables.size)
        val ys = IntArray(placeables.size)

        placeables.forEachIndexed { i, p ->
            if (x > 0 && x + p.width > constraints.maxWidth) {
                x = 0
                y += rowHeight + vGap
                rowHeight = 0
            }
            xs[i] = x
            ys[i] = y
            x += p.width + hGap
            rowHeight = maxOf(rowHeight, p.height)
        }

        layout(constraints.maxWidth, y + rowHeight) {
            placeables.forEachIndexed { i, p -> p.place(xs[i], ys[i]) }
        }
    }
}
