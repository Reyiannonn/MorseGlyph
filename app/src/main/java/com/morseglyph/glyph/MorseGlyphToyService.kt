package com.morseglyph.glyph

import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.util.Log
import com.nothing.ketchum.Common
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphMatrixManager

/**
 * Registers MorseGlyph as a Nothing Glyph Toy.
 *
 * Protocol (from GlyphToy.class in the SDK):
 *   msg.what == 1  (GlyphToy.MSG_GLYPH_TOY)
 *   msg.data.getString("data") == "prepare" | "start" | "end"
 */
class MorseGlyphToyService : Service() {

    private var manager: GlyphMatrixManager? = null
    private var matrixSide = 0

    private val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            if (msg.what == MSG_GLYPH_TOY) {
                when (msg.data?.getString(KEY_DATA)) {
                    STATUS_PREPARE, STATUS_START -> Log.d(TAG, "toy active")
                    STATUS_END -> turnOff()
                    else -> super.handleMessage(msg)
                }
            } else {
                super.handleMessage(msg)
            }
        }
    }

    private val messenger = Messenger(handler)

    override fun onCreate() {
        super.onCreate()
        val len = Common.getDeviceMatrixLength()
        if (len == 0) { Log.w(TAG, "no glyph matrix — toy service inactive"); return }
        matrixSide = len
        try {
            manager = GlyphMatrixManager.getInstance(this)
            manager?.init(object : GlyphMatrixManager.Callback {
                override fun onServiceConnected(name: ComponentName?) {
                    val deviceId = if (matrixSide >= 25) Glyph.DEVICE_23112 else Glyph.DEVICE_25111p
                    runCatching { manager?.register(deviceId) }
                    Log.d(TAG, "toy service connected")
                }
                override fun onServiceDisconnected(name: ComponentName?) {
                    Log.w(TAG, "toy service disconnected")
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "toy service init failed", e)
        }
    }

    override fun onBind(intent: Intent?): IBinder = messenger.binder

    override fun onDestroy() {
        turnOff()
        runCatching { manager?.unInit() }
        manager = null
        super.onDestroy()
    }

    private fun turnOff() {
        val size = matrixSide * matrixSide
        if (size > 0) runCatching { manager?.setMatrixFrame(IntArray(size) { 0 }) }
    }

    companion object {
        private const val TAG = "MorseGlyphToyService"
        private const val MSG_GLYPH_TOY = 1
        private const val KEY_DATA = "data"
        private const val STATUS_PREPARE = "prepare"
        private const val STATUS_START = "start"
        private const val STATUS_END = "end"
    }
}
