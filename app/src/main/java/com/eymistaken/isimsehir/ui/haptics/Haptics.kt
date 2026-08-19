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
import com.eymistaken.isimsehir.model.HapticChoice
import com.eymistaken.isimsehir.model.HapticFamily
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
 * Arayüz titreşimlerinin tamamı **tek bir hazır efekt** çalıyor. Cihazda denendi:
 * EFFECT_CLICK bu motorda EFFECT_HEAVY_CLICK'ten daha sert ve pürüzlü
 * hissettiriyor, HEAVY_CLICK ise tok. Etkileşime göre ayrıştırmak — tık, seçim,
 * onay diye — kulağa doğru geliyordu ama elde sert/yumuşak salınımı yarattı;
 * her yerde aynı darbe daha tutarlı.
 *
 * [Haptic] türleri yine de duruyor: bir etkileşimin titreşim verip vermediğini
 * onlar belirliyor (null = sessiz) ve ileride yeniden ayrıştırmak istenirse
 * çağrı yerlerine dokunmadan yapılabilir.
 *
 * Şiddet ayarı hangi efektin çalacağını seçiyor: Hafif TICK, Orta HEAVY_CLICK,
 * Güçlü DOUBLE_CLICK. Laboratuvardan bir darbe seçilmişse ([choice]) o merdivenin
 * yerine geçer.
 *
 * Hazır efektlerin olmadığı sürümlerde (Android 9 ve altı) tepe + kısa sönüm
 * biçiminde yazılmış kendi dalgamız, genlik kontrolü de yoksa sistemin
 * sabitleri devreye giriyor.
 *
 * Süre bitişi ayrı kanal: kendi gücü var, kendi kalıbı var ve [enabled]
 * anahtarından bağımsız — telefon cepteyken hissedilmesi gereken tek an o.
 */
class Haptics(
    private val view: View?,
    private val vibrator: Vibrator?,
    private val enabled: Boolean,
    private val strength: HapticStrength,
    /** Laboratuvardan seçilmiş darbe; varsa güç merdiveninin yerine geçer. */
    private val choice: HapticChoice?,
    private val endStrength: TimerEndVibration,
    /** Sistemin "dokunsal geri bildirim" ayarı. Vibrator yolu buna kendisi uymaz. */
    private val systemHapticsEnabled: Boolean,
) {
    private val canShapeAmplitude: Boolean =
        vibrator?.let { runCatching { it.hasAmplitudeControl() }.getOrDefault(false) } ?: false

    /** [kind] yalnızca "titreşecek mi" sorusunu yanıtlıyor; null ise sessiz. */
    fun perform(kind: Haptic?) {
        if (!enabled || kind == null) return
        val choice = choice
        if (choice != null) playChoice(choice) else play(strength)
    }

    /** Ayarlar'da bir güç seçilirken o gücü bir kez örnekler. */
    fun previewTouch(preview: HapticStrength) = play(preview)

    /**
     * Laboratuvardaki bir darbeyi çalar. Hem oradaki önizleme hem de seçilmiş
     * darbenin oyundaki karşılığı buradan geçiyor — iki yerde iki farklı
     * çalma kodu olmasın diye.
     */
    fun playChoice(choice: HapticChoice) {
        if (!systemHapticsEnabled) return
        val vibrator = vibrator
        val effect = when (choice.family) {
            HapticFamily.Constant -> {
                view?.performHapticFeedback(choice.id)
                return
            }

            HapticFamily.Predefined ->
                if (vibrator != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    runCatching { VibrationEffect.createPredefined(choice.id) }.getOrNull()
                } else {
                    null
                }

            HapticFamily.Primitive ->
                if (vibrator != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    primitiveEffect(choice.id, choice.scale)
                } else {
                    null
                }

            HapticFamily.OneShot ->
                if (vibrator != null) {
                    runCatching {
                        VibrationEffect.createOneShot(
                            choice.durationMs.coerceAtLeast(1).toLong(),
                            if (canShapeAmplitude) {
                                choice.amplitude.coerceIn(1, 255)
                            } else {
                                VibrationEffect.DEFAULT_AMPLITUDE
                            },
                        )
                    }.getOrNull()
                } else {
                    null
                }
        }

        if (vibrator != null && effect != null) runCatching { vibrateTouch(vibrator, effect) }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun primitiveEffect(id: Int, scale: Float): VibrationEffect? = runCatching {
        VibrationEffect.startComposition()
            .addPrimitive(id, scale.coerceIn(0.05f, 1f))
            .compose()
    }.getOrNull()

    /** Süre dolduğunda; App bunu bip ve kırmızı flaşla birlikte tetikler. */
    fun timerEnd() = playEnd(endStrength)

    /** Ayarlar'da süre bitişi gücü seçilirken o kalıbı bir kez örnekler. */
    fun previewEnd(preview: TimerEndVibration) = playEnd(preview)

    // ------------------------------------------------------------ arayüz

    private fun play(strength: HapticStrength) {
        if (!systemHapticsEnabled) return
        val vibrator = vibrator
        if (vibrator == null) {
            view?.performHapticFeedback(fallbackConstant(strength))
            return
        }

        val effect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            predefinedEffect(strength)
        } else {
            null
        } ?: if (canShapeAmplitude) waveformEffect(strength) else null

        if (effect != null) {
            runCatching { vibrateTouch(vibrator, effect) }
        } else {
            view?.performHapticFeedback(fallbackConstant(strength))
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun predefinedEffect(strength: HapticStrength): VibrationEffect? =
        runCatching {
            VibrationEffect.createPredefined(
                when (strength) {
                    // CLICK bilerek yok: bu motorda HEAVY_CLICK'ten daha sert ve
                    // pürüzlü çıkıyor, yani "hafif" kademeye hiç uymuyor.
                    HapticStrength.Light -> VibrationEffect.EFFECT_TICK
                    HapticStrength.Medium -> VibrationEffect.EFFECT_HEAVY_CLICK
                    HapticStrength.Strong -> VibrationEffect.EFFECT_DOUBLE_CLICK
                },
            )
        }.getOrNull()

    /**
     * Hazır efektlerin olmadığı sürümler için tek darbe: tepe + kısa sönüm.
     * Düz ve alçak bir blok motoru oturtamadan bıraktığı için pürüzlü
     * hissettiriyordu; zarf bunu düzeltiyor. Güç ayarı genliği ölçekliyor.
     */
    private fun waveformEffect(strength: HapticStrength): VibrationEffect? =
        runCatching {
            val timings = longArrayOf(4, 7)
            val amplitudes = intArrayOf(200, 75)
            val scaled = IntArray(amplitudes.size) { i ->
                (amplitudes[i] * strength.factor).roundToInt().coerceIn(1, 255)
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

    /** Genliği hiç ayarlayamayan cihazlarda elde kalan sabitler. */
    private fun fallbackConstant(strength: HapticStrength): Int = when (strength) {
        HapticStrength.Light -> HapticFeedbackConstants.CLOCK_TICK
        HapticStrength.Medium -> HapticFeedbackConstants.KEYBOARD_TAP
        HapticStrength.Strong -> HapticFeedbackConstants.LONG_PRESS
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
            choice = null,
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
    choice: HapticChoice?,
    endStrength: TimerEndVibration,
): Haptics {
    val view = LocalView.current
    val context = LocalContext.current
    return remember(view, context, enabled, strength, choice, endStrength) {
        Haptics(
            view = view,
            vibrator = vibratorOf(context),
            enabled = enabled,
            strength = strength,
            choice = choice,
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
