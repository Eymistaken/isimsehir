package com.eymistaken.isimsehir.ui.timer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eymistaken.isimsehir.model.DURATION_PRESETS
import com.eymistaken.isimsehir.model.TimerState
import com.eymistaken.isimsehir.ui.components.tapNoRipple
import com.eymistaken.isimsehir.ui.theme.AppText
import com.eymistaken.isimsehir.ui.theme.Cream
import com.eymistaken.isimsehir.ui.theme.Ink
import com.eymistaken.isimsehir.ui.theme.LocalAccent
import com.eymistaken.isimsehir.ui.theme.OnCream45
import com.eymistaken.isimsehir.ui.theme.OnCream50
import com.eymistaken.isimsehir.ui.theme.OnCream70

/** Yüzen hapın ölçüleri; [TimerLayer] konum sınırlarını buradan hesaplar. */
val PILL_WIDTH = 108.dp
val PILL_HEIGHT = 44.dp

/**
 * Sürüklenebilir accent hap. Konumu [TimerLayer] tutar; buraya yalnızca
 * hareket miktarı bildirilir.
 *
 * Sürükleme algılayıcısı bilerek görsel katmanlardan sonra, dokunmadan önce
 * duruyor: ters sırada tıklama basma olayını önce yakalayıp sürüklemeyi
 * öldürüyor.
 */
@Composable
fun TimerPill(
    timer: TimerState,
    onOpen: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    dragKey: Any,
    modifier: Modifier = Modifier,
) {
    val accent = LocalAccent.current
    Row(
        modifier
            .width(PILL_WIDTH)
            .height(PILL_HEIGHT)
            .clip(RoundedCornerShape(percent = 50))
            .background(accent)
            .pointerInput(dragKey) {
                detectDragGestures { change, drag ->
                    change.consume()
                    onDrag(drag.x, drag.y)
                }
            }
            .tapNoRipple(onClick = onOpen),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(timer.display, style = AppText.timerPanelPill, color = Ink)
    }
}

/**
 * Zamanlayıcı kartı. Üç hâli var:
 *  - boşta: hazır süreler görünür,
 *  - işliyor/duraklamış: durdur/sürdür ve iptal,
 *  - düzenleme: dev süreye dokununca dakika:saniye alanları açılır.
 *
 * Kart kendi karartmasını çizmez; onu [TimerLayer] yönetir ki açılış
 * animasyonunda karartma ve kart ayrı ayrı hareket edebilsin.
 */
@Composable
fun TimerPanelCard(
    timer: TimerState,
    onStart: (Int) -> Unit,
    onTogglePause: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = LocalAccent.current
    var editing by remember { mutableStateOf(false) }
    var minutes by remember { mutableStateOf("") }
    var seconds by remember { mutableStateOf("") }

    fun beginEditing() {
        minutes = (timer.totalSeconds / 60).toString()
        seconds = (timer.totalSeconds % 60).toString().padStart(2, '0')
        editing = true
    }

    fun commitEditing() {
        val total = (minutes.toIntOrNull() ?: 0) * 60 + (seconds.toIntOrNull() ?: 0).coerceAtMost(59)
        editing = false
        if (total > 0) onStart(total)
    }

    Column(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Cream)
            .tapNoRipple {}
            .padding(22.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (editing) "SÜREYİ AYARLA" else "KALAN SÜRE",
                style = AppText.eyebrow(10, 0.20),
                color = OnCream50,
            )
            if (!editing) {
                // Krem üzerinde accent turuncusu 10sp için fazla düşük kontrastlı
                // kalıyor; etiket mürekkeple yazılıp çerçeveyle tıklanabilir yapıldı.
                Text(
                    "DÜZENLE",
                    style = AppText.eyebrow(10, 0.20),
                    color = OnCream70,
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .border(1.5.dp, Ink.copy(alpha = 0.15f), RoundedCornerShape(percent = 50))
                        .tapNoRipple { beginEditing() }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
        }

        if (editing) {
            Row(
                Modifier.padding(top = 6.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TimeField(
                    value = minutes,
                    onValueChange = { minutes = it.filter(Char::isDigit).take(2) },
                    imeAction = ImeAction.Next,
                    onDone = {},
                )
                Text(":", style = AppText.timerHuge, color = Ink.copy(alpha = 0.35f))
                TimeField(
                    value = seconds,
                    onValueChange = { seconds = it.filter(Char::isDigit).take(2) },
                    imeAction = ImeAction.Done,
                    onDone = { commitEditing() },
                )
            }
        } else {
            Text(
                timer.display,
                style = AppText.timerHuge,
                color = Ink,
                modifier = Modifier
                    .padding(top = 6.dp, bottom = 4.dp)
                    .tapNoRipple { beginEditing() },
            )
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(Ink.copy(alpha = 0.12f)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(if (editing) 1f else timer.progress.coerceIn(0f, 1f))
                    .height(4.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(if (editing) accent.copy(alpha = 0.4f) else accent),
            )
        }

        Spacer(Modifier.height(18.dp))

        when {
            editing -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlineCreamButton("Vazgeç", Modifier.weight(1f)) { editing = false }
                    SolidCreamButton("Başlat", Modifier.weight(1f)) { commitEditing() }
                }
                Spacer(Modifier.height(16.dp))
            }

            timer.running || (timer.remainingSeconds < timer.totalSeconds && !timer.finished) -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SolidCreamButton(
                        label = if (timer.running) "Durdur" else "Sürdür",
                        modifier = Modifier.weight(1f),
                        onClick = onTogglePause,
                    )
                    OutlineCreamButton("İptal", Modifier.weight(1f), onCancel)
                }
                Spacer(Modifier.height(16.dp))
            }
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
                        .tapNoRipple {
                            editing = false
                            onStart(preset.seconds)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(preset.label, style = AppText.buttonLabel(12, 0.0), color = OnCream70)
                }
            }
        }
    }
}

/** Dev süre yazısının düzenlenebilir hâli — dakika ya da saniye. */
@Composable
private fun TimeField(
    value: String,
    onValueChange: (String) -> Unit,
    imeAction: ImeAction,
    onDone: () -> Unit,
) {
    val accent = LocalAccent.current
    Column(Modifier.width(88.dp)) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = AppText.timerHuge.copy(color = Ink, textAlign = TextAlign.Center),
            cursorBrush = SolidColor(accent),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = imeAction,
            ),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            modifier = Modifier.fillMaxWidth(),
        )
        Box(
            Modifier
                .padding(top = 4.dp)
                .fillMaxWidth()
                .height(2.dp)
                .background(Ink.copy(alpha = 0.15f)),
        )
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
