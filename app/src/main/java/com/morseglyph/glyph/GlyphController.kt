package com.morseglyph.glyph

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.morseglyph.morse.MorseSymbol
import com.nothing.ketchum.Common
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphMatrixManager
import kotlinx.coroutines.delay

class GlyphController(
    private val context: Context,
    private val onBindFailed: () -> Unit
) : DefaultLifecycleObserver {

    @Volatile
    var available = false
        private set

    private var manager: GlyphMatrixManager? = null
    private var matrixSide = 0
    private var matrixSize = 0

    // Pre-allocated once — zero GC pressure during playback
    private var dotFrame = EMPTY
    private var dashFrame = EMPTY
    private var onFrame = EMPTY
    private var offFrame = EMPTY

    // Single-slot letter cache: same letter reuses the same array
    private var cachedLetterKey: List<MorseSymbol>? = null
    private var cachedLetterFrame = EMPTY

    // onCreate/onDestroy so screen-off (onStop) does NOT kill the glyph service
    override fun onCreate(owner: LifecycleOwner) {
        val len = Common.getDeviceMatrixLength()
        Log.d(TAG, "MODEL=${Build.MODEL} matrixLen=$len")
        if (len == 0) { onBindFailed(); return }
        matrixSide = len
        matrixSize = len * len
        dotFrame = dotMatrix(len)
        dashFrame = dashMatrix(len)
        onFrame = IntArray(matrixSize) { 4095 }
        offFrame = IntArray(matrixSize) { 0 }
        try {
            manager = GlyphMatrixManager.getInstance(context)
            manager?.init(object : GlyphMatrixManager.Callback {
                override fun onServiceConnected(name: ComponentName?) {
                    try {
                        val deviceId = if (matrixSide >= 25) Glyph.DEVICE_23112 else Glyph.DEVICE_25111p
                        manager?.register(deviceId)
                        Log.d(TAG, "service connected, registered as $deviceId")
                        available = true
                    } catch (e: Exception) {
                        Log.e(TAG, "registration failed", e)
                        onBindFailed()
                    }
                }
                override fun onServiceDisconnected(name: ComponentName?) {
                    Log.w(TAG, "service disconnected")
                    available = false
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "init failed", e)
            onBindFailed()
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        available = false
        try { manager?.closeAppMatrix() } catch (_: Exception) {}
        try { manager?.unInit() } catch (_: Exception) {}
        manager = null
        cachedLetterKey = null
        cachedLetterFrame = EMPTY
    }

    suspend fun flashOn(
        durationMs: Long,
        symbolType: MorseSymbol? = null,
        letterSymbols: List<MorseSymbol>? = null
    ) {
        if (!available) { delay(durationMs); return }
        try {
            val frame = when {
                letterSymbols != null -> cachedLetter(letterSymbols)
                symbolType == MorseSymbol.DOT -> dotFrame
                symbolType == MorseSymbol.DASH -> dashFrame
                else -> onFrame
            }
            manager?.setAppMatrixFrame(frame)
        } catch (e: Exception) {
            // Transient IPC error — log and continue; onServiceDisconnected sets available=false
            Log.e(TAG, "flashOn failed", e)
        }
        delay(durationMs)
    }

    suspend fun flashOff(durationMs: Long) {
        if (!available) { delay(durationMs); return }
        try {
            manager?.setAppMatrixFrame(offFrame)
        } catch (e: Exception) {
            Log.e(TAG, "flashOff failed", e)
        }
        delay(durationMs)
    }

    fun turnOffImmediate() {
        if (!available) return
        try { manager?.setAppMatrixFrame(offFrame) } catch (_: Exception) {}
        try { manager?.closeAppMatrix() } catch (_: Exception) {}
    }

    private fun cachedLetter(symbols: List<MorseSymbol>): IntArray {
        if (symbols != cachedLetterKey) {
            cachedLetterKey = symbols
            cachedLetterFrame = letterMatrix(symbols, matrixSide)
        }
        return cachedLetterFrame
    }

    companion object {
        private const val TAG = "GlyphController"
        private val EMPTY = IntArray(0)

        private fun dotMatrix(side: Int): IntArray {
            val mid = side / 2
            val r2 = (side * side) / 25
            return IntArray(side * side) { idx ->
                val dr = idx / side - mid
                val dc = idx % side - mid
                if (dr * dr + dc * dc <= r2) 4095 else 0
            }
        }

        private fun dashMatrix(side: Int): IntArray {
            val mid = side / 2
            val halfH = (side / 10).coerceAtLeast(1)
            val margin = side / 8
            return IntArray(side * side) { idx ->
                val row = idx / side
                val col = idx % side
                if (row in (mid - halfH)..(mid + halfH) && col in margin..(side - 1 - margin)) 4095 else 0
            }
        }

        fun letterMatrix(symbols: List<MorseSymbol>, side: Int): IntArray {
            if (symbols.isEmpty()) return IntArray(side * side) { 4095 }
            val frame = IntArray(side * side) { 0 }
            val n = symbols.size
            val midRow = side / 2
            val cellW = side / n
            val dotR2 = ((cellW / 4).coerceAtLeast(1)).let { it * it }
            val dashHalfH = (side / 10).coerceAtLeast(1)
            val dashMargin = (cellW / 8).coerceAtLeast(0)
            for (i in 0 until n) {
                val cellStart = i * cellW
                val cellMidCol = cellStart + cellW / 2
                when (symbols[i]) {
                    MorseSymbol.DOT -> {
                        for (r in 0 until side) {
                            for (c in cellStart until minOf(cellStart + cellW, side)) {
                                val dr = r - midRow; val dc = c - cellMidCol
                                if (dr * dr + dc * dc <= dotR2) frame[r * side + c] = 4095
                            }
                        }
                    }
                    MorseSymbol.DASH -> {
                        for (r in (midRow - dashHalfH)..(midRow + dashHalfH)) {
                            for (c in (cellStart + dashMargin) until minOf(cellStart + cellW - dashMargin, side)) {
                                if (r in 0 until side) frame[r * side + c] = 4095
                            }
                        }
                    }
                }
            }
            return frame
        }
    }
}
