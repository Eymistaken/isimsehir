package com.eymistaken.isimsehir.ui.timer

import android.media.AudioManager
import android.media.ToneGenerator
import kotlinx.coroutines.delay

private const val TONE_MILLIS = 250
private const val VOLUME = 80

/**
 * Süre dolduğunda çalan kısa bip. Web sürümündeki 880 Hz'lik WebAudio sesinin
 * karşılığı; APK'ya ses dosyası koymamak için ToneGenerator kullanılıyor.
 *
 * Bildirim akışına bağlı, yani telefon sessizdeyse ses çıkmaz. ToneGenerator
 * bazı cihazlarda kurulurken hata fırlatabildiği için çağrı sarmalanmış.
 */
suspend fun playTimerEndTone() {
    val tone = runCatching {
        ToneGenerator(AudioManager.STREAM_NOTIFICATION, VOLUME)
    }.getOrNull() ?: return

    try {
        tone.startTone(ToneGenerator.TONE_PROP_BEEP, TONE_MILLIS)
        delay(TONE_MILLIS + 150L)
    } finally {
        tone.release()
    }
}
