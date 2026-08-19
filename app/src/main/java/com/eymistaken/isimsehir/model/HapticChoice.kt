package com.eymistaken.isimsehir.model

/** Bir titreşimin nereden geldiği; çalınma biçimi buna göre değişir. */
enum class HapticFamily { Predefined, Primitive, Constant, OneShot }

/**
 * Laboratuvardan seçilip oyunun tamamına uygulanan darbe. Null ise uygulama
 * kendi varsayılanını ([HapticStrength] merdiveni) kullanır.
 *
 * [id]'nin anlamı [family]'ye göre değişir: hazır efekt kimliği, primitive
 * kimliği ya da HapticFeedbackConstants değeri. [scale] yalnızca primitive'de,
 * [durationMs] ve [amplitude] yalnızca serbest darbede kullanılır.
 */
data class HapticChoice(
    val family: HapticFamily,
    val id: Int = 0,
    val scale: Float = 1f,
    val durationMs: Int = 0,
    val amplitude: Int = 0,
    /** Ayarlar'da "şu an bu seçili" diye göstermek için. */
    val label: String = "",
) {
    fun encode(): String =
        listOf(family.name, id, scale, durationMs, amplitude, label).joinToString(SEPARATOR)

    companion object {
        /** Kategorilerdeki ile aynı sebep: etikette geçemeyecek bir ayraç. */
        private const val SEPARATOR = "\u001F"

        fun decode(raw: String?): HapticChoice? {
            if (raw.isNullOrBlank()) return null
            val parts = raw.split(SEPARATOR)
            if (parts.size < 6) return null
            val family = HapticFamily.entries.firstOrNull { it.name == parts[0] } ?: return null
            return HapticChoice(
                family = family,
                id = parts[1].toIntOrNull() ?: return null,
                scale = parts[2].toFloatOrNull() ?: 1f,
                durationMs = parts[3].toIntOrNull() ?: 0,
                amplitude = parts[4].toIntOrNull() ?: 0,
                label = parts[5],
            )
        }
    }
}
