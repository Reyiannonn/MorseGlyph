package com.morseglyph.morse

data class TransmitEvent(
    val symbol: TimedSymbol,
    val wordIndex: Int = -1,
    val letterIndex: Int = -1,
    val symbolIndexInLetter: Int = -1,
    val symbolType: MorseSymbol? = null
)
