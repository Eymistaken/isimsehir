package com.eymistaken.isimsehir.ui.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eymistaken.isimsehir.BuildConfig
import com.eymistaken.isimsehir.model.HapticChoice
import com.eymistaken.isimsehir.model.HapticStrength
import com.eymistaken.isimsehir.model.TimerEndVibration
import com.eymistaken.isimsehir.ui.components.SectionRule
import com.eymistaken.isimsehir.ui.theme.AppText
import com.eymistaken.isimsehir.ui.theme.OnInk45

/**
 * Deneysel şeylerin yaşadığı yer. Yalnızca `-dev` sürümlerinde görünür:
 * `BuildConfig.DEV_BUILD`, sürüm adındaki `-dev` ekinden geliyor (yerel debug
 * derlemelerinde de açık). Normal bir sürümde bu bölüm hiç çizilmez.
 *
 * Yeni bir deney eklerken buraya bir blok daha koymak yeterli.
 */
@Composable
fun DeveloperSection(
    hapticStrength: HapticStrength,
    hapticChoice: HapticChoice?,
    timerEndVibration: TimerEndVibration,
    onHapticChoiceChange: (HapticChoice?) -> Unit,
) {
    if (!BuildConfig.DEV_BUILD) return

    Spacer(Modifier.height(30.dp))
    SectionRule("Geliştirici")
    Spacer(Modifier.height(10.dp))
    Text(
        "Yalnızca -dev sürümlerinde görünür",
        style = AppText.caption,
        color = OnInk45,
    )

    HapticLabBlock(
        hapticStrength = hapticStrength,
        hapticChoice = hapticChoice,
        timerEndVibration = timerEndVibration,
        onChoose = onHapticChoiceChange,
    )
}
