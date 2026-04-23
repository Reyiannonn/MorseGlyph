package com.morseglyph.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.morseglyph.ui.theme.NothingAccent
import com.morseglyph.ui.theme.NothingBorder
import com.morseglyph.ui.theme.NothingError
import com.morseglyph.ui.theme.NothingInactive
import com.morseglyph.ui.theme.NothingSurface
import com.morseglyph.ui.theme.RobotoMono
import com.morseglyph.viewmodel.TransmissionState

@Composable
fun TransmitStopRow(
    transmissionState: TransmissionState,
    onTransmit: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isTransmitting = transmissionState == TransmissionState.TRANSMITTING
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onTransmit,
            enabled = !isTransmitting,
            shape = RoundedCornerShape(0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = NothingAccent,
                contentColor = NothingSurface,
                disabledContainerColor = NothingInactive,
                disabledContentColor = NothingSurface
            ),
            modifier = Modifier.weight(1f).height(52.dp)
        ) {
            Text(
                text = "TRANSMIT",
                fontFamily = RobotoMono,
                fontSize = 13.sp,
                letterSpacing = 3.sp
            )
        }
        OutlinedButton(
            onClick = onStop,
            enabled = isTransmitting,
            shape = RoundedCornerShape(0.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = NothingError,
                disabledContentColor = NothingInactive
            ),
            border = BorderStroke(
                1.dp,
                if (isTransmitting) NothingError else NothingBorder
            ),
            modifier = Modifier.weight(1f).height(52.dp)
        ) {
            Text(
                text = "STOP",
                fontFamily = RobotoMono,
                fontSize = 13.sp,
                letterSpacing = 3.sp
            )
        }
    }
}
