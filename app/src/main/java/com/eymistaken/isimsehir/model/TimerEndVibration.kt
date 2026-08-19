package com.eymistaken.isimsehir.model

/**
 * Süre dolduğunda çalan titreşimin gücü. Arayüz dokunuşlarının titreşiminden
 * bağımsız bir ayar: dokunsal geri bildirim kapalıyken bile süre bitişi
 * hissedilebilsin diye ayrı tutuluyor.
 */
enum class TimerEndVibration(val key: String, val label: String) {
    High("high", "Yüksek"),
    Medium("medium", "Orta"),
    Off("off", "Kapalı");

    companion object {
        fun fromKey(key: String?) = entries.firstOrNull { it.key == key } ?: Medium
    }
}
