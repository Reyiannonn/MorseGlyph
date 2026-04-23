package com.morseglyph.morse

sealed class TimedSymbol {
    data class Tone(val durationMs: Long) : TimedSymbol()
    data class Silence(val durationMs: Long) : TimedSymbol()
}
