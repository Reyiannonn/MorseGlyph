package com.morseglyph.glyph

import android.content.Context
import android.os.Build
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.delay

class GlyphController(
    private val context: Context,
    private val onBindFailed: () -> Unit
) : DefaultLifecycleObserver {

    var available = false
        private set

    // Holds the GlyphManager instance via reflection so we compile without the AAR
    private var glyphManager: Any? = null

    override fun onStart(owner: LifecycleOwner) {
        if (!isNothingPhone3()) {
            onBindFailed()
            return
        }
        try {
            val managerClass = Class.forName("com.nothing.ketchum.GlyphManager")
            val getInstanceMethod = managerClass.getMethod("getInstance", Context::class.java)
            glyphManager = getInstanceMethod.invoke(null, context)

            val callbackClass = Class.forName("com.nothing.ketchum.GlyphManager\$Callback")
            val proxy = java.lang.reflect.Proxy.newProxyInstance(
                callbackClass.classLoader,
                arrayOf(callbackClass)
            ) { _, method, args ->
                when (method.name) {
                    "onServiceConnected" -> {
                        try {
                            val mgr = args?.get(0)
                            mgr?.javaClass?.getMethod("openSession")?.invoke(mgr)
                            available = true
                        } catch (_: Exception) {}
                    }
                    "onServiceDisconnected" -> available = false
                }
                null
            }

            glyphManager?.javaClass?.getMethod("init", callbackClass)?.invoke(glyphManager, proxy)
        } catch (e: Exception) {
            available = false
            onBindFailed()
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        turnOffImmediate()
        try {
            glyphManager?.javaClass?.getMethod("closeSession")?.invoke(glyphManager)
        } catch (_: Exception) {}
        glyphManager = null
        available = false
    }

    suspend fun flashOn(durationMs: Long) {
        if (!available) { delay(durationMs); return }
        try { setAllChannels(4095) } catch (e: Exception) {
            available = false
            onBindFailed()
        }
        delay(durationMs)
    }

    suspend fun flashOff(durationMs: Long) {
        if (!available) { delay(durationMs); return }
        try { setAllChannels(0) } catch (_: Exception) {}
        delay(durationMs)
    }

    fun turnOffImmediate() {
        if (!available) return
        try { setAllChannels(0) } catch (_: Exception) {}
    }

    private fun setAllChannels(brightness: Int) {
        val mgr = glyphManager ?: return
        try {
            val builderMethod = mgr.javaClass.getMethod("getGlyphFrameBuilder")
            val builder = builderMethod.invoke(mgr) ?: return
            for (i in 0 until 25) {
                try {
                    builder.javaClass.getMethod("buildChannel", Int::class.java, Int::class.java)
                        .invoke(builder, i, brightness)
                } catch (_: Exception) {}
            }
            val frame = builder.javaClass.getMethod("build").invoke(builder)
            mgr.javaClass.getMethod("toggle", frame!!.javaClass).invoke(mgr, frame)
        } catch (_: Exception) {}
    }

    private fun isNothingPhone3(): Boolean =
        Build.MANUFACTURER.equals("Nothing", ignoreCase = true) &&
        (Build.MODEL.contains("A059", ignoreCase = true) ||
         Build.MODEL.contains("Phone (3)", ignoreCase = true))
}
