package com.morseglyph

import android.os.Bundle
import android.os.PowerManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.morseglyph.audio.AudioController
import com.morseglyph.data.SharedPrefsRepository
import com.morseglyph.glyph.GlyphController
import com.morseglyph.ui.MorseScreen
import com.morseglyph.ui.OnboardingScreen
import com.morseglyph.ui.theme.MorseGlyphTheme
import com.morseglyph.viewmodel.MorseViewModel
import com.morseglyph.viewmodel.MorseViewModelFactory
import com.morseglyph.viewmodel.TransmissionState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var glyphController: GlyphController
    private lateinit var audioController: AudioController
    private lateinit var wakeLock: PowerManager.WakeLock
    private lateinit var prefsRepository: SharedPrefsRepository

    private val viewModel: MorseViewModel by viewModels {
        MorseViewModelFactory(glyphController, audioController, prefsRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        prefsRepository = SharedPrefsRepository(this)
        audioController = AudioController()
        glyphController = GlyphController(
            context = this,
            onBindFailed = { viewModel.onGlyphBindFailed() }
        )
        lifecycle.addObserver(glyphController)

        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MorseGlyph::TransmitLock")

        lifecycleScope.launch {
            viewModel.uiState
                .map { it.transmissionState }
                .distinctUntilChanged()
                .collect { state ->
                    if (state == TransmissionState.TRANSMITTING) {
                        if (!wakeLock.isHeld) wakeLock.acquire(10 * 60 * 1000L)
                    } else {
                        if (wakeLock.isHeld) wakeLock.release()
                    }
                }
        }

        setContent {
            MorseGlyphTheme {
                var showOnboarding by rememberSaveable { mutableStateOf(prefsRepository.isFirstLaunch()) }
                if (showOnboarding) {
                    OnboardingScreen(onFinish = {
                        prefsRepository.markOnboarded()
                        showOnboarding = false
                    })
                } else {
                    MorseScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::wakeLock.isInitialized && wakeLock.isHeld) wakeLock.release()
    }
}
