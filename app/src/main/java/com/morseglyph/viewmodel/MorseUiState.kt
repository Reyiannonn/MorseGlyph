package com.morseglyph.viewmodel

import com.morseglyph.morse.MorseWord
import com.morseglyph.morse.TransmitEvent

data class MorseUiState(
    val inputText: String = "",
    val morseDisplayString: String = "",
    val morseWords: List<MorseWord> = emptyList(),
    val transmitEvents: List<TransmitEvent> = emptyList(),
    val transmissionState: TransmissionState = TransmissionState.IDLE,
    val activeEventIndex: Int = -1,
    val wpm: Int = 15,
    val indicatorMode: IndicatorMode = IndicatorMode.FULL_STRING,
    val inputError: String? = null,
    val snackbarMessage: String? = null
)
