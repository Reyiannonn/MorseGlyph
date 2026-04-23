package com.morseglyph.data

import android.content.Context
import com.morseglyph.viewmodel.IndicatorMode

class SharedPrefsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("morseglyph_prefs", Context.MODE_PRIVATE)

    fun getWpm(): Int = prefs.getInt("wpm", 15).coerceIn(5, 30)

    fun saveWpm(wpm: Int) {
        prefs.edit().putInt("wpm", wpm.coerceIn(5, 30)).apply()
    }

    fun getIndicatorMode(): IndicatorMode {
        val name = prefs.getString("indicator_mode", IndicatorMode.FULL_STRING.name)
        return runCatching { IndicatorMode.valueOf(name!!) }.getOrDefault(IndicatorMode.FULL_STRING)
    }

    fun saveIndicatorMode(mode: IndicatorMode) {
        prefs.edit().putString("indicator_mode", mode.name).apply()
    }
}
