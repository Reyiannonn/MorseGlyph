package com.morseglyph.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.morseglyph.audio.AudioController
import com.morseglyph.data.SharedPrefsRepository
import com.morseglyph.glyph.GlyphController

class MorseViewModelFactory(
    private val glyphController: GlyphController,
    private val audioController: AudioController,
    private val prefsRepository: SharedPrefsRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        MorseViewModel(glyphController, audioController, prefsRepository) as T
}
