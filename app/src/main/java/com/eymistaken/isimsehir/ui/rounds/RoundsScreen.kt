package com.eymistaken.isimsehir.ui.rounds

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eymistaken.isimsehir.model.CompletedRound
import com.eymistaken.isimsehir.ui.components.Eyebrow
import com.eymistaken.isimsehir.ui.haptics.Haptic
import com.eymistaken.isimsehir.ui.haptics.LocalHaptics
import com.eymistaken.isimsehir.ui.theme.AppText
import com.eymistaken.isimsehir.ui.theme.Cream
import com.eymistaken.isimsehir.ui.theme.Ink
import com.eymistaken.isimsehir.ui.theme.LocalAccent
import com.eymistaken.isimsehir.ui.theme.OnCream50
import com.eymistaken.isimsehir.ui.theme.OnCream55
import com.eymistaken.isimsehir.ui.theme.OnInk35
import com.eymistaken.isimsehir.ui.theme.OnInk45
import com.eymistaken.isimsehir.ui.theme.Panel

/**
 * Tamamlanmış turlar. Tasarımdaki 5. ekran: en yeni tur üstte, altta
 * genel toplamı taşıyan krem panel. Bir kartı basılı tutmak silme onayı açar.
 */
@Composable
fun RoundsScreen(
    rounds: List<CompletedRound>,
    grandTotal: Int,
    onDeleteRound: (Long) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp)) {
            Eyebrow("Bu oturum", OnInk45)
            Text(
                "Turlar",
                style = AppText.screenTitle,
                color = Cream,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        if (rounds.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    "Henüz tamamlanmış bir tur yok.",
                    style = AppText.caption,
                    color = OnInk45,
                    textAlign = TextAlign.Center,
                )
            }
            return@Column
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            rounds.forEach { round ->
                RoundCard(round, onLongPress = { onDeleteRound(round.id) })
            }
            Spacer(Modifier.height(8.dp))
        }

        GrandTotalPanel(
            roundCount = rounds.size,
            total = grandTotal,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 16.dp),
        )
    }
}

@Composable
private fun RoundCard(round: CompletedRound, onLongPress: () -> Unit) {
    val accent = LocalAccent.current
    val haptics = LocalHaptics.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Panel)
            .pointerInput(round.id, haptics) {
                detectTapGestures(
                    onLongPress = {
                        haptics.perform(Haptic.LongPress)
                        onLongPress()
                    },
                )
            }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                round.letter.toString(),
                style = AppText.roundLetter,
                color = Ink,
            )
        }
        Column(Modifier.weight(1f)) {
            Text("${round.index}. tur", style = AppText.bodyBold, color = Cream)
            Text(
                "${round.categoryCount} kategori · ${round.correctCount} doğru",
                style = AppText.caption,
                color = OnInk45,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(round.total.toString(), style = AppText.scoreRow, color = Cream)
            Eyebrow("puan", OnInk35, size = 10, tracking = 0.14)
        }
    }
}

@Composable
private fun GrandTotalPanel(roundCount: Int, total: Int, modifier: Modifier = Modifier) {
    val accent = LocalAccent.current
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Cream)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Eyebrow("Genel toplam", OnCream50, size = 10, tracking = 0.20)
            Text(
                "$roundCount tur oynandı",
                style = AppText.captionMd,
                color = OnCream55,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Text(total.toString(), style = AppText.scoreGrand, color = accent)
    }
}
