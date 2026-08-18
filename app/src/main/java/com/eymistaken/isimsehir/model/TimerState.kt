package com.eymistaken.isimsehir.model

/** Hazır süreler — Ayarlar ve zamanlayıcı panelinde aynı liste kullanılıyor. */
data class DurationPreset(val label: String, val seconds: Int)

val DURATION_PRESETS = listOf(
    DurationPreset("10 sn", 10),
    DurationPreset("30 sn", 30),
    DurationPreset("1 dk", 60),
    DurationPreset("5 dk", 300),
)

/**
 * Zamanlayıcının anlık durumu.
 *
 * @param totalSeconds sayımın başladığı süre; ilerleme çubuğunun paydası.
 * @param remainingSeconds kalan saniye.
 * @param running geri sayım işliyor mu (duraklatılmışsa false).
 * @param finished süre bu turda doldu mu — puanlama başlığındaki "SÜRE BİTTİ".
 */
data class TimerState(
    val totalSeconds: Int = 60,
    val remainingSeconds: Int = 60,
    val running: Boolean = false,
    val finished: Boolean = false,
) {
    val active: Boolean get() = running || (remainingSeconds != totalSeconds && !finished)

    val progress: Float
        get() = if (totalSeconds <= 0) 0f else remainingSeconds.toFloat() / totalSeconds

    val display: String
        get() = "${remainingSeconds / 60}:${(remainingSeconds % 60).toString().padStart(2, '0')}"
}
