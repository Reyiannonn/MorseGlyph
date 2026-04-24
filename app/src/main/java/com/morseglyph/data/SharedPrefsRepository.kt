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

    fun getHistory(): List<String> {
        val raw = prefs.getStringSet("history", emptySet()) ?: emptySet()
        val indexed = raw.mapNotNull { entry ->
            val sep = entry.indexOf('|')
            if (sep < 0) null else entry.substring(0, sep).toIntOrNull()?.let { it to entry.substring(sep + 1) }
        }
        return indexed.sortedByDescending { it.first }.map { it.second }
    }

    fun addToHistory(message: String) {
        if (message.isBlank()) return
        val current = getHistory().toMutableList()
        current.remove(message)
        current.add(0, message)
        val trimmed = current.take(10)
        val raw = trimmed.mapIndexed { i, s -> "${trimmed.size - 1 - i}|$s" }.toSet()
        prefs.edit().putStringSet("history", raw).apply()
    }

    fun isFirstLaunch(): Boolean = prefs.getBoolean("first_launch", true)

    fun markOnboarded() {
        prefs.edit().putBoolean("first_launch", false).apply()
    }
}
