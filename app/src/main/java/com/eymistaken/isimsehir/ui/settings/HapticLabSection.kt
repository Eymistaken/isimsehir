package com.eymistaken.isimsehir.ui.settings

import android.os.Build
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.eymistaken.isimsehir.model.HapticStrength
import com.eymistaken.isimsehir.model.TimerEndVibration
import com.eymistaken.isimsehir.ui.components.Eyebrow
import com.eymistaken.isimsehir.ui.components.FlowRowSimple
import com.eymistaken.isimsehir.ui.components.SectionRule
import com.eymistaken.isimsehir.ui.components.tapNoRipple
import com.eymistaken.isimsehir.ui.haptics.Haptic
import com.eymistaken.isimsehir.ui.haptics.LAB_CONSTANTS
import com.eymistaken.isimsehir.ui.haptics.LAB_PREDEFINED
import com.eymistaken.isimsehir.ui.haptics.LAB_PRIMITIVES
import com.eymistaken.isimsehir.ui.haptics.LabEffect
import com.eymistaken.isimsehir.ui.haptics.rememberHapticLab
import com.eymistaken.isimsehir.ui.haptics.rememberHaptics
import com.eymistaken.isimsehir.ui.theme.AppText
import com.eymistaken.isimsehir.ui.theme.Cream
import com.eymistaken.isimsehir.ui.theme.LocalAccent
import com.eymistaken.isimsehir.ui.theme.OnInk16
import com.eymistaken.isimsehir.ui.theme.OnInk30
import com.eymistaken.isimsehir.ui.theme.OnInk45
import com.eymistaken.isimsehir.ui.theme.OnInk60
import kotlin.math.roundToInt

/**
 * GEÇİCİ — titreşim laboratuvarı.
 *
 * Cihazın desteklediği bütün darbeleri tek tek denemek için. Doğru karakter
 * bulununca [com.eymistaken.isimsehir.ui.haptics.Haptics] ona göre ayarlanıp
 * bu dosya ile HapticLab.kt silinecek.
 */
@Composable
fun HapticLabSection(
    hapticStrength: HapticStrength,
    timerEndVibration: TimerEndVibration,
) {
    var open by remember { mutableStateOf(false) }
    val lab = rememberHapticLab()

    // Uygulamanın kendi sözlüğü; laboratuvarda ayardan bağımsız hep çalsın.
    val appHaptics = rememberHaptics(
        enabled = true,
        strength = hapticStrength,
        endStrength = timerEndVibration,
    )

    var scale by remember { mutableFloatStateOf(0.5f) }
    var duration by remember { mutableFloatStateOf(12f) }
    var amplitude by remember { mutableFloatStateOf(90f) }

    SectionRule("Geliştirici · geçici")
    Spacer(Modifier.height(16.dp))

    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Titreşim laboratuvarı", style = AppText.body, color = Cream)
            Text(
                "Her darbeyi tek tek dene, hangisi doğruysa söyle",
                style = AppText.caption,
                color = OnInk45,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
        Text(
            if (open) "GİZLE" else "AÇ",
            style = AppText.eyebrow(10, 0.20),
            color = OnInk60,
            modifier = Modifier
                .clip(RoundedCornerShape(percent = 50))
                .border(1.5.dp, OnInk16, RoundedCornerShape(percent = 50))
                .tapNoRipple { open = !open }
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }

    if (!open) return

    Spacer(Modifier.height(14.dp))
    Text(
        buildString {
            append("Android ${Build.VERSION.SDK_INT} · ")
            append(if (lab.hasVibrator) "motor var" else "motor yok")
            append(" · ")
            append(if (lab.hasAmplitudeControl) "genlik kontrolü var" else "genlik kontrolü yok")
            append(" · ${lab.supportedPrimitiveCount()}/${LAB_PRIMITIVES.size} primitive")
        },
        style = AppText.caption,
        color = OnInk45,
    )

    LabGroup(
        title = "Primitive · şiddet ayarlanabilir",
        note = "Donanımın kendi ayarlı darbeleri. Vurmalı motorda asıl fark burada.",
    ) {
        LabSlider(
            label = "Şiddet",
            value = scale,
            range = 0.05f..1f,
            display = { "%.2f".format(it) },
            onChange = { scale = it },
        )
        Spacer(Modifier.height(10.dp))
        LabChips(LAB_PRIMITIVES, lab::supports) { lab.play(it, scale) }
    }

    LabGroup(
        title = "Hazır efektler",
        note = "Şiddetleri cihaz belirler, ayarlanamaz.",
    ) {
        LabChips(LAB_PREDEFINED, lab::supports) { lab.play(it, scale) }
    }

    LabGroup(
        title = "Sistem sabitleri",
        note = "performHapticFeedback yolu; üretici kalibrasyonu.",
    ) {
        LabChips(LAB_CONSTANTS, lab::supports) { lab.play(it, scale) }
    }

    LabGroup(
        title = "Serbest darbe",
        note = "Süre ve genliği kendin ver — düz blok titreşimin sınırını görmek için.",
    ) {
        LabSlider(
            label = "Süre",
            value = duration,
            range = 1f..80f,
            display = { "${it.roundToInt()} ms" },
            onChange = { duration = it },
        )
        Spacer(Modifier.height(8.dp))
        LabSlider(
            label = "Genlik",
            value = amplitude,
            range = 1f..255f,
            display = { it.roundToInt().toString() },
            onChange = { amplitude = it },
        )
        Spacer(Modifier.height(10.dp))
        LabChip(
            label = "ÇAL",
            enabled = lab.hasVibrator,
            onClick = { lab.playOneShot(duration.roundToInt(), amplitude.roundToInt()) },
        )
    }

    LabGroup(
        title = "Uygulamanın kendi sözlüğü",
        note = "Şu anki ayarla (${hapticStrength.label}) gerçekte ne çalıyorsa o.",
    ) {
        FlowRowSimple(horizontalGap = 7.dp, verticalGap = 7.dp) {
            Haptic.entries.forEach { kind ->
                LabChip(
                    label = kind.name,
                    enabled = true,
                    onClick = { appHaptics.perform(kind) },
                )
            }
        }
    }

    Spacer(Modifier.height(6.dp))
}

@Composable
private fun LabGroup(title: String, note: String, content: @Composable () -> Unit) {
    Spacer(Modifier.height(20.dp))
    Eyebrow(title, OnInk60, size = 10, tracking = 0.18)
    Text(
        note,
        style = AppText.caption,
        color = OnInk30,
        modifier = Modifier.padding(top = 3.dp, bottom = 10.dp),
    )
    content()
}

@Composable
private fun LabChips(
    effects: List<LabEffect>,
    supported: (LabEffect) -> Boolean,
    onPlay: (LabEffect) -> Unit,
) {
    FlowRowSimple(horizontalGap = 7.dp, verticalGap = 7.dp) {
        effects.forEach { effect ->
            val enabled = supported(effect)
            LabChip(
                label = if (enabled) effect.label else "${effect.label} ·",
                enabled = enabled,
                onClick = { onPlay(effect) },
            )
        }
    }
}

@Composable
private fun LabChip(label: String, enabled: Boolean, onClick: () -> Unit) {
    val accent = LocalAccent.current
    val shape = RoundedCornerShape(10.dp)
    Box(
        Modifier
            .height(38.dp)
            .clip(shape)
            .border(1.5.dp, if (enabled) accent.copy(alpha = 0.55f) else OnInk16, shape)
            .tapNoRipple(enabled = enabled, haptic = null, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = AppText.buttonLabel(11, 0.08),
            color = if (enabled) Cream else OnInk30,
        )
    }
}

@Composable
private fun LabSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    display: (Float) -> String,
    onChange: (Float) -> Unit,
) {
    val accent = LocalAccent.current
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Eyebrow(label, OnInk45, size = 10, tracking = 0.16)
            Text(display(value), style = AppText.caption, color = Cream)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = Cream.copy(alpha = 0.16f),
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
