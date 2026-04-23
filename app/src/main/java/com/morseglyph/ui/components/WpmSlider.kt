package com.morseglyph.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.morseglyph.ui.theme.NothingAccent
import com.morseglyph.ui.theme.NothingBorder
import com.morseglyph.ui.theme.NothingDim
import com.morseglyph.ui.theme.NothingInactive
import com.morseglyph.ui.theme.RobotoMono

@Composable
fun WpmSlider(
    wpm: Int,
    onWpmChange: (Int) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SPEED",
                color = NothingDim,
                fontFamily = RobotoMono,
                fontSize = 11.sp,
                letterSpacing = 2.sp
            )
            Text(
                text = " — $wpm WPM",
                color = NothingAccent,
                fontFamily = RobotoMono,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "5            30",
                color = NothingInactive,
                fontFamily = RobotoMono,
                fontSize = 10.sp
            )
        }
        Slider(
            value = wpm.toFloat(),
            onValueChange = { onWpmChange(it.toInt()) },
            valueRange = 5f..30f,
            steps = 24,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = NothingAccent,
                activeTrackColor = NothingAccent,
                inactiveTrackColor = NothingBorder
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
