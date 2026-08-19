package com.eymistaken.isimsehir.model

/**
 * Arayüz titreşimlerinin gücü. Cihazdan cihaza motor çok değiştiği için
 * kullanıcıya bırakılıyor; varsayılan en hafifi.
 */
enum class HapticStrength(val key: String, val label: String, val factor: Float) {
    Light("light", "Hafif", 0.40f),
    Medium("medium", "Orta", 0.75f),
    Strong("strong", "Güçlü", 1.15f);

    companion object {
        fun fromKey(key: String?) = entries.firstOrNull { it.key == key } ?: Light
    }
}
