package com.morseglyph.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.morseglyph.audio.AudioController
import com.morseglyph.data.SharedPrefsRepository
import com.morseglyph.glyph.GlyphController
import com.morseglyph.morse.MorseTranslator
import com.morseglyph.morse.TimedSymbol
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MorseViewModel(
    private val glyphController: GlyphController,
    private val audioController: AudioController,
    private val prefsRepository: SharedPrefsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MorseUiState())
    val uiState: StateFlow<MorseUiState> = _uiState.asStateFlow()

    private var transmissionJob: Job? = null

    init {
        _uiState.update { it.copy(
            wpm = prefsRepository.getWpm(),
            indicatorMode = prefsRepository.getIndicatorMode(),
            messageHistory = prefsRepository.getHistory()
        )}
    }

    fun onInputTextChange(text: String) {
        if (text.length > 100) return
        val words = if (text.isBlank()) emptyList() else MorseTranslator.translate(text)
        val unitMs = unitMs()
        _uiState.update { it.copy(
            inputText = text,
            morseDisplayString = if (text.isBlank()) "" else MorseTranslator.toDisplayString(words),
            morseWords = words,
            transmitEvents = MorseTranslator.toTransmitEvents(words, unitMs),
            inputError = null
        )}
    }

    fun onWpmChange(wpm: Int) {
        prefsRepository.saveWpm(wpm)
        val words = _uiState.value.morseWords
        val unitMs = unitMs(wpm)
        _uiState.update { it.copy(
            wpm = wpm,
            transmitEvents = MorseTranslator.toTransmitEvents(words, unitMs)
        )}
    }

    fun onIndicatorModeChange(mode: IndicatorMode) {
        prefsRepository.saveIndicatorMode(mode)
        _uiState.update { it.copy(indicatorMode = mode) }
    }

    fun onLoopModeChange(enabled: Boolean) {
        _uiState.update { it.copy(loopMode = enabled) }
    }

    fun transmit() {
        val state = _uiState.value
        if (state.transmissionState == TransmissionState.TRANSMITTING) return
        if (state.inputText.isBlank()) {
            _uiState.update { it.copy(inputError = "Enter a message") }
            return
        }
        val events = state.transmitEvents
        if (events.isEmpty()) return

        prefsRepository.addToHistory(state.inputText)
        _uiState.update { it.copy(messageHistory = prefsRepository.getHistory()) }

        transmissionJob = viewModelScope.launch {
            try {
                _uiState.update { it.copy(
                    transmissionState = TransmissionState.TRANSMITTING,
                    inputError = null,
                    snackbarMessage = null
                )}
                do {
                    events.forEachIndexed { index, event ->
                        _uiState.update { it.copy(activeEventIndex = index) }
                        when (val sym = event.symbol) {
                            is TimedSymbol.Tone -> coroutineScope {
                                val mode = _uiState.value.indicatorMode
                                val symbolType = if (mode == IndicatorMode.SYMBOL) event.symbolType else null
                                val letterSymbols = if (mode == IndicatorMode.PER_LETTER) {
                                    _uiState.value.morseWords
                                        .getOrNull(event.wordIndex)
                                        ?.letters?.getOrNull(event.letterIndex)
                                        ?.symbols
                                } else null
                                launch { glyphController.flashOn(sym.durationMs, symbolType, letterSymbols) }
                                launch { audioController.beep(sym.durationMs) }
                            }
                            is TimedSymbol.Silence -> glyphController.flashOff(sym.durationMs)
                        }
                    }
                    if (_uiState.value.loopMode) {
                        _uiState.update { it.copy(activeEventIndex = -1) }
                        glyphController.flashOff(7 * unitMs())
                    }
                } while (_uiState.value.loopMode)
            } finally {
                glyphController.turnOffImmediate()
                _uiState.update { it.copy(
                    transmissionState = TransmissionState.IDLE,
                    activeEventIndex = -1
                )}
            }
        }
    }

    fun transmitSos() {
        onInputTextChange("SOS")
        transmit()
    }

    fun stop() {
        transmissionJob?.cancel()
    }

    fun onGlyphBindFailed() {
        _uiState.update { it.copy(
            glyphUnavailable = true,
            snackbarMessage = "Glyph unavailable — audio only"
        )}
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    private fun unitMs(wpm: Int = _uiState.value.wpm): Long = 1200L / wpm.coerceAtLeast(1)
}
