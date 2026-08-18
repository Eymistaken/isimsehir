package com.eymistaken.isimsehir.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.eymistaken.isimsehir.model.AccentColor
import com.eymistaken.isimsehir.model.DURATION_PRESETS
import com.eymistaken.isimsehir.ui.components.AccentPillButton
import com.eymistaken.isimsehir.ui.components.Eyebrow
import com.eymistaken.isimsehir.ui.components.FlowRowSimple
import com.eymistaken.isimsehir.ui.components.SectionRule
import com.eymistaken.isimsehir.ui.components.ToggleSwitch
import com.eymistaken.isimsehir.ui.components.tapNoRipple
import com.eymistaken.isimsehir.ui.theme.AppText
import com.eymistaken.isimsehir.ui.theme.Cream
import com.eymistaken.isimsehir.ui.theme.Ink
import com.eymistaken.isimsehir.ui.theme.LocalAccent
import com.eymistaken.isimsehir.ui.theme.OnInk16
import com.eymistaken.isimsehir.ui.theme.OnInk30
import com.eymistaken.isimsehir.ui.theme.OnInk45
import com.eymistaken.isimsehir.ui.theme.OnInk60

/**
 * Ayarlar. Tasarımdaki 6. ekran — dört bölüm, her biri dar kesim etiket ve
 * ince bir çizgiyle ayrılıyor.
 *
 * Web sürümündeki serbest renk seçicileri, bulanıklık kaydırıcısı ve
 * "Güncellemeleri kontrol et" butonu burada yok: ilk ikisi tasarımda tek bir
 * vurgu rengi seçimine indirgendi, sonuncusu ise yalnızca PWA önbelleğini
 * temizlemek içindi ve APK'da karşılığı yok.
 */
@Composable
fun SettingsScreen(
    categories: List<String>,
    accent: AccentColor,
    floatingTimer: Boolean,
    durationSeconds: Int,
    versionName: String,
    onAddCategory: (String) -> Unit,
    onRemoveCategory: (String) -> Unit,
    onAccentChange: (AccentColor) -> Unit,
    onFloatingTimerChange: (Boolean) -> Unit,
    onDurationChange: (Int) -> Unit,
) {
    var draft by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 24.dp),
    ) {
        Text("Ayarlar", style = AppText.screenTitle, color = Cream)

        Spacer(Modifier.height(26.dp))
        SectionRule("Kategoriler")
        Spacer(Modifier.height(14.dp))
        FlowRowSimple(horizontalGap = 7.dp, verticalGap = 7.dp) {
            categories.forEach { category ->
                CategoryChip(category) { onRemoveCategory(category) }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(Modifier.weight(1f)) {
                BasicTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    singleLine = true,
                    textStyle = AppText.body.copy(color = Cream),
                    cursorBrush = SolidColor(LocalAccent.current),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        onAddCategory(draft)
                        draft = ""
                    }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp, vertical = 8.dp),
                )
                if (draft.isEmpty()) {
                    Text(
                        "Yeni kategori",
                        style = AppText.body,
                        color = OnInk30,
                        modifier = Modifier.padding(horizontal = 2.dp, vertical = 8.dp),
                    )
                }
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(1.5.dp)
                        .background(OnInk16),
                )
            }
            AccentPillButton(
                label = "Ekle",
                height = 40.dp,
                fontSize = 12,
                modifier = Modifier.width(84.dp),
            ) {
                onAddCategory(draft)
                draft = ""
            }
        }

        Spacer(Modifier.height(30.dp))
        SectionRule("Zamanlayıcı")
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Yüzen zamanlayıcı", style = AppText.body, color = Cream)
                Text(
                    "Ekranda sürüklenebilir kalır",
                    style = AppText.caption,
                    color = OnInk45,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
            ToggleSwitch(floatingTimer) { onFloatingTimerChange(!floatingTimer) }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            DURATION_PRESETS.forEach { preset ->
                DurationChip(
                    label = preset.label,
                    selected = preset.seconds == durationSeconds,
                    modifier = Modifier.weight(1f),
                ) { onDurationChange(preset.seconds) }
            }
        }

        Spacer(Modifier.height(30.dp))
        SectionRule("Vurgu rengi")
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AccentColor.entries.forEach { option ->
                Swatch(option, option == accent) { onAccentChange(option) }
            }
        }

        Spacer(Modifier.height(30.dp))
        Text(
            "Sürüm $versionName · turlar oturum boyunca tutulur, ayarlar kalıcıdır",
            style = AppText.caption,
            color = OnInk45,
        )
    }
}

@Composable
private fun CategoryChip(label: String, onRemove: () -> Unit) {
    Row(
        Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(percent = 50))
            .border(1.5.dp, OnInk16, RoundedCornerShape(percent = 50))
            .padding(start = 13.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(label, style = AppText.body.copy(fontSize = AppText.body.fontSize), color = Cream)
        Box(
            Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(Cream.copy(alpha = 0.10f))
                .tapNoRipple(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Text("×", style = AppText.caption, color = OnInk60)
        }
    }
}

@Composable
private fun DurationChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val accent = LocalAccent.current
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier
            .height(44.dp)
            .clip(shape)
            .background(if (selected) accent else Color.Transparent)
            .border(1.5.dp, if (selected) accent else Cream.copy(alpha = 0.14f), shape)
            .tapNoRipple(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = AppText.buttonLabel(13, 0.06),
            color = if (selected) Ink else OnInk60,
        )
    }
}

@Composable
private fun Swatch(option: AccentColor, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(percent = 50))
            .border(2.dp, if (selected) Cream else Color.Transparent, RoundedCornerShape(percent = 50))
            .tapNoRipple(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(option.color),
        )
    }
}
