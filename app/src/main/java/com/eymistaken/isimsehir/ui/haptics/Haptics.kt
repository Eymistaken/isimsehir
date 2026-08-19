package com.eymistaken.isimsehir.ui.haptics

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import com.eymistaken.isimsehir.model.HapticStrength
import com.eymistaken.isimsehir.model.TimerEndVibration
import kotlin.math.roundToInt

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
 * Arayüz titreşimleri kendi kalıplarımızla çalınıyor: `performHapticFeedback`
 * sabitleri (KEYBOARD_TAP, LONG_PRESS, CONFIRM) üreticinin kalibrasyonuna
 * bağlı ve çoğu cihazda bir oyun için fazla sert kaçıyor; genlik ve süreyi
 * kendimiz verince "hafif" gerçekten hafif oluyor.
 *
 * Genlik kontrolü olmayan cihazlarda bu mümkün değil — orada sistemin en
 * yumuşak sabitlerine düşülüyor.
 *
 * Süre bitişi ayrı kanal: kendi gücü var ve [enabled] anahtarından bağımsız,
 * çünkü telefon cepteyken hissedilmesi gereken tek an o.
 */
class Haptics(
    private val view: View?,
    private val vibrator: Vibrator?,
    private val enabled: Boolean,
    private val strength: HapticStrength,
    private val endStrength: TimerEndVibration,
    /** Sistemin "dokunsal geri bildirim" ayarı. Vibrator yolu buna kendisi uymaz. */
    private val systemHapticsEnabled: Boolean,
) {
    private val canShapeAmplitude: Boolean =
        vibrator?.let { runCatching { it.hasAmplitudeControl() }.getOrDefault(false) } ?: false

    fun perform(kind: Haptic?) {
        if (!enabled || kind == null) return
        if (canShapeAmplitude && systemHapticsEnabled) {
            playTouch(kind, strength)
        } else {
            // Genliği ayarlayamıyoruz; sistemin en yumuşak sabitleri kalıyor.
            view?.performHapticFeedback(fallbackConstant(kind))
        }
    }

    /** Ayarlar'da bir güç seçilirken o gücü bir kez örnekler. */
    fun previewTouch(preview: HapticStrength) {
        if (canShapeAmplitude && systemHapticsEnabled) {
            playTouch(Haptic.Tap, preview)
        } else {
            view?.performHapticFeedback(fallbackConstant(Haptic.Tap))
        }
    }

    /** Süre dolduğunda; App bunu bip ve kırmızı flaşla birlikte tetikler. */
    fun timerEnd() = playEnd(endStrength)

    /** Ayarlar'da süre bitişi gücü seçilirken o kalıbı bir kez örnekler. */
    fun previewEnd(preview: TimerEndVibration) = playEnd(preview)

    // ------------------------------------------------------------ arayüz

    /**
     * Milisaniye ve genlik (1-255) çiftleri. Genliği 0 olan parçalar iki
     * darbe arasındaki sessizlik. Değerler "Orta"ya göre yazıldı; seçilen
     * güç bunları ölçekliyor.
     */
    private fun pattern(kind: Haptic): Pair<LongArray, IntArray> = when (kind) {
        Haptic.Tick -> longArrayOf(8) to intArrayOf(65)
        Haptic.Select -> longArrayOf(10) to intArrayOf(80)
        Haptic.Tap -> longArrayOf(11) to intArrayOf(90)
        Haptic.GestureStart -> longArrayOf(10) to intArrayOf(75)
        Haptic.ToggleOff -> longArrayOf(10) to intArrayOf(80)
        Haptic.ToggleOn -> longArrayOf(13) to intArrayOf(100)
        Haptic.LongPress -> longArrayOf(18) to intArrayOf(120)
        // Onay ve ret: tek darbe değil, iki parçalı — güçle değil biçimle ayrışsın.
        Haptic.Confirm -> longArrayOf(11, 55, 15) to intArrayOf(85, 0, 110)
        Haptic.Reject -> longArrayOf(16, 45, 16) to intArrayOf(110, 0, 110)
    }

    private fun playTouch(kind: Haptic, strength: HapticStrength) {
        val vibrator = vibrator ?: return
        val (timings, amplitudes) = pattern(kind)
        val scaled = IntArray(amplitudes.size) { i ->
            val value = amplitudes[i]
            if (value == 0) 0 else (value * strength.factor).roundToInt().coerceIn(1, 255)
        }
        runCatching {
            val effect = VibrationEffect.createWaveform(timings, scaled, NO_REPEAT)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // USAGE_TOUCH: sistem kullanıcının dokunma titreşimi yoğunluğunu da uygular.
                vibrator.vibrate(
                    effect,
                    VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH),
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(effect, TOUCH_ATTRIBUTES)
            }
        }
    }

    /** Genlik kontrolü yokken elde kalan en yumuşak sistem sabitleri. */
    private fun fallbackConstant(kind: Haptic): Int = when (kind) {
        Haptic.LongPress -> HapticFeedbackConstants.LONG_PRESS

        Haptic.Confirm ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.CONFIRM
            } else {
                HapticFeedbackConstants.CLOCK_TICK
            }

        Haptic.Reject ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.REJECT
            } else {
                HapticFeedbackConstants.CLOCK_TICK
            }

        // Geri kalan her şey seçim/tık ailesinden: en hafif olan.
        else ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                HapticFeedbackConstants.SEGMENT_TICK
            } else {
                HapticFeedbackConstants.CLOCK_TICK
            }
    }

    // ------------------------------------------------------- süre bitişi

    private fun playEnd(strength: TimerEndVibration) {
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
            val effect = if (canShapeAmplitude) {
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

    companion object {
        private const val NO_REPEAT = -1

        private val TOUCH_ATTRIBUTES: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        private val NOTIFICATION_ATTRIBUTES: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        /** Önizleme ve testler için sessiz sürüm. */
        val None = Haptics(
            view = null,
            vibrator = null,
            enabled = false,
            strength = HapticStrength.Light,
            endStrength = TimerEndVibration.Off,
            systemHapticsEnabled = false,
        )
    }
}

/** Derinlemesine geçtiği için CompositionLocal — LocalAccent ile aynı sebep. */
val LocalHaptics = staticCompositionLocalOf { Haptics.None }

@Composable
fun rememberHaptics(
    enabled: Boolean,
    strength: HapticStrength,
    endStrength: TimerEndVibration,
): Haptics {
    val view = LocalView.current
    val context = LocalContext.current
    return remember(view, context, enabled, strength, endStrength) {
        Haptics(
            view = view,
            vibrator = vibratorOf(context),
            enabled = enabled,
            strength = strength,
            endStrength = endStrength,
            systemHapticsEnabled = systemHapticsEnabled(context),
        )
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

/**
 * Cihaz ayarlarındaki dokunsal geri bildirim anahtarı. Bir kez okunuyor;
 * kullanıcı bunu oyun açıkken değiştirirse uygulamanın yeniden açılması gerekir.
 */
private fun systemHapticsEnabled(context: Context): Boolean = runCatching {
    Settings.System.getInt(context.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) == 1
}.getOrDefault(true)
