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
import androidx.annotation.RequiresApi
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
 * Arayüz titreşimleri **hazır efektlerle** çalınıyor: EFFECT_TICK, EFFECT_CLICK,
 * EFFECT_HEAVY_CLICK, EFFECT_DOUBLE_CLICK. Bunlar üreticinin kendi motoru için
 * kalibre ettiği darbelerdir; cihazda denenip seçildiler. Şiddetleri
 * ayarlanamaz — ölçeklenebilen primitive'ler ve elle yazılmış dalgalar denendi
 * ama bu motorda hazır efektlerin tokluğunu tutturamadılar.
 *
 * Şiddet ayarı bu yüzden bir ölçek değil, merdivende kaydırma:
 * TICK → CLICK → HEAVY_CLICK. Her etkileşimin merdivende bir basamağı var,
 * Hafif bir aşağı iter, Güçlü bir yukarı.
 *
 * Hazır efektlerin olmadığı sürümlerde (Android 9 ve altı) tepe + kısa sönüm
 * biçiminde yazılmış kendi dalgalarımız, genlik kontrolü de yoksa sistemin
 * yumuşak sabitleri devreye giriyor.
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
        play(kind, strength)
    }

    /** Ayarlar'da bir güç seçilirken o gücü bir kez örnekler. */
    fun previewTouch(preview: HapticStrength) = play(Haptic.Tap, preview)

    /** Süre dolduğunda; App bunu bip ve kırmızı flaşla birlikte tetikler. */
    fun timerEnd() = playEnd(endStrength)

    /** Ayarlar'da süre bitişi gücü seçilirken o kalıbı bir kez örnekler. */
    fun previewEnd(preview: TimerEndVibration) = playEnd(preview)

    // ------------------------------------------------------------ arayüz

    private fun play(kind: Haptic, strength: HapticStrength) {
        if (!systemHapticsEnabled) return
        val vibrator = vibrator
        if (vibrator == null) {
            view?.performHapticFeedback(fallbackConstant(kind))
            return
        }

        val effect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            predefinedEffect(kind, strength)
        } else {
            null
        } ?: if (canShapeAmplitude) waveformEffect(kind, strength) else null

        if (effect != null) {
            runCatching { vibrateTouch(vibrator, effect) }
        } else {
            view?.performHapticFeedback(fallbackConstant(kind))
        }
    }

    /**
     * Etkileşimin merdivendeki basamağı: 0 = TICK, 1 = CLICK, 2 = HEAVY_CLICK.
     * Şiddet ayarı bu basamağı bir aşağı ya da bir yukarı kaydırıyor.
     */
    private fun ladderStep(kind: Haptic): Int = when (kind) {
        Haptic.Tick, Haptic.GestureStart -> 0
        Haptic.Select, Haptic.ToggleOff -> 1
        Haptic.Tap, Haptic.ToggleOn, Haptic.LongPress, Haptic.Reject -> 2
        // Onay merdivende değil: çift darbe, tek başına "oldu" diyor.
        Haptic.Confirm -> -1
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun predefinedEffect(kind: Haptic, strength: HapticStrength): VibrationEffect? =
        runCatching {
            val step = ladderStep(kind)
            val id = if (step < 0) {
                VibrationEffect.EFFECT_DOUBLE_CLICK
            } else {
                EFFECT_LADDER[(step + strength.ladderShift).coerceIn(EFFECT_LADDER.indices)]
            }
            VibrationEffect.createPredefined(id)
        }.getOrNull()

    /**
     * Milisaniye ve genlik (1-255) çiftleri. Her darbe tepe + kısa sönüm;
     * genliği 0 olan parçalar iki darbe arasındaki sessizlik. Değerler
     * "Orta"ya göre yazıldı, seçilen güç bunları ölçekliyor.
     */
    private fun waveform(kind: Haptic): Pair<LongArray, IntArray> = when (kind) {
        Haptic.Tick -> longArrayOf(3, 5) to intArrayOf(120, 45)
        Haptic.Select -> longArrayOf(3, 6) to intArrayOf(140, 50)
        Haptic.GestureStart -> longArrayOf(3, 5) to intArrayOf(120, 45)
        Haptic.ToggleOff -> longArrayOf(3, 6) to intArrayOf(130, 45)
        Haptic.Tap -> longArrayOf(4, 7) to intArrayOf(160, 60)
        Haptic.ToggleOn -> longArrayOf(4, 8) to intArrayOf(170, 65)
        Haptic.LongPress -> longArrayOf(5, 14) to intArrayOf(200, 90)
        Haptic.Confirm -> longArrayOf(3, 6, 50, 4, 10) to intArrayOf(140, 50, 0, 180, 70)
        Haptic.Reject -> longArrayOf(4, 7, 40, 4, 7) to intArrayOf(170, 60, 0, 170, 60)
    }

    private fun waveformEffect(kind: Haptic, strength: HapticStrength): VibrationEffect? =
        runCatching {
            val (timings, amplitudes) = waveform(kind)
            val scaled = IntArray(amplitudes.size) { i ->
                val value = amplitudes[i]
                if (value == 0) 0 else (value * strength.factor).roundToInt().coerceIn(1, 255)
            }
            VibrationEffect.createWaveform(timings, scaled, NO_REPEAT)
        }.getOrNull()

    private fun vibrateTouch(vibrator: Vibrator, effect: VibrationEffect) {
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

    /** Genliği hiç ayarlayamayan cihazlarda elde kalan en yumuşak sabitler. */
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

        /** Hafiften ağıra hazır efektler; şiddet ayarı bu dizide kaydırıyor. */
        private val EFFECT_LADDER = intArrayOf(
            VibrationEffect.EFFECT_TICK,
            VibrationEffect.EFFECT_CLICK,
            VibrationEffect.EFFECT_HEAVY_CLICK,
        )

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

internal fun vibratorOf(context: Context): Vibrator? = runCatching {
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
