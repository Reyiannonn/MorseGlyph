package com.morseglyph

import com.morseglyph.morse.MorseSymbol
import com.morseglyph.morse.MorseTranslator
import com.morseglyph.morse.TimedSymbol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MorseTranslatorTest {

    @Test fun `translate single dot letter E`() {
        val words = MorseTranslator.translate("E")
        assertEquals(1, words.size)
        assertEquals(1, words[0].letters.size)
        assertEquals(listOf(MorseSymbol.DOT), words[0].letters[0].symbols)
    }

    @Test fun `translate SOS`() {
        val words = MorseTranslator.translate("SOS")
        assertEquals(1, words.size)
        val letters = words[0].letters
        assertEquals(3, letters.size)
        assertEquals(listOf(MorseSymbol.DOT, MorseSymbol.DOT, MorseSymbol.DOT), letters[0].symbols)
        assertEquals(listOf(MorseSymbol.DASH, MorseSymbol.DASH, MorseSymbol.DASH), letters[1].symbols)
        assertEquals(listOf(MorseSymbol.DOT, MorseSymbol.DOT, MorseSymbol.DOT), letters[2].symbols)
    }

    @Test fun `translate is case insensitive`() {
        assertEquals(MorseTranslator.translate("A"), MorseTranslator.translate("a"))
    }

    @Test fun `translate digit 0`() {
        val words = MorseTranslator.translate("0")
        assertEquals(
            listOf(MorseSymbol.DASH, MorseSymbol.DASH, MorseSymbol.DASH, MorseSymbol.DASH, MorseSymbol.DASH),
            words[0].letters[0].symbols
        )
    }

    @Test fun `translate digit 5`() {
        val words = MorseTranslator.translate("5")
        assertEquals(
            listOf(MorseSymbol.DOT, MorseSymbol.DOT, MorseSymbol.DOT, MorseSymbol.DOT, MorseSymbol.DOT),
            words[0].letters[0].symbols
        )
    }

    @Test fun `translate space creates two words`() {
        val words = MorseTranslator.translate("HI EM")
        assertEquals(2, words.size)
        assertEquals(2, words[0].letters.size)
        assertEquals(2, words[1].letters.size)
    }

    @Test fun `translate unknown char produces empty symbols`() {
        val words = MorseTranslator.translate("A@B")
        assertEquals(1, words.size)
        val letters = words[0].letters
        assertEquals(3, letters.size)
        assertTrue(letters[1].symbols.isEmpty())
        assertEquals('@', letters[1].char)
    }

    @Test fun `toDisplayString SOS`() {
        val words = MorseTranslator.translate("SOS")
        val display = MorseTranslator.toDisplayString(words)
        assertEquals(". . .   - - -   . . .", display)
    }

    @Test fun `toDisplayString two words`() {
        val words = MorseTranslator.translate("E T")
        val display = MorseTranslator.toDisplayString(words)
        assertEquals(". / -", display)
    }

    @Test fun `toTransmitEvents dot duration at 15 WPM`() {
        val unitMs = 1200L / 15
        val words = MorseTranslator.translate("E")
        val events = MorseTranslator.toTransmitEvents(words, unitMs)
        assertEquals(1, events.size)
        val first = events[0]
        assertTrue(first.symbol is TimedSymbol.Tone)
        assertEquals(unitMs, (first.symbol as TimedSymbol.Tone).durationMs)
        assertEquals(MorseSymbol.DOT, first.symbolType)
    }

    @Test fun `toTransmitEvents dash is 3 units`() {
        val unitMs = 1200L / 15
        val words = MorseTranslator.translate("T")
        val events = MorseTranslator.toTransmitEvents(words, unitMs)
        assertEquals(1, events.size)
        assertEquals(3 * unitMs, (events[0].symbol as TimedSymbol.Tone).durationMs)
    }

    @Test fun `toTransmitEvents letter A has symbol gap between dot and dash`() {
        val unitMs = 1200L / 15
        val words = MorseTranslator.translate("A")
        val events = MorseTranslator.toTransmitEvents(words, unitMs)
        assertEquals(3, events.size)
        assertTrue(events[0].symbol is TimedSymbol.Tone)
        assertTrue(events[1].symbol is TimedSymbol.Silence)
        assertEquals(unitMs, (events[1].symbol as TimedSymbol.Silence).durationMs)
        assertTrue(events[2].symbol is TimedSymbol.Tone)
    }

    @Test fun `toTransmitEvents two letters have letter gap between them`() {
        val unitMs = 1200L / 15
        val words = MorseTranslator.translate("ET")
        val events = MorseTranslator.toTransmitEvents(words, unitMs)
        assertEquals(3, events.size)
        assertTrue(events[1].symbol is TimedSymbol.Silence)
        assertEquals(3 * unitMs, (events[1].symbol as TimedSymbol.Silence).durationMs)
    }

    @Test fun `toTransmitEvents two words have word gap between them`() {
        val unitMs = 1200L / 15
        val words = MorseTranslator.translate("E T")
        val events = MorseTranslator.toTransmitEvents(words, unitMs)
        assertEquals(3, events.size)
        assertEquals(7 * unitMs, (events[1].symbol as TimedSymbol.Silence).durationMs)
    }

    @Test fun `toTransmitEvents position metadata is correct`() {
        val unitMs = 1200L / 15
        val words = MorseTranslator.translate("A")
        val events = MorseTranslator.toTransmitEvents(words, unitMs)
        assertEquals(0, events[0].wordIndex)
        assertEquals(0, events[0].letterIndex)
        assertEquals(0, events[0].symbolIndexInLetter)
        assertEquals(-1, events[1].symbolIndexInLetter)
        assertEquals(1, events[2].symbolIndexInLetter)
    }

    @Test fun `wpm timing formula`() {
        assertEquals(240L, 1200L / 5)
        assertEquals(40L, 1200L / 30)
    }
}
