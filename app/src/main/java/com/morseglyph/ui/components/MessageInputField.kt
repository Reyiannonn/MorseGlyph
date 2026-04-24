package com.morseglyph.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.morseglyph.ui.theme.NothingAccent
import com.morseglyph.ui.theme.NothingBorder
import com.morseglyph.ui.theme.NothingDim
import com.morseglyph.ui.theme.NothingError
import com.morseglyph.ui.theme.NothingSurface
import com.morseglyph.ui.theme.RobotoMono

@Composable
fun MessageInputField(
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboardManager.current
    val isError = error != null
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = { if (it.length <= 100) onValueChange(it) },
            label = {
                Text(
                    text = "ENTER MESSAGE",
                    fontFamily = RobotoMono,
                    fontSize = 11.sp,
                    letterSpacing = 2.sp
                )
            },
            trailingIcon = {
                IconButton(onClick = {
                    val pasted = clipboard.getText()?.text ?: return@IconButton
                    val combined = (value + pasted).take(100)
                    onValueChange(combined)
                }) {
                    Icon(
                        imageVector = Icons.Filled.ContentPaste,
                        contentDescription = "Paste",
                        tint = NothingDim
                    )
                }
            },
            isError = isError,
            supportingText = {
                if (isError) {
                    Text(text = error!!, color = NothingError, fontFamily = RobotoMono, fontSize = 11.sp)
                } else {
                    Text(
                        text = "${value.length}/100",
                        color = if (value.length > 90) NothingError else NothingDim,
                        fontFamily = RobotoMono,
                        fontSize = 11.sp
                    )
                }
            },
            shape = RoundedCornerShape(0.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NothingAccent,
                unfocusedBorderColor = NothingBorder,
                errorBorderColor = NothingError,
                focusedContainerColor = NothingSurface,
                unfocusedContainerColor = NothingSurface,
                errorContainerColor = NothingSurface,
                focusedTextColor = NothingAccent,
                unfocusedTextColor = NothingAccent,
                cursorColor = NothingAccent,
                focusedLabelColor = NothingAccent,
                unfocusedLabelColor = NothingDim,
                errorLabelColor = NothingError
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = false,
            maxLines = 3
        )
    }
}
