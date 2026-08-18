package com.eymistaken.isimsehir.model

import androidx.compose.ui.graphics.Color

/** Ayarlar'daki dört vurgu rengi. Tasarımdaki swatch sırası korunuyor. */
enum class AccentColor(val hex: Long, val key: String) {
    Amber(0xFFE07B26, "amber"),
    Gold(0xFFE8B44A, "gold"),
    Pine(0xFF4E8C7B, "pine"),
    Rust(0xFFC2412D, "rust");

    val color: Color get() = Color(hex)

    companion object {
        fun fromKey(key: String?) = entries.firstOrNull { it.key == key } ?: Amber
    }
}
