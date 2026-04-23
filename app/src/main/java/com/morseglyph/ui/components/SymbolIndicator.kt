package com.morseglyph.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
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
import com.morseglyph.morse.TimedSymbol
import com.morseglyph.morse.TransmitEvent
import com.morseglyph.ui.theme.RobotoMono

@Composable
fun SymbolIndicator(
    events: List<TransmitEvent>,
    activeIndex: Int,
    modifier: Modifier = Modifier
) {
    val activeEvent = events.getOrNull(activeIndex)
    val isTone = activeEvent?.symbol is TimedSymbol.Tone
    val symbolChar = when (activeEvent?.symbolType) {
        MorseSymbol.DOT -> "·"
        MorseSymbol.DASH -> "—"
        null -> if (isTone) "·" else " "
    }
    val alpha by animateFloatAsState(
        targetValue = if (isTone) 1f else 0.12f,
        animationSpec = tween(durationMillis = 40),
        label = "symbol_alpha"
    )
    Box(
        modifier = modifier.fillMaxWidth().height(96.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbolChar,
            fontSize = 80.sp,
            color = Color.White.copy(alpha = alpha),
            fontFamily = RobotoMono
        )
    }
}
