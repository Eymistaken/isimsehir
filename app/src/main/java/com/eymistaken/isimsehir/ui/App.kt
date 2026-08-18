package com.eymistaken.isimsehir.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eymistaken.isimsehir.BuildConfig
import com.eymistaken.isimsehir.ui.game.EmptyGameState
import com.eymistaken.isimsehir.ui.game.GameScreen
import com.eymistaken.isimsehir.ui.letters.LetterPickerSheet
import com.eymistaken.isimsehir.ui.rounds.RoundsScreen
import com.eymistaken.isimsehir.ui.settings.SettingsScreen
import com.eymistaken.isimsehir.ui.theme.Danger
import com.eymistaken.isimsehir.ui.theme.Ink
import com.eymistaken.isimsehir.ui.theme.IsimSehirTheme
import com.eymistaken.isimsehir.ui.wheel.WheelScreen
import com.eymistaken.isimsehir.ui.timer.TimerLayer
import com.eymistaken.isimsehir.ui.timer.playTimerEndTone
import com.eymistaken.isimsehir.vm.GameSurface
import com.eymistaken.isimsehir.vm.GameViewModel
import com.eymistaken.isimsehir.vm.Tab
import kotlinx.coroutines.launch

/** Bitiş flaşının en yüksek opaklığı ve sönme süresi. */
private const val FLASH_PEAK = 0.32f
private const val FLASH_MILLIS = 420

/**
 * Uygulamanın tek giriş noktası. Sekmeler, katmanlar (harf seçimi,
 * zamanlayıcı paneli, onay kutusu) ve yüzen zamanlayıcı burada üst üste binir.
 */
@Composable
fun App(vm: GameViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val flash = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        val scope = this
        vm.timerFinished.collect {
            scope.launch { playTimerEndTone() }
            flash.snapTo(FLASH_PEAK)
            flash.animateTo(0f, tween(FLASH_MILLIS, easing = LinearEasing))
        }
    }

    IsimSehirTheme(accent = state.settings.accent.color) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Ink),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .imePadding(),
            ) {
                StatusBarSpacer()

                Box(Modifier.weight(1f)) {
                    when (state.tab) {
                        Tab.Game -> GameTab(vm)
                        Tab.Rounds -> RoundsScreen(
                            rounds = state.completedRounds,
                            grandTotal = state.grandTotal,
                            onDeleteRound = vm::requestDeleteRound,
                        )
                        Tab.Settings -> SettingsScreen(
                            categories = state.settings.categories,
                            accent = state.settings.accent,
                            floatingTimer = state.settings.floatingTimer,
                            durationSeconds = state.settings.durationSeconds,
                            versionName = BuildConfig.VERSION_NAME,
                            onAddCategory = vm::addCategory,
                            onRemoveCategory = vm::removeCategory,
                            onAccentChange = vm::setAccent,
                            onFloatingTimerChange = vm::setFloatingTimer,
                            onDurationChange = vm::setDuration,
                        )
                    }
                }

                BottomNav(active = state.tab, onSelect = vm::selectTab)
            }

            TimerLayer(
                timer = state.timer,
                floatingEnabled = state.settings.floatingTimer,
                panelOpen = state.timerPanelOpen,
                onOpen = vm::openTimerPanel,
                onDismiss = vm::closeTimerPanel,
                onStart = vm::startTimer,
                onTogglePause = vm::togglePauseTimer,
                onCancel = vm::cancelTimer,
            )

            if (state.letterPickerOpen) {
                LetterPickerSheet(
                    usedLetters = state.usedLetters,
                    onPick = vm::startRoundWithLetter,
                    onDismiss = vm::closeLetterPicker,
                )
            }

            // Süre dolduğunda ekran kısa bir an kırmızıya çalar. Dokunmayı
            // engellememesi için yalnızca boyanan, tıklama almayan bir katman.
            if (flash.value > 0f) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Danger.copy(alpha = flash.value)),
                )
            }

            state.dialog?.let { request ->
                ConfirmDialog(
                    request = request,
                    onConfirm = vm::confirmDialog,
                    onDismiss = vm::dismissDialog,
                )
            }
        }
    }
}

@Composable
private fun GameTab(vm: GameViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val round = state.activeRound

    when {
        round != null -> GameScreen(
            round = round,
            timerText = state.timer.display,
            timerFinished = state.timer.finished,
            // Yüzen zamanlayıcı kapalıysa süreye üst bardan erişilir.
            showTimerPill = !state.settings.floatingTimer,
            onTimerClick = vm::openTimerPanel,
            onWordChange = vm::updateWord,
            onScoreToggle = vm::toggleScore,
            onEnterScoring = vm::enterScoring,
            onExitScoring = vm::exitScoring,
            onFinish = vm::finishRound,
        )

        state.surface == GameSurface.Wheel -> WheelScreen(
            usedLetters = state.usedLetters,
            spinTarget = state.spinTarget,
            onSpin = vm::spin,
            onSpinFinished = vm::onSpinFinished,
            onPickManually = vm::openLetterPicker,
        )

        else -> EmptyGameState(onNewRound = vm::openWheel)
    }
}
