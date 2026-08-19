package com.eymistaken.isimsehir.ui.haptics

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.eymistaken.isimsehir.model.HapticChoice
import com.eymistaken.isimsehir.model.HapticFamily

/**
 * GEÇİCİ — Ayarlar'daki geliştirici bölümünün motoru.
 *
 * Amaç: cihazın gerçekten ne yapabildiğini elle denemek. Burada seçilen darbe
 * oyunun tamamına uygulanıyor; doğru olan bulununca [Haptics] varsayılanı ona
 * çekilip bu dosya ile HapticLabSection silinebilir.
 *
 * Çalma işini kendisi yapmaz — [Haptics.playChoice] yapar, böylece laboratuvarda
 * duyduğun ile oyunda duyduğun aynı koddan çıkar.
 */
class HapticLab(private val vibrator: Vibrator?) {
    val hasVibrator: Boolean = vibrator != null

    val hasAmplitudeControl: Boolean =
        vibrator?.let { runCatching { it.hasAmplitudeControl() }.getOrDefault(false) } ?: false

    fun supports(effect: LabEffect): Boolean {
        if (Build.VERSION.SDK_INT < effect.minApi) return false
        if (effect.family != HapticFamily.Primitive) return true
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
        val vibrator = vibrator ?: return false
        return runCatching { vibrator.areAllPrimitivesSupported(effect.id) }.getOrDefault(false)
    }

    /** Desteklenen primitive sayısı — cihaz özeti satırında gösteriliyor. */
    fun supportedPrimitiveCount(): Int = LAB_PRIMITIVES.count { supports(it) }

}

/**
 * Denenebilir tek bir efekt. [id]'nin anlamı [family]'ye göre değişir:
 * primitive kimliği, hazır efekt kimliği ya da HapticFeedbackConstants değeri.
 */
data class LabEffect(
    val label: String,
    val id: Int,
    val minApi: Int,
    val family: HapticFamily,
) {
    /** Seçilince oyuna uygulanacak hâli. */
    fun toChoice(scale: Float) = HapticChoice(
        family = family,
        id = id,
        scale = scale,
        label = label,
    )
}

/**
 * Şiddeti ayarlanabilen tek aile bu: donanımın kendi ayarlı darbeleri.
 * Vurmalı (LRA) motorlarda asıl karakter farkı burada.
 */
val LAB_PRIMITIVES: List<LabEffect> = listOf(
    LabEffect("CLICK", VibrationEffect.Composition.PRIMITIVE_CLICK, 30, HapticFamily.Primitive),
    LabEffect("TICK", VibrationEffect.Composition.PRIMITIVE_TICK, 30, HapticFamily.Primitive),
    LabEffect("LOW TICK", VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 31, HapticFamily.Primitive),
    LabEffect("THUD", VibrationEffect.Composition.PRIMITIVE_THUD, 31, HapticFamily.Primitive),
    LabEffect("SPIN", VibrationEffect.Composition.PRIMITIVE_SPIN, 31, HapticFamily.Primitive),
    LabEffect("QUICK RISE", VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 30, HapticFamily.Primitive),
    LabEffect("SLOW RISE", VibrationEffect.Composition.PRIMITIVE_SLOW_RISE, 30, HapticFamily.Primitive),
    LabEffect("QUICK FALL", VibrationEffect.Composition.PRIMITIVE_QUICK_FALL, 30, HapticFamily.Primitive),
)

/** Hazır efektler: şiddetleri sabit, cihaz kendi belirler. */
val LAB_PREDEFINED: List<LabEffect> = listOf(
    LabEffect("TICK", VibrationEffect.EFFECT_TICK, 29, HapticFamily.Predefined),
    LabEffect("CLICK", VibrationEffect.EFFECT_CLICK, 29, HapticFamily.Predefined),
    LabEffect("HEAVY CLICK", VibrationEffect.EFFECT_HEAVY_CLICK, 29, HapticFamily.Predefined),
    LabEffect("DOUBLE CLICK", VibrationEffect.EFFECT_DOUBLE_CLICK, 29, HapticFamily.Predefined),
)

/** Sistemin arayüz sabitleri — üretici kalibrasyonu, ayar yok. */
val LAB_CONSTANTS: List<LabEffect> = listOf(
    LabEffect("CLOCK TICK", HapticFeedbackConstants.CLOCK_TICK, 26, HapticFamily.Constant),
    LabEffect("KEYBOARD TAP", HapticFeedbackConstants.KEYBOARD_TAP, 26, HapticFamily.Constant),
    LabEffect("VIRTUAL KEY", HapticFeedbackConstants.VIRTUAL_KEY, 26, HapticFamily.Constant),
    LabEffect("CONTEXT CLICK", HapticFeedbackConstants.CONTEXT_CLICK, 26, HapticFamily.Constant),
    LabEffect("TEXT HANDLE", HapticFeedbackConstants.TEXT_HANDLE_MOVE, 27, HapticFamily.Constant),
    LabEffect("LONG PRESS", HapticFeedbackConstants.LONG_PRESS, 26, HapticFamily.Constant),
    LabEffect("CONFIRM", HapticFeedbackConstants.CONFIRM, 30, HapticFamily.Constant),
    LabEffect("REJECT", HapticFeedbackConstants.REJECT, 30, HapticFamily.Constant),
    LabEffect("GESTURE START", HapticFeedbackConstants.GESTURE_START, 30, HapticFamily.Constant),
    LabEffect("GESTURE END", HapticFeedbackConstants.GESTURE_END, 30, HapticFamily.Constant),
    LabEffect("TOGGLE ON", HapticFeedbackConstants.TOGGLE_ON, 34, HapticFamily.Constant),
    LabEffect("TOGGLE OFF", HapticFeedbackConstants.TOGGLE_OFF, 34, HapticFamily.Constant),
    LabEffect("SEGMENT TICK", HapticFeedbackConstants.SEGMENT_TICK, 34, HapticFamily.Constant),
    LabEffect("SEGMENT FREQ", HapticFeedbackConstants.SEGMENT_FREQUENT_TICK, 34, HapticFamily.Constant),
    LabEffect("DRAG START", HapticFeedbackConstants.DRAG_START, 34, HapticFamily.Constant),
)

@Composable
fun rememberHapticLab(): HapticLab {
    val context = LocalContext.current
    return remember(context) { HapticLab(vibratorOf(context)) }
}
