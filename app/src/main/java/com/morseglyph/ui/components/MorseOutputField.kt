package com.morseglyph.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.morseglyph.ui.theme.NothingBorder
import com.morseglyph.ui.theme.NothingDim
import com.morseglyph.ui.theme.NothingInactive
import com.morseglyph.ui.theme.NothingSurface
import com.morseglyph.ui.theme.RobotoMono

@Composable
fun MorseOutputField(morseString: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .background(NothingSurface)
            .border(1.dp, NothingBorder)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (morseString.isEmpty()) {
            Text(
                text = "MORSE OUTPUT",
                color = NothingInactive,
                fontFamily = RobotoMono,
                fontSize = 11.sp,
                letterSpacing = 2.sp
            )
        } else {
            Text(
                text = morseString,
                color = NothingDim,
                fontFamily = RobotoMono,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Start
            )
        }
    }
}
