package com.eymistaken.isimsehir.ui.letters

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eymistaken.isimsehir.model.ALPHABET
import com.eymistaken.isimsehir.ui.components.tapNoRipple
import com.eymistaken.isimsehir.ui.haptics.Haptic
import com.eymistaken.isimsehir.ui.theme.AppText
import com.eymistaken.isimsehir.ui.theme.Cream
import com.eymistaken.isimsehir.ui.theme.Ink
import com.eymistaken.isimsehir.ui.theme.InkDeep
import com.eymistaken.isimsehir.ui.theme.OnCream50
import com.eymistaken.isimsehir.ui.theme.OnCream60
import com.eymistaken.isimsehir.ui.theme.OnCream16

private const val COLUMNS = 5

/**
 * Harf seçimi. Tasarımdaki 4. ekran: karartılmış arka planın üstüne oturan
 * krem sayfa. Daha önce oynanmış harfler sönük ama yine seçilebilir —
 * seçilirse üst katman onay ister.
 */
@Composable
fun LetterPickerSheet(
    usedLetters: List<Char>,
    onPick: (Char) -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(InkDeep.copy(alpha = 0.55f))
            .tapNoRipple(onClick = onDismiss),
    ) {
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .background(Cream)
                // Sayfaya dokunmak arkadaki kapatmayı tetiklemesin.
                .tapNoRipple(haptic = null) {}
                .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 26.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Harf seç", style = AppText.sheetTitle, color = Ink)
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .border(1.5.dp, OnCream16, RoundedCornerShape(percent = 50))
                        .tapNoRipple(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("×", style = AppText.body, color = OnCream60)
                }
            }

            Text(
                "Soluk harfler daha önce oynandı",
                style = AppText.caption,
                color = OnCream50,
                modifier = Modifier.padding(top = 4.dp, bottom = 18.dp),
            )

            // 27 harf — sabit 5 sütun; LazyGrid'e gerek yok, hepsi tek ekranda.
            ALPHABET.chunked(COLUMNS).forEach { row ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    row.forEach { letter ->
                        LetterCell(
                            letter = letter,
                            used = letter in usedLetters,
                            modifier = Modifier.weight(1f),
                            onClick = { onPick(letter) },
                        )
                    }
                    // Son satırı hizada tutmak için boş yerler.
                    repeat(COLUMNS - row.size) {
                        Box(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun LetterCell(
    letter: Char,
    used: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier
            .height(56.dp)
            .clip(shape)
            .background(if (used) Ink.copy(alpha = 0.06f) else Color.Transparent)
            .border(1.5.dp, if (used) Color.Transparent else OnCream16, shape)
            .tapNoRipple(haptic = Haptic.Select, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = letter.toString(),
            style = AppText.word.copy(fontWeight = FontWeight.Bold),
            color = if (used) Ink.copy(alpha = 0.30f) else Ink,
        )
    }
}
