package com.morseglyph.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
fun PerLetterIndicator(
    words: List<MorseWord>,
    events: List<TransmitEvent>,
    activeIndex: Int,
    modifier: Modifier = Modifier
) {
    val activeEvent = events.getOrNull(activeIndex)
    val toneEvent = activeEvent?.takeIf { it.symbol is TimedSymbol.Tone }
    val currentLetter = toneEvent?.let {
        words.getOrNull(it.wordIndex)?.letters?.getOrNull(it.letterIndex)
    }

    Box(
        modifier = modifier.fillMaxWidth().height(96.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = currentLetter?.char?.uppercaseChar()?.toString() ?: "·",
                fontSize = 44.sp,
                color = if (currentLetter != null) NothingAccent else NothingInactive,
                fontFamily = RobotoMono
            )
            Spacer(Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                currentLetter?.symbols?.forEachIndexed { sIdx, sym ->
                    val isActive = toneEvent?.symbolIndexInLetter == sIdx
                    val alpha by animateFloatAsState(
                        targetValue = if (isActive) 1f else 0.25f,
                        animationSpec = tween(40),
                        label = "sym_alpha_$sIdx"
                    )
                    Text(
                        text = if (sym == MorseSymbol.DOT) "·" else "—",
                        fontSize = if (isActive) 26.sp else 20.sp,
                        color = Color.White.copy(alpha = alpha),
                        fontFamily = RobotoMono
                    )
                } ?: run {
                    Text(text = "—", fontSize = 20.sp, color = NothingInactive, fontFamily = RobotoMono)
                }
            }
        }
    }
}
