package com.eymistaken.isimsehir.model

/**
 * Arayüz titreşimlerinin gücü. Bütün etkileşimler aynı darbeyi çaldığı için bu
 * ayar doğrudan hangi hazır efektin kullanılacağını seçiyor:
 * Hafif CLICK, Orta HEAVY_CLICK, Güçlü DOUBLE_CLICK.
 *
 * [factor] yalnızca hazır efektlerin bulunmadığı eski sürümlerde, elle yazılmış
 * dalganın genliğini ölçeklemek için.
 */
enum class HapticStrength(val key: String, val label: String, val factor: Float) {
    Light("light", "Hafif", 0.55f),
    Medium("medium", "Orta", 0.85f),
    Strong("strong", "Güçlü", 1.15f);

    companion object {
        fun fromKey(key: String?) = entries.firstOrNull { it.key == key } ?: Medium
    }
}
