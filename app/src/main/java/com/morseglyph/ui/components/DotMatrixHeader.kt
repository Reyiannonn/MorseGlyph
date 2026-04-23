package com.morseglyph.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun DotMatrixHeader(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        val dotRadius = 2.dp.toPx()
        val spacing = 12.dp.toPx()
        val cols = (size.width / spacing).toInt()
        val rows = (size.height / spacing).toInt()

        for (row in 0..rows) {
            for (col in 0..cols) {
                val x = col * spacing
                val y = row * spacing
                val alpha = if ((row + col) % 3 == 0) 0.6f else 0.15f
                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = dotRadius,
                    center = Offset(x, y)
                )
            }
        }
    }
}
