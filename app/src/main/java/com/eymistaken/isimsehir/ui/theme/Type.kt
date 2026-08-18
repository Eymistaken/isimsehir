package com.eymistaken.isimsehir.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.eymistaken.isimsehir.R

/** Gövde ve başlıklar. */
val Archivo = FontFamily(
    Font(R.font.archivo_regular, FontWeight.Normal),
    Font(R.font.archivo_medium, FontWeight.Medium),
    Font(R.font.archivo_semibold, FontWeight.SemiBold),
    Font(R.font.archivo_bold, FontWeight.Bold),
    Font(R.font.archivo_extrabold, FontWeight.ExtraBold),
)

/** Büyük harfli, harf aralığı açılmış etiketler için dar kesim. */
val ArchivoNarrow = FontFamily(
    Font(R.font.archivo_narrow_semibold, FontWeight.SemiBold),
    Font(R.font.archivo_narrow_bold, FontWeight.Bold),
)

/** Sayıların zıplamaması için tabular rakamlar (süre, puan). */
private const val TABULAR = "tnum"

/**
 * Tasarımdaki tekrar eden metin rolleri. Compose'un Typography'si yerine
 * doğrudan bunlar kullanılıyor; tasarım sabit bir ölçek değil, rol listesi.
 */
object AppText {
    /** Ekran başlığı — "Turlar", "Ayarlar". */
    val screenTitle = TextStyle(
        fontFamily = Archivo, fontWeight = FontWeight.ExtraBold,
        fontSize = 30.sp, letterSpacing = (-0.02).em(30),
    )

    /** Bölüm başlığı — "Harfi çark seçsin". */
    val heading = TextStyle(
        fontFamily = Archivo, fontWeight = FontWeight.ExtraBold,
        fontSize = 26.sp, letterSpacing = (-0.02).em(26),
    )

    /** Krem bottom sheet başlığı. */
    val sheetTitle = TextStyle(
        fontFamily = Archivo, fontWeight = FontWeight.ExtraBold,
        fontSize = 22.sp, letterSpacing = (-0.02).em(22),
    )

    /** Turun harfi — krem karttaki dev karakter. */
    val letterHuge = TextStyle(
        fontFamily = Archivo, fontWeight = FontWeight.ExtraBold,
        fontSize = 92.sp, lineHeight = 79.sp, letterSpacing = (-0.04).em(92),
    )

    /** Krem karttaki tur toplamı. */
    val scoreBig = TextStyle(
        fontFamily = Archivo, fontWeight = FontWeight.ExtraBold,
        fontSize = 46.sp, lineHeight = 46.sp, fontFeatureSettings = TABULAR,
    )

    /** Genel toplam. */
    val scoreGrand = TextStyle(
        fontFamily = Archivo, fontWeight = FontWeight.ExtraBold,
        fontSize = 40.sp, lineHeight = 40.sp, fontFeatureSettings = TABULAR,
    )

    /** Tur kartındaki puan. */
    val scoreRow = TextStyle(
        fontFamily = Archivo, fontWeight = FontWeight.ExtraBold,
        fontSize = 26.sp, lineHeight = 26.sp, fontFeatureSettings = TABULAR,
    )

    /** Tur kartındaki harf karosu. */
    val roundLetter = TextStyle(
        fontFamily = Archivo, fontWeight = FontWeight.ExtraBold,
        fontSize = 23.sp, lineHeight = 23.sp,
    )

    /** Zamanlayıcı panelindeki dev süre. */
    val timerHuge = TextStyle(
        fontFamily = Archivo, fontWeight = FontWeight.ExtraBold,
        fontSize = 72.sp, lineHeight = 72.sp,
        letterSpacing = (-0.04).em(72), fontFeatureSettings = TABULAR,
    )

    /** Yazılan/puanlanan kelime. */
    val word = TextStyle(
        fontFamily = Archivo, fontWeight = FontWeight.SemiBold, fontSize = 19.sp,
    )

    /** Liste satırı başlığı, ayar satırı. */
    val body = TextStyle(
        fontFamily = Archivo, fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
    )

    val bodyBold = TextStyle(
        fontFamily = Archivo, fontWeight = FontWeight.Bold, fontSize = 16.sp,
    )

    /** Puan chip'i ("5" / "10") ve harf hücreleri. */
    val chip = TextStyle(
        fontFamily = Archivo, fontWeight = FontWeight.Bold, fontSize = 15.sp,
    )

    /** Yüzen zamanlayıcı hapındaki süre. */
    val timerPanelPill = TextStyle(
        fontFamily = Archivo, fontWeight = FontWeight.ExtraBold,
        fontSize = 17.sp, fontFeatureSettings = TABULAR,
    )

    /** Üst bardaki süre. */
    val timerPill = TextStyle(
        fontFamily = Archivo, fontWeight = FontWeight.Bold,
        fontSize = 14.sp, fontFeatureSettings = TABULAR,
    )

    // --- Dar kesim, büyük harfli etiketler ---

    /** "İSİM ŞEHİR", "PUANLAMA" gibi üst bar etiketleri. */
    fun eyebrow(size: Int = 11, tracking: Double = 0.22, weight: FontWeight = FontWeight.Bold) =
        TextStyle(
            fontFamily = ArchivoNarrow, fontWeight = weight,
            fontSize = size.sp, letterSpacing = tracking.em(size),
        )

    /** Buton yazıları. */
    fun buttonLabel(size: Int = 13, tracking: Double = 0.12) = TextStyle(
        fontFamily = ArchivoNarrow, fontWeight = FontWeight.Bold,
        fontSize = size.sp, letterSpacing = tracking.em(size),
    )

    /** Kart altı açıklaması — "5 kategori · 4 doğru". */
    val caption = TextStyle(
        fontFamily = ArchivoNarrow, fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
    )

    val captionMd = TextStyle(
        fontFamily = ArchivoNarrow, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
    )

    val captionSmall = TextStyle(
        fontFamily = ArchivoNarrow, fontWeight = FontWeight.SemiBold, fontSize = 11.sp,
    )
}

/** CSS'teki `em` cinsinden letter-spacing'i Compose'un `sp` beklentisine çevirir. */
private fun Double.em(fontSizeSp: Int) = (this * fontSizeSp).sp
