package com.eymistaken.isimsehir.model

/**
 * Türkçe alfabe — web sürümündeki dizinin birebir aynısı.
 * `Ğ` ve `Ü` bilerek yok: kelime başında gelmedikleri için oyun dışı.
 */
const val TURKISH_ALPHABET = "ABCÇDEFGHIİJKLMNOÖPRSŞTUVYZ"

val ALPHABET: List<Char> = TURKISH_ALPHABET.toList()

val DEFAULT_CATEGORIES = listOf("İsim", "Şehir", "Hayvan", "Bitki", "Eşya")

/** Bir turdaki tek kategori satırı. */
data class WordEntry(
    val category: String,
    val word: String = "",
    /** 0 = puanlanmadı, 5 veya 10. */
    val score: Int = 0,
)

enum class RoundPhase { Writing, Scoring }

/** Oynanmakta olan tur. */
data class ActiveRound(
    val index: Int,
    val letter: Char,
    val entries: List<WordEntry>,
    val phase: RoundPhase = RoundPhase.Writing,
) {
    val total: Int get() = entries.sumOf { it.score }
}

/** Kapanmış tur — Turlar ekranında listelenir. */
data class CompletedRound(
    val id: Long,
    val index: Int,
    val letter: Char,
    val categoryCount: Int,
    val correctCount: Int,
    val total: Int,
)
