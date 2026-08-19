package com.eymistaken.isimsehir.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eymistaken.isimsehir.data.Settings
import com.eymistaken.isimsehir.data.SettingsStore
import com.eymistaken.isimsehir.model.ALPHABET
import com.eymistaken.isimsehir.model.AccentColor
import com.eymistaken.isimsehir.model.ActiveRound
import com.eymistaken.isimsehir.model.CompletedRound
import com.eymistaken.isimsehir.model.HapticStrength
import com.eymistaken.isimsehir.model.RoundPhase
import com.eymistaken.isimsehir.model.TimerEndVibration
import com.eymistaken.isimsehir.model.TimerState
import com.eymistaken.isimsehir.model.WordEntry
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

/** Uygulamanın üç sekmesi. */
enum class Tab { Game, Rounds, Settings }

/** Oyun sekmesinde o an hangi yüzey görünüyor. */
enum class GameSurface { Empty, Round, Wheel }

/** Onay/uyarı diyaloğu. [onConfirm] null ise tek butonlu bilgi kutusudur. */
data class DialogRequest(
    val title: String,
    val message: String,
    val confirmLabel: String = "Onayla",
    val onConfirm: (() -> Unit)? = null,
)

data class UiState(
    val tab: Tab = Tab.Game,
    val surface: GameSurface = GameSurface.Empty,
    val settings: Settings = Settings(),
    val activeRound: ActiveRound? = null,
    val completedRounds: List<CompletedRound> = emptyList(),
    val usedLetters: List<Char> = emptyList(),
    val roundCounter: Int = 0,
    val timer: TimerState = TimerState(),
    val spinning: Boolean = false,
    /** Çark dönerken üzerine ineceği harf; animasyon bitince tüketilir. */
    val spinTarget: Char? = null,
    val letterPickerOpen: Boolean = false,
    val timerPanelOpen: Boolean = false,
    val dialog: DialogRequest? = null,
) {
    val grandTotal: Int get() = completedRounds.sumOf { it.total }
    val remainingLetters: List<Char> get() = ALPHABET.filter { it !in usedLetters }
}

class GameViewModel(app: Application) : AndroidViewModel(app) {

    private val store = SettingsStore(app)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _timerFinished = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Süre dolduğu anda bir kez akar. Arayüz ses, flaş ve titreşim için dinler. */
    val timerFinished: SharedFlow<Unit> = _timerFinished.asSharedFlow()

    private val _wordRejected = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /**
     * Kelime alanına tur harfiyle başlamayan bir giriş yapıldığında akar.
     * Alan sessizce temizlendiği için tek geri bildirim titreşim.
     */
    val wordRejected: SharedFlow<Unit> = _wordRejected.asSharedFlow()

    private var timerJob: Job? = null
    private var nextRoundId = 1L

    init {
        viewModelScope.launch {
            store.settings.collect { saved ->
                _state.update { current ->
                    // Süre ayarı değiştiyse ve sayaç boştaysa gösterilen değeri de tazele.
                    val timer = if (current.timer.running || current.timer.finished) {
                        current.timer
                    } else {
                        TimerState(saved.durationSeconds, saved.durationSeconds)
                    }
                    current.copy(settings = saved, timer = timer)
                }
            }
        }
    }

    // ---------------------------------------------------------------- gezinme

    fun selectTab(tab: Tab) = _state.update { it.copy(tab = tab) }

    /** "Yeni tur" akışının girişi: çark ekranını açar. */
    fun openWheel() {
        val s = _state.value
        if (s.activeRound != null) {
            showAlert("Aktif tur var", "Mevcut turu bitirmeden yenisini başlatamazsınız.")
            return
        }
        _state.update { it.copy(tab = Tab.Game, surface = GameSurface.Wheel) }
    }

    fun openLetterPicker() = _state.update { it.copy(letterPickerOpen = true) }

    fun closeLetterPicker() = _state.update { it.copy(letterPickerOpen = false) }

    // ------------------------------------------------------------------ çark

    /**
     * Kullanılmamış harflerden birini seçip döndürme animasyonunu tetikler.
     * Hedef harf state'te bekler; animasyon bitince [onSpinFinished] turu açar.
     * Web sürümündeki `transitionend` akışının karşılığı.
     */
    fun spin() {
        val s = _state.value
        if (s.spinning) return
        val available = s.remainingLetters
        if (available.isEmpty()) {
            showAlert("Tüm harfler kullanıldı", "Yeni tur için harf kalmadı.")
            return
        }
        _state.update { it.copy(spinning = true, spinTarget = available.random()) }
    }

    fun onSpinFinished() {
        val letter = _state.value.spinTarget ?: return
        _state.update { it.copy(spinning = false, spinTarget = null) }
        startRoundWithLetter(letter)
    }

    /**
     * Harf seçiminin tek girişi. Daha önce oynanmış bir harf seçilirse
     * web sürümündeki gibi önce onay ister.
     */
    fun startRoundWithLetter(letter: Char) {
        if (letter in _state.value.usedLetters) {
            _state.update {
                it.copy(
                    letterPickerOpen = false,
                    dialog = DialogRequest(
                        title = "'$letter' kullanılmış",
                        message = "Bu harfle daha önce oynandı. Yine de devam etmek istiyor musun?",
                        onConfirm = { createRound(letter) },
                    ),
                )
            }
        } else {
            createRound(letter)
        }
    }

    private fun createRound(letter: Char) {
        _state.update { s ->
            if (s.activeRound != null) return@update s
            val index = s.roundCounter + 1
            s.copy(
                tab = Tab.Game,
                surface = GameSurface.Round,
                letterPickerOpen = false,
                roundCounter = index,
                usedLetters = if (letter in s.usedLetters) s.usedLetters else s.usedLetters + letter,
                activeRound = ActiveRound(
                    index = index,
                    letter = letter,
                    entries = s.settings.categories.map { WordEntry(it) },
                ),
            )
        }
    }

    // -------------------------------------------------------------------- tur

    /**
     * Kelime alanı. Web sürümüyle aynı kural: ilk harf tur harfiyle
     * uyuşmuyorsa giriş kabul edilmez (alan temizlenir).
     */
    fun updateWord(categoryIndex: Int, raw: String) {
        val current = _state.value.activeRound ?: return
        val required = current.letter.lowercase(TR)
        val rejected = raw.isNotEmpty() && raw.first().lowercase(TR) != required
        // Emit update bloğunun dışında: blok çekişme hâlinde yeniden çalışabilir.
        if (rejected) _wordRejected.tryEmit(Unit)
        val accepted = if (rejected) "" else raw

        _state.update { s ->
            val round = s.activeRound ?: return@update s
            s.copy(
                activeRound = round.copy(
                    entries = round.entries.mapIndexed { i, e ->
                        if (i == categoryIndex) e.copy(word = accepted) else e
                    },
                ),
            )
        }
    }

    /** 5/10 chip'i. Aynı değere tekrar basmak seçimi kaldırır. */
    fun toggleScore(categoryIndex: Int, value: Int) {
        _state.update { s ->
            val round = s.activeRound ?: return@update s
            if (round.phase != RoundPhase.Scoring) return@update s
            s.copy(
                activeRound = round.copy(
                    entries = round.entries.mapIndexed { i, e ->
                        if (i == categoryIndex) e.copy(score = if (e.score == value) 0 else value) else e
                    },
                ),
            )
        }
    }

    fun enterScoring() = _state.update { s ->
        s.copy(activeRound = s.activeRound?.copy(phase = RoundPhase.Scoring))
    }

    fun exitScoring() = _state.update { s ->
        s.copy(activeRound = s.activeRound?.copy(phase = RoundPhase.Writing))
    }

    /** Basılı tutma tamamlandığında turu kapatır ve Turlar listesine yazar. */
    fun finishRound() {
        _state.update { s ->
            val round = s.activeRound ?: return@update s
            val completed = CompletedRound(
                id = nextRoundId++,
                index = round.index,
                letter = round.letter,
                categoryCount = round.entries.size,
                correctCount = round.entries.count { it.score > 0 },
                total = round.total,
            )
            s.copy(
                activeRound = null,
                surface = GameSurface.Empty,
                completedRounds = listOf(completed) + s.completedRounds,
            )
        }
        cancelTimer()
    }

    fun requestDeleteRound(id: Long) {
        _state.update {
            it.copy(
                dialog = DialogRequest(
                    title = "Turu sil",
                    message = "Bu geçmiş tur kaydı kalıcı olarak silinecek.",
                    onConfirm = { deleteRound(id) },
                ),
            )
        }
    }

    private fun deleteRound(id: Long) = _state.update {
        it.copy(completedRounds = it.completedRounds.filterNot { r -> r.id == id })
    }

    // ------------------------------------------------------------- zamanlayıcı

    fun openTimerPanel() = _state.update { it.copy(timerPanelOpen = true) }

    fun closeTimerPanel() = _state.update { it.copy(timerPanelOpen = false) }

    fun startTimer(seconds: Int) {
        _state.update { it.copy(timer = TimerState(seconds, seconds, running = true)) }
        runCountdown(from = seconds, total = seconds)
    }

    fun togglePauseTimer() {
        val timer = _state.value.timer
        if (timer.running) {
            timerJob?.cancel()
            _state.update { it.copy(timer = it.timer.copy(running = false)) }
        } else if (timer.remainingSeconds > 0) {
            _state.update { it.copy(timer = it.timer.copy(running = true, finished = false)) }
            runCountdown(from = timer.remainingSeconds, total = timer.totalSeconds)
        }
    }

    /**
     * Geri sayımın tek gövdesi; hem baştan başlatma hem duraklatmadan sürdürme
     * buraya iner. Süre dolduğunda [timerFinished] tetiklenir ve arayüz
     * bip sesiyle kırmızı flaşı oradan sürer.
     */
    private fun runCountdown(from: Int, total: Int) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var remaining = from
            while (remaining > 0) {
                delay(1000)
                remaining--
                val done = remaining == 0
                _state.update { s ->
                    s.copy(
                        timer = s.timer.copy(
                            totalSeconds = total,
                            remainingSeconds = remaining,
                            running = !done,
                            finished = done,
                        ),
                    )
                }
                if (done) _timerFinished.emit(Unit)
            }
        }
    }

    fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
        val duration = _state.value.settings.durationSeconds
        _state.update { it.copy(timer = TimerState(duration, duration)) }
    }

    // ----------------------------------------------------------------- ayarlar

    fun addCategory(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val current = _state.value.settings.categories
        if (trimmed in current) return
        viewModelScope.launch { store.setCategories(current + trimmed) }
    }

    fun removeCategory(name: String) {
        _state.update {
            it.copy(
                dialog = DialogRequest(
                    title = "Kategoriyi sil",
                    message = "\"$name\" kategorisi silinsin mi?",
                    onConfirm = {
                        viewModelScope.launch {
                            store.setCategories(_state.value.settings.categories - name)
                        }
                    },
                ),
            )
        }
    }

    fun setAccent(accent: AccentColor) = viewModelScope.launch { store.setAccent(accent) }

    fun setFloatingTimer(enabled: Boolean) =
        viewModelScope.launch { store.setFloatingTimer(enabled) }

    fun setDuration(seconds: Int) = viewModelScope.launch { store.setDuration(seconds) }

    fun setHaptics(enabled: Boolean) = viewModelScope.launch { store.setHaptics(enabled) }

    fun setHapticStrength(value: HapticStrength) =
        viewModelScope.launch { store.setHapticStrength(value) }

    fun setTimerEndVibration(value: TimerEndVibration) =
        viewModelScope.launch { store.setTimerEndVibration(value) }

    // ---------------------------------------------------------------- diyalog

    private fun showAlert(title: String, message: String) = _state.update {
        it.copy(dialog = DialogRequest(title, message, confirmLabel = "Tamam"))
    }

    fun dismissDialog() = _state.update { it.copy(dialog = null) }

    fun confirmDialog() {
        val action = _state.value.dialog?.onConfirm
        _state.update { it.copy(dialog = null) }
        action?.invoke()
    }

    private companion object {
        /** Türkçe'de i/İ ve ı/I ayrımı için locale şart. */
        val TR: Locale = Locale.forLanguageTag("tr-TR")
    }
}
