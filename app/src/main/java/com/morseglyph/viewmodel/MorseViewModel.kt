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
            indicatorMode = prefsRepository.getIndicatorMode()
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

    fun transmit() {
        val state = _uiState.value
        if (state.inputText.isBlank()) {
            _uiState.update { it.copy(inputError = "Enter a message") }
            return
        }
        val events = state.transmitEvents
        if (events.isEmpty()) return

        transmissionJob = viewModelScope.launch {
            try {
                _uiState.update { it.copy(
                    transmissionState = TransmissionState.TRANSMITTING,
                    inputError = null,
                    snackbarMessage = null
                )}
                events.forEachIndexed { index, event ->
                    _uiState.update { it.copy(activeEventIndex = index) }
                    when (val sym = event.symbol) {
                        is TimedSymbol.Tone -> coroutineScope {
                            launch { glyphController.flashOn(sym.durationMs) }
                            launch { audioController.beep(sym.durationMs) }
                        }
                        is TimedSymbol.Silence -> glyphController.flashOff(sym.durationMs)
                    }
                }
            } finally {
                glyphController.turnOffImmediate()
                _uiState.update { it.copy(
                    transmissionState = TransmissionState.IDLE,
                    activeEventIndex = -1
                )}
            }
        }
    }

    fun stop() {
        transmissionJob?.cancel()
        // IDLE state and glyph cleanup handled by the try/finally in transmit()
    }

    fun onGlyphBindFailed() {
        _uiState.update { it.copy(snackbarMessage = "Glyph unavailable — audio only") }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    private fun unitMs(wpm: Int = _uiState.value.wpm): Long = 1200L / wpm.coerceAtLeast(1)
}
