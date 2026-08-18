package com.eymistaken.isimsehir.ui.game

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eymistaken.isimsehir.model.ActiveRound
import com.eymistaken.isimsehir.model.RoundPhase
import com.eymistaken.isimsehir.model.WordEntry
import com.eymistaken.isimsehir.ui.components.AccentPillButton
import com.eymistaken.isimsehir.ui.components.Eyebrow
import com.eymistaken.isimsehir.ui.components.HoldToFinishButton
import com.eymistaken.isimsehir.ui.components.OutlinePillButton
import com.eymistaken.isimsehir.ui.components.ScoreChip
import com.eymistaken.isimsehir.ui.components.TimerPill
import com.eymistaken.isimsehir.ui.theme.AppText
import com.eymistaken.isimsehir.ui.theme.Cream
import com.eymistaken.isimsehir.ui.theme.Ink
import com.eymistaken.isimsehir.ui.theme.LocalAccent
import com.eymistaken.isimsehir.ui.theme.OnCream50
import com.eymistaken.isimsehir.ui.theme.OnInk16
import com.eymistaken.isimsehir.ui.theme.OnInk30
import com.eymistaken.isimsehir.ui.theme.OnInk40
import com.eymistaken.isimsehir.ui.theme.OnInk45

/**
 * Tur ekranı. Yazma ve puanlama tek bir düzenin iki hâli — tasarımdaki
 * 1. ve 2. ekran. Kart, satırlar ve dolgu aynı kalıyor; değişen yalnızca
 * alanların etkinliği ve alttaki eylemler.
 */
@Composable
fun GameScreen(
    round: ActiveRound,
    timerText: String,
    timerFinished: Boolean,
    showTimerPill: Boolean,
    onTimerClick: () -> Unit,
    onWordChange: (Int, String) -> Unit,
    onScoreToggle: (Int, Int) -> Unit,
    onEnterScoring: () -> Unit,
    onExitScoring: () -> Unit,
    onFinish: () -> Unit,
) {
    val scoring = round.phase == RoundPhase.Scoring

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Eyebrow(if (scoring) "Puanlama" else "İsim Şehir", OnInk45)
            if (showTimerPill) {
                if (scoring && timerFinished) {
                    TimerPill("Süre bitti", showIcon = false, onClick = onTimerClick)
                } else {
                    TimerPill(timerText, showIcon = true, onClick = onTimerClick)
                }
            }
        }

        LetterCard(
            letter = round.letter,
            total = round.total,
            roundIndex = round.index,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp),
        )

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            round.entries.forEachIndexed { index, entry ->
                CategoryRow(
                    entry = entry,
                    letter = round.letter,
                    scoring = scoring,
                    onWordChange = { onWordChange(index, it) },
                    onScoreToggle = { onScoreToggle(index, it) },
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        Box(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 16.dp)) {
            if (!scoring) {
                AccentPillButton(
                    label = "Puanlamaya geç",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onEnterScoring,
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinePillButton(
                        label = "Geri dön",
                        modifier = Modifier.width(118.dp),
                        onClick = onExitScoring,
                    )
                    HoldToFinishButton(
                        label = "Basılı tut · bitir",
                        modifier = Modifier.weight(1f),
                        onComplete = onFinish,
                    )
                }
            }
        }
    }
}

/** Krem kart: turun harfi solda dev, toplam sağda. */
@Composable
private fun LetterCard(letter: Char, total: Int, roundIndex: Int, modifier: Modifier = Modifier) {
    val accent = LocalAccent.current
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Cream)
            .padding(start = 22.dp, end = 22.dp, top = 16.dp, bottom = 18.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Eyebrow("Bu turun harfi", OnCream50, size = 10, tracking = 0.20)
            Text(
                letter.toString(),
                style = AppText.letterHuge,
                color = Ink,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.padding(bottom = 6.dp),
        ) {
            Eyebrow("Toplam", OnCream50, size = 10, tracking = 0.20)
            Text(total.toString(), style = AppText.scoreBig, color = accent)
            Text(
                "$roundIndex. tur",
                style = AppText.captionSmall,
                color = OnCream50,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

/** Tek kategori satırı: etiket, kelime alanı ve iki puan chip'i. */
@Composable
private fun CategoryRow(
    entry: WordEntry,
    letter: Char,
    scoring: Boolean,
    onWordChange: (String) -> Unit,
    onScoreToggle: (Int) -> Unit,
) {
    val accent = LocalAccent.current
    Column {
        Eyebrow(
            text = entry.category,
            color = if (scoring) OnInk40 else accent,
            size = 11,
            tracking = 0.16,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(Modifier.weight(1f)) {
                if (scoring) {
                    Text(
                        text = entry.word.ifBlank { "boş" },
                        style = AppText.word,
                        color = if (entry.word.isBlank()) OnInk30 else Cream,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp, vertical = 7.dp),
                    )
                } else {
                    WordField(
                        value = entry.word,
                        letter = letter,
                        onValueChange = onWordChange,
                    )
                }
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(1.5.dp)
                        .background(if (scoring) Cream.copy(alpha = 0.10f) else OnInk16),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ScoreChip(5, entry.score == 5, scoring) { onScoreToggle(5) }
                ScoreChip(10, entry.score == 10, scoring) { onScoreToggle(10) }
            }
        }
    }
}

@Composable
private fun WordField(value: String, letter: Char, onValueChange: (String) -> Unit) {
    val accent = LocalAccent.current
    Box {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = AppText.word.copy(color = Cream),
            cursorBrush = SolidColor(accent),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 7.dp),
        )
        if (value.isEmpty()) {
            Text(
                text = "$letter ile başlayan…",
                style = AppText.word,
                color = OnInk30,
                modifier = Modifier.padding(horizontal = 2.dp, vertical = 7.dp),
            )
        }
    }
}

/** Aktif tur yokken görünen boş durum. */
@Composable
fun EmptyGameState(onNewRound: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Eyebrow("Oyun alanı", OnInk45)
        Text(
            "Yeni bir tur başlat",
            style = AppText.heading,
            color = Cream,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            "Harfi çark seçsin ya da listeden kendin seç.",
            style = AppText.caption,
            color = OnInk45,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
        AccentPillButton(
            label = "Yeni tur",
            modifier = Modifier
                .padding(top = 28.dp)
                .fillMaxWidth(),
            onClick = onNewRound,
        )
    }
}
