package com.eymistaken.isimsehir.ui.wheel

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eymistaken.isimsehir.model.ALPHABET
import com.eymistaken.isimsehir.ui.components.Eyebrow
import com.eymistaken.isimsehir.ui.components.FlowRowSimple
import com.eymistaken.isimsehir.ui.components.OutlinePillButton
import com.eymistaken.isimsehir.ui.components.tapNoRipple
import com.eymistaken.isimsehir.ui.theme.AppText
import com.eymistaken.isimsehir.ui.theme.Cream
import com.eymistaken.isimsehir.ui.theme.Ink
import com.eymistaken.isimsehir.ui.theme.LocalAccent
import com.eymistaken.isimsehir.ui.theme.OnCream45
import com.eymistaken.isimsehir.ui.theme.OnInk06
import com.eymistaken.isimsehir.ui.theme.OnInk14
import com.eymistaken.isimsehir.ui.theme.OnInk22
import com.eymistaken.isimsehir.ui.theme.OnInk35
import com.eymistaken.isimsehir.ui.theme.OnInk45
import com.eymistaken.isimsehir.ui.theme.Panel
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private const val WHEEL_DIAMETER = 310
private const val SPIN_TURNS = 5
private const val SPIN_MILLIS = 4000

/**
 * Çark ekranı. Döndürme, web sürümündeki mantığın aynısı: hedef harf önce
 * seçilir ([spinTarget]), çark 5 tam tur + hedef açı döner, animasyon bitince
 * [onSpinFinished] turu açar.
 */
@Composable
fun WheelScreen(
    usedLetters: List<Char>,
    spinTarget: Char?,
    onSpin: () -> Unit,
    onSpinFinished: () -> Unit,
    onPickManually: () -> Unit,
) {
    val accent = LocalAccent.current
    val rotation = remember { Animatable(0f) }
    val remaining = ALPHABET.count { it !in usedLetters }

    LaunchedEffect(spinTarget) {
        val target = spinTarget ?: return@LaunchedEffect
        val step = 360f / ALPHABET.size
        val index = ALPHABET.indexOf(target)
        // Harf `index`, çark 0°'dayken saat 12'den `index * step` kadar ileride.
        // İşaretçinin altına getirmek için o kadar geri döndürüyoruz.
        val landing = SPIN_TURNS * 360f + (360f - index * step) +
            (Random.nextFloat() - 0.5f) * step
        rotation.snapTo(rotation.value % 360f)
        rotation.animateTo(
            targetValue = rotation.value + landing,
            animationSpec = tween(SPIN_MILLIS, easing = FastOutSlowInEasing),
        )
        onSpinFinished()
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Eyebrow("Yeni tur", OnInk45)
            Text(
                "Harfi çark seçsin",
                style = AppText.heading,
                color = Cream,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        Spacer(Modifier.height(34.dp))

        // İşaretçi çarkın tepesinde sabit durur, çark onun altında döner.
        Canvas(
            Modifier
                .width(22.dp)
                .height(16.dp),
        ) {
            drawPath(
                Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width / 2f, size.height)
                    close()
                },
                accent,
            )
        }

        Box(
            Modifier
                .padding(top = 0.dp)
                .size(WHEEL_DIAMETER.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(Panel)
                .border(1.5.dp, OnInk14, RoundedCornerShape(percent = 50)),
            contentAlignment = Alignment.Center,
        ) {
            LetterRing(usedLetters, rotation.value)

            Column(
                Modifier
                    .size(132.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Cream)
                    .tapNoRipple(enabled = spinTarget == null, onClick = onSpin),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("ÇEVİR", style = AppText.eyebrow(15, 0.16), color = Ink)
                Text(
                    "$remaining HARF KALDI",
                    style = AppText.eyebrow(10, 0.10, FontWeight.SemiBold),
                    color = OnCream45,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }

        if (usedLetters.isNotEmpty()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 34.dp),
            ) {
                Eyebrow("Kullanılan harfler", OnInk35, size = 10, tracking = 0.20)
                Spacer(Modifier.height(10.dp))
                FlowRowSimple(horizontalGap = 6.dp, verticalGap = 6.dp) {
                    usedLetters.forEach { letter ->
                        Box(
                            Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(OnInk06),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(letter.toString(), style = AppText.chip, color = OnInk35)
                        }
                    }
                }
            }
        }

        OutlinePillButton(
            label = "Harfi kendim seçeyim",
            height = 52.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 34.dp, bottom = 20.dp),
            onClick = onPickManually,
        )
    }
}

/** Harflerin çark yarıçapına dizilmesi; daha önce oynanmışlar sönük. */
@Composable
private fun LetterRing(usedLetters: List<Char>, rotationDegrees: Float) {
    val density = LocalDensity.current
    val radiusPx = with(density) { (WHEEL_DIAMETER / 2f - 24f).dp.toPx() }
    val step = 360f / ALPHABET.size

    Box(
        Modifier
            .size(WHEEL_DIAMETER.dp)
            .rotate(rotationDegrees),
        contentAlignment = Alignment.Center,
    ) {
        ALPHABET.forEachIndexed { index, letter ->
            val angle = Math.toRadians((step * index - 90f).toDouble())
            val dx = (radiusPx * cos(angle)).toFloat().roundToInt()
            val dy = (radiusPx * sin(angle)).toFloat().roundToInt()
            Box(
                Modifier
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        layout(placeable.width, placeable.height) {
                            placeable.place(dx, dy)
                        }
                    }
                    .rotate(step * index),
            ) {
                Text(
                    letter.toString(),
                    style = AppText.eyebrow(16, 0.0),
                    color = if (letter in usedLetters) OnInk22 else Cream.copy(alpha = 0.85f),
                )
            }
        }
    }
}
