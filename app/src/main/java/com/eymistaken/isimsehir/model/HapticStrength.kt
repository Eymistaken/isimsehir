package com.eymistaken.isimsehir.model

/**
 * Arayüz titreşimlerinin gücü.
 *
 * Hazır efektlerin (TICK / CLICK / HEAVY_CLICK) şiddeti ayarlanamadığı için bu
 * ayar bir ölçek değil, merdivende kaydırma: [ladderShift] her etkileşimi bir
 * basamak hafifletiyor ya da ağırlaştırıyor. [factor] yalnızca hazır efektlerin
 * bulunmadığı eski sürümlerde, elle yazılmış dalgaların genliğini ölçekliyor.
 */
enum class HapticStrength(
    val key: String,
    val label: String,
    val ladderShift: Int,
    val factor: Float,
) {
    Light("light", "Hafif", -1, 0.40f),
    Medium("medium", "Orta", 0, 0.75f),
    Strong("strong", "Güçlü", 1, 1.15f);

    companion object {
        fun fromKey(key: String?) = entries.firstOrNull { it.key == key } ?: Medium
    }
}
