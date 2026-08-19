package com.eymistaken.isimsehir.ui.haptics

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import com.eymistaken.isimsehir.model.TimerEndVibration

/**
 * Uygulamanın dokunsal sözlüğü. İsimler cihazın titreşim motorunu değil,
 * etkileşimin anlamını anlatıyor: seçim mi, onay mı, ret mi.
 */
enum class Haptic {
    /** Sıradan buton dokunuşu. */
    Tap,

    /** Bir seçeneği seçmek: puan chip'i, harf, sekme, renk. */
    Select,

    /** Çark tıkırtısı, geri sayımın son saniyeleri. */
    Tick,

    /** Aç/kapa anahtarı. */
    ToggleOn,
    ToggleOff,

    /** Bir şeyi kesinleştirmek: tur başladı, tur bitti, onaylandı. */
    Confirm,

    /** Kabul edilmeyen giriş. */
    Reject,

    /** Uzun basış eşiğine ulaşıldı. */
    LongPress,

    /** Basılı tutma jesti başladı. */
    GestureStart,
}

/**
 * İki kanal:
 *  - Arayüz dokunuşları [View.performHapticFeedback] ile çalar. İzin
 *    gerektirmez ve cihazın sistem "dokunsal geri bildirim" ayarına
 *    kendiliğinden uyar; [enabled] uygulama içindeki anahtar.
 *  - Süre bitişi [Vibrator] ile çalar; cepteyken hissedilmesi gereken tek an o.
 *    Kendi gücü var ve [enabled] anahtarından bilerek bağımsız.
 */
class Haptics(
    private val view: View?,
    private val vibrator: Vibrator?,
    private val enabled: Boolean,
    private val endStrength: TimerEndVibration,
) {
    fun perform(kind: Haptic?) {
        if (!enabled || kind == null) return
        view?.performHapticFeedback(constantFor(kind))
    }

    /** Süre dolduğunda; App bunu bip ve kırmızı flaşla birlikte tetikler. */
    fun timerEnd() = play(endStrength)

    /** Ayarlar'da bir güç seçilirken o kalıbı bir kez örnekler. */
    fun previewEnd(strength: TimerEndVibration) = play(strength)

    private fun play(strength: TimerEndVibration) {
        val vibrator = vibrator ?: return
        val timings: LongArray
        val amplitudes: IntArray
        when (strength) {
            TimerEndVibration.Off -> return
            TimerEndVibration.Medium -> {
                timings = longArrayOf(0, 90, 70, 90)
                amplitudes = intArrayOf(0, 150, 0, 150)
            }
            TimerEndVibration.High -> {
                timings = longArrayOf(0, 160, 90, 160, 90, 260)
                amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
            }
        }

        // ToneGenerator gibi titreşim de bazı cihazlarda patlayabiliyor.
        runCatching {
            val effect = if (vibrator.hasAmplitudeControl()) {
                VibrationEffect.createWaveform(timings, amplitudes, NO_REPEAT)
            } else {
                VibrationEffect.createWaveform(timings, NO_REPEAT)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                vibrator.vibrate(
                    effect,
                    VibrationAttributes.createForUsage(VibrationAttributes.USAGE_NOTIFICATION),
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(effect, NOTIFICATION_ATTRIBUTES)
            }
        }
    }

    /**
     * Anlamın en yakın sistem karşılığı. Yeni sabitler (CONFIRM, REJECT,
     * TOGGLE_*) sonradan geldiği için sürüm kontrollü; taban sürümlerde
     * elde olan en yakın his kullanılıyor.
     */
    private fun constantFor(kind: Haptic): Int = when (kind) {
        Haptic.Tap -> HapticFeedbackConstants.KEYBOARD_TAP
        Haptic.Select, Haptic.Tick -> HapticFeedbackConstants.CLOCK_TICK
        Haptic.LongPress -> HapticFeedbackConstants.LONG_PRESS

        Haptic.ToggleOn ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                HapticFeedbackConstants.TOGGLE_ON
            } else {
                HapticFeedbackConstants.CONTEXT_CLICK
            }

        Haptic.ToggleOff ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                HapticFeedbackConstants.TOGGLE_OFF
            } else {
                HapticFeedbackConstants.CLOCK_TICK
            }

        Haptic.Confirm ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.CONFIRM
            } else {
                HapticFeedbackConstants.LONG_PRESS
            }

        Haptic.Reject ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.REJECT
            } else {
                HapticFeedbackConstants.LONG_PRESS
            }

        Haptic.GestureStart ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.GESTURE_START
            } else {
                HapticFeedbackConstants.CLOCK_TICK
            }
    }

    companion object {
        private const val NO_REPEAT = -1

        private val NOTIFICATION_ATTRIBUTES: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        /** Önizleme ve testler için sessiz sürüm. */
        val None = Haptics(null, null, enabled = false, endStrength = TimerEndVibration.Off)
    }
}

/** Derinlemesine geçtiği için CompositionLocal — LocalAccent ile aynı sebep. */
val LocalHaptics = staticCompositionLocalOf { Haptics.None }

@Composable
fun rememberHaptics(enabled: Boolean, endStrength: TimerEndVibration): Haptics {
    val view = LocalView.current
    val context = LocalContext.current
    return remember(view, context, enabled, endStrength) {
        Haptics(view, vibratorOf(context), enabled, endStrength)
    }
}

private fun vibratorOf(context: Context): Vibrator? = runCatching {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }
}.getOrNull()?.takeIf { it.hasVibrator() }
