package com.ztc1997.k30notificationlight

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.os.Handler
import android.os.PowerManager
import eu.chainfire.libsuperuser.Shell
import kotlin.math.abs

const val LIGHT_MAX = 4
const val LIGHT_MIN = 0
private const val SYS_LIGHT_INTERFACE = "/sys/class/leds/white/brightness"

class LightUtil(private val wakeLock: PowerManager.WakeLock?) {
    private val handler = Handler()
    private val blinkRunnable = object : Runnable {
        override fun run() {
            toggleLightAnimated()
            handler.postDelayed(this, 1500)
        }
    }

    private var rootSession: Shell.Interactive? = null
    private var lastValueAnimator: ValueAnimator? = null

    var blink = false
        @SuppressLint("WakelockTimeout")
        set(value) {
            if (value == field) return
            field = value
            if (value) {
                wakeLock?.acquire()
                handler.post(blinkRunnable)
            } else {
                handler.removeCallbacks(blinkRunnable)
                setLightAnimated(LIGHT_MIN)
                wakeLock?.release()
            }
        }

    val isRunning: Boolean
        get() {
            rootSession?.let { return it.isRunning }
            return false
        }

    var light: Int = LIGHT_MIN
        set(value) {
            if (value == light) return
            val cmd = "echo $value > $SYS_LIGHT_INTERFACE"
            rootSession!!.addCommand(cmd)
            field = value
        }

    fun setLightAnimated(value: Int) {
        lastValueAnimator?.cancel()

        val valueAnimator = ValueAnimator.ofInt(light, value)
        lastValueAnimator = valueAnimator

        valueAnimator.addUpdateListener { light = it.animatedValue as Int }
        valueAnimator.duration = (abs(light - value) * 100).toLong()
        valueAnimator.start()
    }

    fun toggleLightAnimated() {
        setLightAnimated(if (light <= LIGHT_MIN) LIGHT_MAX else LIGHT_MIN)
    }

    fun startShell(onFinished: ((success: Boolean) -> Unit)?) {
        if (isRunning) {
            onFinished?.let { it(true) }
            return
        }
        rootSession = Shell.Builder()
            .useSU()
            .open { success, _ ->
                onFinished?.let { it(success) }
            }
    }

    fun closeShell() {
        blink = false
        lastValueAnimator?.cancel()
        light = LIGHT_MIN
        rootSession?.closeWhenIdle()
    }
}