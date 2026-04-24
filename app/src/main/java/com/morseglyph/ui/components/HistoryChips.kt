package com.morseglyph.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.morseglyph.ui.theme.NothingBorder
import com.morseglyph.ui.theme.NothingDim
import com.morseglyph.ui.theme.NothingInactive
import com.morseglyph.ui.theme.RobotoMono

@Composable
fun HistoryChips(
    history: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (history.isEmpty()) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        history.forEach { message ->
            OutlinedButton(
                onClick = { onSelect(message) },
                shape = RoundedCornerShape(0.dp),
                border = BorderStroke(1.dp, NothingBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = NothingDim,
                    disabledContentColor = NothingInactive
                )
            ) {
                Text(
                    text = message,
                    fontFamily = RobotoMono,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
