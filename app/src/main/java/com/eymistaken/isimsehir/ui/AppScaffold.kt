package com.eymistaken.isimsehir.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eymistaken.isimsehir.R
import com.eymistaken.isimsehir.ui.components.AccentPillButton
import com.eymistaken.isimsehir.ui.components.OutlinePillButton
import com.eymistaken.isimsehir.ui.components.tapNoRipple
import com.eymistaken.isimsehir.ui.theme.AppText
import com.eymistaken.isimsehir.ui.theme.Cream
import com.eymistaken.isimsehir.ui.theme.Ink
import com.eymistaken.isimsehir.ui.theme.InkDeep
import com.eymistaken.isimsehir.ui.theme.LocalAccent
import com.eymistaken.isimsehir.ui.theme.OnCream55
import com.eymistaken.isimsehir.ui.theme.OnInk42
import com.eymistaken.isimsehir.vm.DialogRequest
import com.eymistaken.isimsehir.vm.Tab

/** Alt navigasyon — Nav bileşeninin karşılığı: üç sekme, aktif olan accent. */
@Composable
fun BottomNav(active: Tab, onSelect: (Tab) -> Unit) {
    val accent = LocalAccent.current
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Column(
        Modifier
            .fillMaxWidth()
            .background(Ink),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Cream.copy(alpha = 0.08f)),
        )
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp + bottomInset),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            NavItem(Tab.Game, "Oyun", R.drawable.ic_nav_game, active, accent, Modifier.weight(1f), onSelect)
            NavItem(Tab.Rounds, "Turlar", R.drawable.ic_nav_rounds, active, accent, Modifier.weight(1f), onSelect)
            NavItem(Tab.Settings, "Ayarlar", R.drawable.ic_nav_settings, active, accent, Modifier.weight(1f), onSelect)
        }
    }
}

@Composable
private fun NavItem(
    tab: Tab,
    label: String,
    iconRes: Int,
    active: Tab,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier,
    onSelect: (Tab) -> Unit,
) {
    val tint = if (tab == active) accent else OnInk42
    Column(
        modifier
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .tapNoRipple { onSelect(tab) },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = label.uppercase(java.util.Locale.forLanguageTag("tr-TR")),
            style = AppText.eyebrow(11, 0.10, androidx.compose.ui.text.font.FontWeight.SemiBold),
            color = tint,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 5.dp),
        )
    }
}

/** Üst çentik/durum çubuğu için güvenli boşluk. */
@Composable
fun StatusBarSpacer() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding()),
    )
}

/** Onay ve uyarı kutusu. Web sürümündeki `showConfirmationModal` karşılığı. */
@Composable
fun ConfirmDialog(request: DialogRequest, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(InkDeep.copy(alpha = 0.7f))
            .tapNoRipple(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .padding(horizontal = 28.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Cream)
                .tapNoRipple {}
                .padding(24.dp),
        ) {
            Text(request.title, style = AppText.sheetTitle, color = Ink)
            Text(
                request.message,
                style = AppText.captionMd,
                color = OnCream55,
                modifier = Modifier.padding(top = 8.dp, bottom = 22.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (request.onConfirm != null) {
                    OutlinePillButton(
                        label = "İptal",
                        height = 48.dp,
                        contentColor = Ink.copy(alpha = 0.7f),
                        borderColor = Ink.copy(alpha = 0.2f),
                        modifier = Modifier.weight(1f),
                        onClick = onDismiss,
                    )
                }
                AccentPillButton(
                    label = request.confirmLabel,
                    height = 48.dp,
                    fontSize = 13,
                    tracking = 0.12,
                    modifier = Modifier.weight(1f),
                    onClick = onConfirm,
                )
            }
        }
    }
}
