package com.eymistaken.isimsehir.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eymistaken.isimsehir.R
import com.eymistaken.isimsehir.ui.haptics.Haptic
import com.eymistaken.isimsehir.ui.haptics.LocalHaptics
import com.eymistaken.isimsehir.ui.theme.AppText
import com.eymistaken.isimsehir.ui.theme.Cream
import com.eymistaken.isimsehir.ui.theme.Danger
import com.eymistaken.isimsehir.ui.theme.Ink
import com.eymistaken.isimsehir.ui.theme.LocalAccent
import com.eymistaken.isimsehir.ui.theme.OnInk16
import com.eymistaken.isimsehir.ui.theme.OnInk20
import com.eymistaken.isimsehir.ui.theme.OnInk35
import com.eymistaken.isimsehir.ui.theme.OnInk60
import com.eymistaken.isimsehir.ui.theme.OnInk80
import kotlinx.coroutines.launch

/**
 * Tıklamayı ripple'sız yapar — tasarımda hiçbir yerde Material dalgası yok.
 *
 * Uygulamadaki dokunuşların neredeyse tamamı buradan geçtiği için titreşim de
 * burada veriliyor. [haptic] null verilirse titreşim olmaz; dokunmayı yutan
 * ("arkadaki katmana geçmesin") kutular bunu kullanır.
 */
@Composable
fun Modifier.tapNoRipple(
    enabled: Boolean = true,
    haptic: Haptic? = Haptic.Tap,
    onClick: () -> Unit,
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val haptics = LocalHaptics.current
    return this.clickable(
        interactionSource = interaction,
        indication = null,
        enabled = enabled,
    ) {
        haptics.perform(haptic)
        onClick()
    }
}

/** Dar kesim, büyük harfli, harf aralığı açık etiket. */
@Composable
fun Eyebrow(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    size: Int = 11,
    tracking: Double = 0.22,
) {
    Text(
        text = text.uppercase(TR),
        style = AppText.eyebrow(size, tracking),
        color = color,
        modifier = modifier,
    )
}

/** Ayarlar bölümü başlığı: dar kesim etiket + ince alt çizgi. */
@Composable
fun SectionRule(title: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Eyebrow(title, OnInk35, size = 10, tracking = 0.20)
        Box(
            Modifier
                .padding(top = 10.dp)
                .fillMaxWidth()
                .height(1.dp)
                .background(Cream.copy(alpha = 0.10f)),
        )
    }
}

/** Ana eylem: accent dolgulu hap buton. */
@Composable
fun AccentPillButton(
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 56.dp,
    fontSize: Int = 15,
    tracking: Double = 0.14,
    haptic: Haptic? = Haptic.Tap,
    onClick: () -> Unit,
) {
    val accent = LocalAccent.current
    Box(
        modifier
            .height(height)
            .clip(RoundedCornerShape(percent = 50))
            .background(if (enabled) accent else accent.copy(alpha = 0.35f))
            .tapNoRipple(enabled = enabled, haptic = haptic, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label.uppercase(TR),
            style = AppText.buttonLabel(fontSize, tracking),
            color = Ink,
            textAlign = TextAlign.Center,
        )
    }
}

/** İkincil eylem: yalnızca ince çerçeve. */
@Composable
fun OutlinePillButton(
    label: String,
    modifier: Modifier = Modifier,
    height: Dp = 56.dp,
    fontSize: Int = 13,
    tracking: Double = 0.12,
    contentColor: Color = OnInk80,
    borderColor: Color = OnInk20,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .height(height)
            .clip(RoundedCornerShape(percent = 50))
            .border(1.5.dp, borderColor, RoundedCornerShape(percent = 50))
            .tapNoRipple(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label.uppercase(TR),
            style = AppText.buttonLabel(fontSize, tracking),
            color = contentColor,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * "Basılı tut · bitir". Parmak 1 sn basılı kalırsa [onComplete] çalışır;
 * erken bırakılırsa dolgu 200 ms'de geri sarar. Web sürümüyle aynı süreler.
 */
@Composable
fun HoldToFinishButton(
    label: String,
    modifier: Modifier = Modifier,
    holdMillis: Int = 1000,
    onComplete: () -> Unit,
) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val shape = RoundedCornerShape(percent = 50)
    val haptics = LocalHaptics.current

    Box(
        modifier
            .height(56.dp)
            .clip(shape)
            .border(1.5.dp, Danger, shape)
            .pointerInput(holdMillis) {
                detectTapGestures(
                    onPress = {
                        // Parmak değdiği an jestin başladığı hissedilsin.
                        haptics.perform(Haptic.GestureStart)
                        // Dolgu tamamlandığı anda tur biter; parmağın kalkması beklenmez.
                        val fill = scope.launch {
                            progress.animateTo(1f, tween(holdMillis, easing = LinearEasing))
                            progress.snapTo(0f)
                            haptics.perform(Haptic.Confirm)
                            onComplete()
                        }
                        tryAwaitRelease()
                        if (fill.isActive) {
                            fill.cancel()
                            scope.launch {
                                progress.animateTo(0f, tween(200, easing = LinearEasing))
                            }
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.value)
                .background(Danger)
                .align(Alignment.CenterStart),
        )
        Text(
            text = label.uppercase(TR),
            style = AppText.buttonLabel(13, 0.12),
            color = Cream,
        )
    }
}

/** Puan chip'i: 5 / 10. Yazma aşamasında sönük ve tıklanamaz. */
@Composable
fun ScoreChip(
    value: Int,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val accent = LocalAccent.current
    val shape = RoundedCornerShape(12.dp)
    val border = when {
        !enabled -> Cream.copy(alpha = 0.12f)
        selected -> accent
        else -> Cream.copy(alpha = 0.14f)
    }
    val fill = if (selected && enabled) accent else Color.Transparent
    val textColor = when {
        !enabled -> Cream.copy(alpha = 0.22f)
        selected -> Ink
        else -> Cream.copy(alpha = 0.40f)
    }
    Box(
        Modifier
            .size(44.dp)
            .clip(shape)
            .background(fill)
            .border(1.5.dp, border, shape)
            .tapNoRipple(enabled = enabled, haptic = Haptic.Select, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(value.toString(), style = AppText.chip, color = textColor)
    }
}

/** Üst bardaki süre göstergesi; dokununca zamanlayıcı paneli açılır. */
@Composable
fun TimerPill(text: String, showIcon: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(percent = 50))
            .border(1.5.dp, OnInk16, RoundedCornerShape(percent = 50))
            .tapNoRipple(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        if (showIcon) {
            Icon(
                painter = painterResource(R.drawable.ic_clock),
                contentDescription = null,
                tint = OnInk60,
                modifier = Modifier.size(15.dp),
            )
            Text(text, style = AppText.timerPill, color = Cream)
        } else {
            Text(
                text.uppercase(TR),
                style = AppText.eyebrow(11, 0.14),
                color = OnInk60,
            )
        }
    }
}

/** Aç/kapa anahtarı — Ayarlar'daki "Yüzen zamanlayıcı". */
@Composable
fun ToggleSwitch(checked: Boolean, onToggle: () -> Unit) {
    val accent = LocalAccent.current
    var pressed by remember { mutableStateOf(false) }
    LaunchedEffect(checked) { pressed = checked }
    Box(
        Modifier
            .size(width = 52.dp, height = 30.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(if (checked) accent else Cream.copy(alpha = 0.14f))
            .tapNoRipple(
                haptic = if (checked) Haptic.ToggleOff else Haptic.ToggleOn,
                onClick = onToggle,
            ),
    ) {
        Box(
            Modifier
                .padding(start = if (checked) 25.dp else 3.dp, top = 3.dp)
                .size(24.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(if (checked) Ink else Cream.copy(alpha = 0.6f)),
        )
    }
}

private val TR: java.util.Locale = java.util.Locale.forLanguageTag("tr-TR")
