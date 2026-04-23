package com.morseglyph.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.morseglyph.morse.MorseWord
import com.morseglyph.morse.TransmitEvent
import com.morseglyph.ui.theme.NothingBorder
import com.morseglyph.ui.theme.NothingSurface
import com.morseglyph.viewmodel.IndicatorMode

@Composable
fun LiveIndicator(
    mode: IndicatorMode,
    words: List<MorseWord>,
    events: List<TransmitEvent>,
    activeIndex: Int,
    modifier: Modifier = Modifier
) {
    val boxMod = modifier
        .fillMaxWidth()
        .background(NothingSurface)
        .border(1.dp, NothingBorder)
        .padding(8.dp)

    when (mode) {
        IndicatorMode.SYMBOL -> SymbolIndicator(events, activeIndex, boxMod)
        IndicatorMode.FULL_STRING -> FullStringIndicator(words, events, activeIndex, boxMod)
        IndicatorMode.PER_LETTER -> PerLetterIndicator(words, events, activeIndex, boxMod)
    }
}
