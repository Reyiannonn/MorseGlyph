package com.morseglyph.morse

object MorseTranslator {

    private val TABLE: Map<Char, List<MorseSymbol>> = mapOf(
        'A' to listOf(MorseSymbol.DOT, MorseSymbol.DASH),
        'B' to listOf(MorseSymbol.DASH, MorseSymbol.DOT, MorseSymbol.DOT, MorseSymbol.DOT),
        'C' to listOf(MorseSymbol.DASH, MorseSymbol.DOT, MorseSymbol.DASH, MorseSymbol.DOT),
        'D' to listOf(MorseSymbol.DASH, MorseSymbol.DOT, MorseSymbol.DOT),
        'E' to listOf(MorseSymbol.DOT),
        'F' to listOf(MorseSymbol.DOT, MorseSymbol.DOT, MorseSymbol.DASH, MorseSymbol.DOT),
        'G' to listOf(MorseSymbol.DASH, MorseSymbol.DASH, MorseSymbol.DOT),
        'H' to listOf(MorseSymbol.DOT, MorseSymbol.DOT, MorseSymbol.DOT, MorseSymbol.DOT),
        'I' to listOf(MorseSymbol.DOT, MorseSymbol.DOT),
        'J' to listOf(MorseSymbol.DOT, MorseSymbol.DASH, MorseSymbol.DASH, MorseSymbol.DASH),
        'K' to listOf(MorseSymbol.DASH, MorseSymbol.DOT, MorseSymbol.DASH),
        'L' to listOf(MorseSymbol.DOT, MorseSymbol.DASH, MorseSymbol.DOT, MorseSymbol.DOT),
        'M' to listOf(MorseSymbol.DASH, MorseSymbol.DASH),
        'N' to listOf(MorseSymbol.DASH, MorseSymbol.DOT),
        'O' to listOf(MorseSymbol.DASH, MorseSymbol.DASH, MorseSymbol.DASH),
        'P' to listOf(MorseSymbol.DOT, MorseSymbol.DASH, MorseSymbol.DASH, MorseSymbol.DOT),
        'Q' to listOf(MorseSymbol.DASH, MorseSymbol.DASH, MorseSymbol.DOT, MorseSymbol.DASH),
        'R' to listOf(MorseSymbol.DOT, MorseSymbol.DASH, MorseSymbol.DOT),
        'S' to listOf(MorseSymbol.DOT, MorseSymbol.DOT, MorseSymbol.DOT),
        'T' to listOf(MorseSymbol.DASH),
        'U' to listOf(MorseSymbol.DOT, MorseSymbol.DOT, MorseSymbol.DASH),
        'V' to listOf(MorseSymbol.DOT, MorseSymbol.DOT, MorseSymbol.DOT, MorseSymbol.DASH),
        'W' to listOf(MorseSymbol.DOT, MorseSymbol.DASH, MorseSymbol.DASH),
        'X' to listOf(MorseSymbol.DASH, MorseSymbol.DOT, MorseSymbol.DOT, MorseSymbol.DASH),
        'Y' to listOf(MorseSymbol.DASH, MorseSymbol.DOT, MorseSymbol.DASH, MorseSymbol.DASH),
        'Z' to listOf(MorseSymbol.DASH, MorseSymbol.DASH, MorseSymbol.DOT, MorseSymbol.DOT),
        '0' to listOf(MorseSymbol.DASH, MorseSymbol.DASH, MorseSymbol.DASH, MorseSymbol.DASH, MorseSymbol.DASH),
        '1' to listOf(MorseSymbol.DOT, MorseSymbol.DASH, MorseSymbol.DASH, MorseSymbol.DASH, MorseSymbol.DASH),
        '2' to listOf(MorseSymbol.DOT, MorseSymbol.DOT, MorseSymbol.DASH, MorseSymbol.DASH, MorseSymbol.DASH),
        '3' to listOf(MorseSymbol.DOT, MorseSymbol.DOT, MorseSymbol.DOT, MorseSymbol.DASH, MorseSymbol.DASH),
        '4' to listOf(MorseSymbol.DOT, MorseSymbol.DOT, MorseSymbol.DOT, MorseSymbol.DOT, MorseSymbol.DASH),
        '5' to listOf(MorseSymbol.DOT, MorseSymbol.DOT, MorseSymbol.DOT, MorseSymbol.DOT, MorseSymbol.DOT),
        '6' to listOf(MorseSymbol.DASH, MorseSymbol.DOT, MorseSymbol.DOT, MorseSymbol.DOT, MorseSymbol.DOT),
        '7' to listOf(MorseSymbol.DASH, MorseSymbol.DASH, MorseSymbol.DOT, MorseSymbol.DOT, MorseSymbol.DOT),
        '8' to listOf(MorseSymbol.DASH, MorseSymbol.DASH, MorseSymbol.DASH, MorseSymbol.DOT, MorseSymbol.DOT),
        '9' to listOf(MorseSymbol.DASH, MorseSymbol.DASH, MorseSymbol.DASH, MorseSymbol.DASH, MorseSymbol.DOT),
    )

    fun translate(text: String): List<MorseWord> =
        text.uppercase()
            .split(' ')
            .filter { it.isNotEmpty() }
            .map { word ->
                MorseWord(word.map { char ->
                    MorseLetter(char, TABLE[char] ?: emptyList())
                })
            }

    fun toDisplayString(words: List<MorseWord>): String =
        words.joinToString(" / ") { word ->
            word.letters.joinToString("   ") { letter ->
                if (letter.symbols.isEmpty()) "?"
                else letter.symbols.joinToString(" ") { sym ->
                    if (sym == MorseSymbol.DOT) "." else "-"
                }
            }
        }

    fun toTransmitEvents(words: List<MorseWord>, unitMs: Long): List<TransmitEvent> {
        val events = mutableListOf<TransmitEvent>()
        words.forEachIndexed { wIdx, word ->
            word.letters.forEachIndexed { lIdx, letter ->
                if (letter.symbols.isEmpty()) return@forEachIndexed
                letter.symbols.forEachIndexed { sIdx, sym ->
                    val toneDuration = if (sym == MorseSymbol.DOT) unitMs else 3 * unitMs
                    events += TransmitEvent(
                        symbol = TimedSymbol.Tone(toneDuration),
                        wordIndex = wIdx,
                        letterIndex = lIdx,
                        symbolIndexInLetter = sIdx,
                        symbolType = sym
                    )
                    if (sIdx < letter.symbols.lastIndex) {
                        events += TransmitEvent(symbol = TimedSymbol.Silence(unitMs))
                    }
                }
                if (lIdx < word.letters.lastIndex) {
                    events += TransmitEvent(symbol = TimedSymbol.Silence(3 * unitMs))
                }
            }
            if (wIdx < words.lastIndex) {
                events += TransmitEvent(symbol = TimedSymbol.Silence(7 * unitMs))
            }
        }
        return events
    }
}
