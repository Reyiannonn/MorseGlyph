package com.morseglyph.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.morseglyph.ui.components.DotMatrixHeader
import com.morseglyph.ui.components.GlyphUnavailableBanner
import com.morseglyph.ui.components.HistoryChips
import com.morseglyph.ui.components.IndicatorModeSelector
import com.morseglyph.ui.components.LiveIndicator
import com.morseglyph.ui.components.MessageInputField
import com.morseglyph.ui.components.MorseOutputField
import com.morseglyph.ui.components.TransmitStopRow
import com.morseglyph.ui.components.WpmSlider
import com.morseglyph.ui.theme.Black
import com.morseglyph.ui.theme.NothingAccent
import com.morseglyph.ui.theme.NothingBorder
import com.morseglyph.ui.theme.NothingDim
import com.morseglyph.ui.theme.NothingSurface
import com.morseglyph.ui.theme.RobotoMono
import com.morseglyph.viewmodel.MorseViewModel
import com.morseglyph.viewmodel.TransmissionState

@Composable
fun MorseScreen(viewModel: MorseViewModel) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val isTransmitting = state.transmissionState == TransmissionState.TRANSMITTING

    LaunchedEffect(state.snackbarMessage) {
        val msg = state.snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearSnackbar()
    }

    Scaffold(
        containerColor = Black,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = NothingSurface,
                    contentColor = NothingAccent,
                    actionColor = NothingAccent
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
        ) {
            DotMatrixHeader(modifier = Modifier.fillMaxWidth())

            if (state.glyphUnavailable) {
                GlyphUnavailableBanner()
            }

            Text(
                text = "MORSEGLYPH",
                color = NothingAccent,
                fontFamily = RobotoMono,
                fontSize = 11.sp,
                letterSpacing = 4.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(NothingBorder)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(Modifier.height(16.dp))

                MessageInputField(
                    value = state.inputText,
                    onValueChange = { viewModel.onInputTextChange(it) },
                    error = state.inputError
                )

                if (state.messageHistory.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "RECENT",
                        color = NothingDim,
                        fontFamily = RobotoMono,
                        fontSize = 9.sp,
                        letterSpacing = 2.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    HistoryChips(
                        history = state.messageHistory,
                        onSelect = { viewModel.onInputTextChange(it) }
                    )
                }

                Spacer(Modifier.height(12.dp))

                MorseOutputField(morseString = state.morseDisplayString)

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "INDICATOR MODE",
                    color = NothingDim,
                    fontFamily = RobotoMono,
                    fontSize = 10.sp,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.height(6.dp))

                IndicatorModeSelector(
                    selected = state.indicatorMode,
                    onSelect = { viewModel.onIndicatorModeChange(it) }
                )

                Spacer(Modifier.height(12.dp))

                LiveIndicator(
                    mode = state.indicatorMode,
                    words = state.morseWords,
                    events = state.transmitEvents,
                    activeIndex = state.activeEventIndex
                )

                Spacer(Modifier.height(16.dp))

                WpmSlider(
                    wpm = state.wpm,
                    onWpmChange = { viewModel.onWpmChange(it) },
                    enabled = !isTransmitting
                )

                Spacer(Modifier.height(20.dp))

                TransmitStopRow(
                    transmissionState = state.transmissionState,
                    loopMode = state.loopMode,
                    onLoopModeChange = { viewModel.onLoopModeChange(it) },
                    onTransmit = { viewModel.transmit() },
                    onStop = { viewModel.stop() },
                    onSos = { viewModel.transmitSos() }
                )

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
