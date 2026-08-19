package com.eymistaken.isimsehir.ui.haptics

import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

/**
 * GEÇİCİ — Ayarlar'daki geliştirici bölümünün motoru.
 *
 * Amaç: cihazın gerçekten ne yapabildiğini elle denemek. Uygulamanın kendi
 * dokunsal sözlüğü ([Haptics]) burada seçilenlere göre ayarlanacak; doğru
 * kombinasyon bulununca bu dosya ve [HapticLabSection] silinebilir.
 *
 * Ayarlar'daki titreşim anahtarına bakmaz — burada her şey her zaman çalar.
 */
class HapticLab(
    private val view: View?,
    private val vibrator: Vibrator?,
) {
    val hasVibrator: Boolean = vibrator != null

    val hasAmplitudeControl: Boolean =
        vibrator?.let { runCatching { it.hasAmplitudeControl() }.getOrDefault(false) } ?: false

    fun supports(effect: LabEffect): Boolean = when {
        Build.VERSION.SDK_INT < effect.minApi -> false
        effect.family != LabFamily.Primitive -> true
        vibrator == null -> false
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
            runCatching { vibrator.areAllPrimitivesSupported(effect.id) }.getOrDefault(false)
        else -> false
    }

    /** Desteklenen primitive sayısı — cihaz özeti satırında gösteriliyor. */
    fun supportedPrimitiveCount(): Int = LAB_PRIMITIVES.count { supports(it) }

    fun play(effect: LabEffect, scale: Float) {
        when (effect.family) {
            LabFamily.Primitive ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) playPrimitive(effect.id, scale)

            LabFamily.Predefined ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) playPredefined(effect.id)

            LabFamily.Constant -> view?.performHapticFeedback(effect.id)
        }
    }

    /** Serbest darbe: süre ve genlik doğrudan verilir. */
    fun playOneShot(durationMs: Int, amplitude: Int) {
        val vibrator = vibrator ?: return
        runCatching {
            val effect = if (hasAmplitudeControl) {
                VibrationEffect.createOneShot(durationMs.toLong(), amplitude.coerceIn(1, 255))
            } else {
                VibrationEffect.createOneShot(durationMs.toLong(), VibrationEffect.DEFAULT_AMPLITUDE)
            }
            emit(vibrator, effect)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun playPrimitive(id: Int, scale: Float) {
        val vibrator = vibrator ?: return
        runCatching {
            emit(
                vibrator,
                VibrationEffect.startComposition()
                    .addPrimitive(id, scale.coerceIn(0.05f, 1f))
                    .compose(),
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun playPredefined(id: Int) {
        val vibrator = vibrator ?: return
        runCatching { emit(vibrator, VibrationEffect.createPredefined(id)) }
    }

    private fun emit(vibrator: Vibrator, effect: VibrationEffect) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrator.vibrate(
                effect,
                VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH),
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(effect, TOUCH_ATTRIBUTES)
        }
    }

    private companion object {
        val TOUCH_ATTRIBUTES: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
    }
}

enum class LabFamily { Primitive, Predefined, Constant }

/**
 * Denenebilir tek bir efekt. [id]'nin anlamı [family]'ye göre değişir:
 * primitive kimliği, hazır efekt kimliği ya da HapticFeedbackConstants değeri.
 */
data class LabEffect(
    val label: String,
    val id: Int,
    val minApi: Int,
    val family: LabFamily,
)

/**
 * Şiddeti ayarlanabilen tek aile bu: donanımın kendi ayarlı darbeleri.
 * Vurmalı (LRA) motorlarda asıl karakter farkı burada.
 */
val LAB_PRIMITIVES: List<LabEffect> = listOf(
    LabEffect("CLICK", VibrationEffect.Composition.PRIMITIVE_CLICK, 30, LabFamily.Primitive),
    LabEffect("TICK", VibrationEffect.Composition.PRIMITIVE_TICK, 30, LabFamily.Primitive),
    LabEffect("LOW TICK", VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 31, LabFamily.Primitive),
    LabEffect("THUD", VibrationEffect.Composition.PRIMITIVE_THUD, 31, LabFamily.Primitive),
    LabEffect("SPIN", VibrationEffect.Composition.PRIMITIVE_SPIN, 31, LabFamily.Primitive),
    LabEffect("QUICK RISE", VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 30, LabFamily.Primitive),
    LabEffect("SLOW RISE", VibrationEffect.Composition.PRIMITIVE_SLOW_RISE, 30, LabFamily.Primitive),
    LabEffect("QUICK FALL", VibrationEffect.Composition.PRIMITIVE_QUICK_FALL, 30, LabFamily.Primitive),
)

/** Hazır efektler: şiddetleri sabit, cihaz kendi belirler. */
val LAB_PREDEFINED: List<LabEffect> = listOf(
    LabEffect("TICK", VibrationEffect.EFFECT_TICK, 29, LabFamily.Predefined),
    LabEffect("CLICK", VibrationEffect.EFFECT_CLICK, 29, LabFamily.Predefined),
    LabEffect("HEAVY CLICK", VibrationEffect.EFFECT_HEAVY_CLICK, 29, LabFamily.Predefined),
    LabEffect("DOUBLE CLICK", VibrationEffect.EFFECT_DOUBLE_CLICK, 29, LabFamily.Predefined),
)

/** Sistemin arayüz sabitleri — üretici kalibrasyonu, ayar yok. */
val LAB_CONSTANTS: List<LabEffect> = listOf(
    LabEffect("CLOCK TICK", HapticFeedbackConstants.CLOCK_TICK, 26, LabFamily.Constant),
    LabEffect("KEYBOARD TAP", HapticFeedbackConstants.KEYBOARD_TAP, 26, LabFamily.Constant),
    LabEffect("VIRTUAL KEY", HapticFeedbackConstants.VIRTUAL_KEY, 26, LabFamily.Constant),
    LabEffect("CONTEXT CLICK", HapticFeedbackConstants.CONTEXT_CLICK, 26, LabFamily.Constant),
    LabEffect("TEXT HANDLE", HapticFeedbackConstants.TEXT_HANDLE_MOVE, 27, LabFamily.Constant),
    LabEffect("LONG PRESS", HapticFeedbackConstants.LONG_PRESS, 26, LabFamily.Constant),
    LabEffect("CONFIRM", HapticFeedbackConstants.CONFIRM, 30, LabFamily.Constant),
    LabEffect("REJECT", HapticFeedbackConstants.REJECT, 30, LabFamily.Constant),
    LabEffect("GESTURE START", HapticFeedbackConstants.GESTURE_START, 30, LabFamily.Constant),
    LabEffect("GESTURE END", HapticFeedbackConstants.GESTURE_END, 30, LabFamily.Constant),
    LabEffect("TOGGLE ON", HapticFeedbackConstants.TOGGLE_ON, 34, LabFamily.Constant),
    LabEffect("TOGGLE OFF", HapticFeedbackConstants.TOGGLE_OFF, 34, LabFamily.Constant),
    LabEffect("SEGMENT TICK", HapticFeedbackConstants.SEGMENT_TICK, 34, LabFamily.Constant),
    LabEffect("SEGMENT FREQ", HapticFeedbackConstants.SEGMENT_FREQUENT_TICK, 34, LabFamily.Constant),
    LabEffect("DRAG START", HapticFeedbackConstants.DRAG_START, 34, LabFamily.Constant),
)

@Composable
fun rememberHapticLab(): HapticLab {
    val view = LocalView.current
    val context = LocalContext.current
    return remember(view, context) { HapticLab(view, vibratorOf(context)) }
}
