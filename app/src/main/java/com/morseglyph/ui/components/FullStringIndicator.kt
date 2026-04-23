package com.morseglyph.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.morseglyph.morse.MorseSymbol
import com.morseglyph.morse.MorseWord
import com.morseglyph.morse.TimedSymbol
import com.morseglyph.morse.TransmitEvent
import com.morseglyph.ui.theme.NothingAccent
import com.morseglyph.ui.theme.NothingInactive
import com.morseglyph.ui.theme.RobotoMono

@Composable
fun FullStringIndicator(
    words: List<MorseWord>,
    events: List<TransmitEvent>,
    activeIndex: Int,
    modifier: Modifier = Modifier
) {
    val activeEvent = events.getOrNull(activeIndex)
        ?.takeIf { it.symbol is TimedSymbol.Tone }

    val annotated = buildAnnotatedString {
        words.forEachIndexed { wIdx, word ->
            if (wIdx > 0) {
                withStyle(SpanStyle(color = NothingInactive, fontSize = 14.sp)) { append(" / ") }
            }
            word.letters.forEachIndexed { lIdx, letter ->
                if (lIdx > 0) {
                    withStyle(SpanStyle(color = NothingInactive, fontSize = 14.sp)) { append("   ") }
                }
                if (letter.symbols.isEmpty()) {
                    withStyle(SpanStyle(color = NothingInactive, fontSize = 14.sp)) { append("?") }
                    return@forEachIndexed
                }
                letter.symbols.forEachIndexed { sIdx, sym ->
                    if (sIdx > 0) {
                        withStyle(SpanStyle(color = NothingInactive, fontSize = 14.sp)) { append(" ") }
                    }
                    val isActive = activeEvent?.wordIndex == wIdx &&
                                   activeEvent.letterIndex == lIdx &&
                                   activeEvent.symbolIndexInLetter == sIdx
                    val color = if (isActive) NothingAccent else NothingInactive
                    val size = if (isActive) 20.sp else 14.sp
                    withStyle(SpanStyle(color = color, fontSize = size)) {
                        append(if (sym == MorseSymbol.DOT) "·" else "—")
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .horizontalScroll(rememberScrollState()),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = annotated,
            fontFamily = RobotoMono,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}
