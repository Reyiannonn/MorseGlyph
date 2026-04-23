package com.morseglyph.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.morseglyph.ui.theme.NothingAccent
import com.morseglyph.ui.theme.NothingBorder
import com.morseglyph.ui.theme.NothingDim
import com.morseglyph.ui.theme.NothingSurface
import com.morseglyph.ui.theme.RobotoMono
import com.morseglyph.viewmodel.IndicatorMode

private val modes = listOf(
    IndicatorMode.SYMBOL to "SYM",
    IndicatorMode.FULL_STRING to "FULL",
    IndicatorMode.PER_LETTER to "LETTER"
)

@Composable
fun IndicatorModeSelector(
    selected: IndicatorMode,
    onSelect: (IndicatorMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth()) {
        modes.forEach { (mode, label) ->
            val isSelected = mode == selected
            OutlinedButton(
                onClick = { onSelect(mode) },
                shape = RoundedCornerShape(0.dp),
                border = BorderStroke(1.dp, if (isSelected) NothingAccent else NothingBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isSelected) NothingAccent else NothingSurface,
                    contentColor = if (isSelected) NothingSurface else NothingDim
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
            ) {
                Text(
                    text = label,
                    fontFamily = RobotoMono,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
