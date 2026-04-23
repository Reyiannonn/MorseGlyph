package com.morseglyph

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import com.morseglyph.audio.AudioController
import com.morseglyph.data.SharedPrefsRepository
import com.morseglyph.glyph.GlyphController
import com.morseglyph.ui.MorseScreen
import com.morseglyph.ui.theme.MorseGlyphTheme
import com.morseglyph.viewmodel.MorseViewModel
import com.morseglyph.viewmodel.MorseViewModelFactory

class MainActivity : ComponentActivity() {

    private lateinit var glyphController: GlyphController
    private lateinit var audioController: AudioController

    private val viewModel: MorseViewModel by viewModels {
        MorseViewModelFactory(glyphController, audioController, SharedPrefsRepository(this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        audioController = AudioController()

        glyphController = GlyphController(
            context = this,
            onBindFailed = { viewModel.onGlyphBindFailed() }
        )
        lifecycle.addObserver(glyphController)

        setContent {
            MorseGlyphTheme {
                MorseScreen(viewModel = viewModel)
            }
        }
    }
}
