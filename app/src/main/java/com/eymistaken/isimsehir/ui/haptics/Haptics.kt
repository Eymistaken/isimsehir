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
 * Arayüz titreşimleri üç kademeli bir yoldan çalınıyor; hedef "hafif ama tok":
 *
 *  1. **Primitive'ler** (Android 11+, donanım destekliyorsa). CLICK, TICK,
 *     LOW_TICK, THUD üreticinin motor için ayarladığı hazır darbelerdir: keskin
 *     atak, hızlı sönüm. `scale` ile şiddetleri düşürülünce hafifler ama
 *     tokluğunu kaybetmez. Tercih edilen yol bu.
 *  2. **Zarflı dalga** (genlik kontrolü varsa). Düz ve alçak bir dalga motoru
 *     oturtamadan bırakıp pürüzlü hissettirdiği için darbeler tepe + kısa
 *     sönüm biçiminde yazıldı.
 *  3. **Sistem sabitleri.** Genliği hiç ayarlayamayan cihazlarda elde kalan
 *     en yumuşak seçenekler.
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

    /** İstenen primitive'in bu cihazdaki karşılığı; bir kez çözülüp saklanıyor. */
    private val primitiveSupport: Map<Int, Int> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && vibrator != null) {
            resolvePrimitives(vibrator)
        } else {
            emptyMap()
        }

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
        val effect = when {
            vibrator == null -> null
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && primitiveSupport.isNotEmpty() ->
                primitiveEffect(kind, strength)
            else -> null
        } ?: if (vibrator != null && canShapeAmplitude) waveformEffect(kind, strength) else null

        if (vibrator != null && effect != null) {
            runCatching { vibrateTouch(vibrator, effect) }
        } else {
            view?.performHapticFeedback(fallbackConstant(kind))
        }
    }

    /**
     * Primitive dizisi: hangi darbe, ne şiddette (0-1, "Orta" referansı) ve
     * öncekinden kaç ms sonra. Onay ve ret tek darbe değil — güçle değil
     * biçimle ayrışsınlar diye iki parçalı.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun primitiveSteps(kind: Haptic): List<Triple<Int, Float, Int>> = when (kind) {
        Haptic.Tick -> listOf(Triple(LOW_TICK, 0.90f, 0))
        Haptic.Select -> listOf(Triple(TICK, 0.80f, 0))
        Haptic.GestureStart -> listOf(Triple(TICK, 0.65f, 0))
        Haptic.ToggleOff -> listOf(Triple(TICK, 0.85f, 0))
        Haptic.Tap -> listOf(Triple(CLICK, 0.65f, 0))
        Haptic.ToggleOn -> listOf(Triple(CLICK, 0.75f, 0))
        Haptic.LongPress -> listOf(Triple(THUD, 0.85f, 0))
        Haptic.Confirm -> listOf(Triple(CLICK, 0.55f, 0), Triple(THUD, 0.80f, 55))
        Haptic.Reject -> listOf(Triple(CLICK, 0.75f, 0), Triple(CLICK, 0.75f, 45))
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun primitiveEffect(kind: Haptic, strength: HapticStrength): VibrationEffect? =
        runCatching {
            val composition = VibrationEffect.startComposition()
            var added = false
            primitiveSteps(kind).forEach { (wanted, scale, delay) ->
                val primitive = primitiveSupport[wanted] ?: return@forEach
                composition.addPrimitive(
                    primitive,
                    (scale * strength.factor).coerceIn(0.05f, 1f),
                    delay,
                )
                added = true
            }
            if (added) composition.compose() else null
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

        // VibrationEffect.Composition.PRIMITIVE_* değerleri. Sabitler API
        // seviyesine bağlı olduğu için sayı olarak yazıldı; LOW_TICK ve THUD
        // Android 12'de geldi ve her cihazda desteklenmiyor — aşağıdaki
        // zincirle desteklenen en yakınına düşülüyor.
        private const val CLICK = 1
        private const val THUD = 2
        private const val TICK = 7
        private const val LOW_TICK = 8

        /** İstenen primitive yoksa en yakın desteklenene düş. */
        private val PRIMITIVE_CHAINS: Map<Int, IntArray> = mapOf(
            LOW_TICK to intArrayOf(LOW_TICK, TICK, CLICK),
            TICK to intArrayOf(TICK, CLICK),
            CLICK to intArrayOf(CLICK),
            THUD to intArrayOf(THUD, CLICK),
        )

        @RequiresApi(Build.VERSION_CODES.R)
        private fun resolvePrimitives(vibrator: Vibrator): Map<Int, Int> =
            runCatching {
                PRIMITIVE_CHAINS.mapNotNull { (wanted, chain) ->
                    chain.firstOrNull { vibrator.areAllPrimitivesSupported(it) }
                        ?.let { wanted to it }
                }.toMap()
            }.getOrDefault(emptyMap())

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
