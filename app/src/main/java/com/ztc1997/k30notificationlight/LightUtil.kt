package com.ztc1997.k30notificationlight

import eu.chainfire.libsuperuser.Shell

private const val LIGHT_MAX = 4
private const val LIGHT_MIN = 0
private const val SYS_LIGHT_INTERFACE = "/sys/class/leds/white/brightness"

class LightUtil {
    private var rootSession: Shell.Interactive? = null

    val isRunning: Boolean
        get() {
            rootSession?.let { return it.isRunning }
            return false
        }

    var light: Boolean = false
        set(value) {
            if (value == light) return
            field = value
            val cmd = "echo ${if (value) LIGHT_MAX else LIGHT_MIN} > $SYS_LIGHT_INTERFACE"
            rootSession!!.addCommand(cmd)
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
        if (!isRunning) return
        light = false
        rootSession?.closeWhenIdle()
    }
}