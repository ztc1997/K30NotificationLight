package com.ztc1997.k30notificationlight

import android.app.Activity
import android.app.Notification
import android.companion.CompanionDeviceManager
import android.content.*
import android.os.BatteryManager
import android.os.PowerManager
import android.os.Process
import android.preference.PreferenceManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.widget.Toast
import java.util.*

class BackgroundService : NotificationListenerService() {
    private val TAG = "BackgroundService"

    private val lightUtil by lazy {
        LightUtil(
            (getSystemService(Context.POWER_SERVICE) as PowerManager).newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "k30notificationlight:blinkWakeLockTag"
            )
        )
    }
    private val notifications = NotificationSet()
    private val receiver = ChargingScreenReceiver()
    private var isChanging = false
    private var isScreenOn = true
    private lateinit var preferences: SharedPreferences

    private val companionDeviceManager by lazy { getSystemService(Activity.COMPANION_DEVICE_SERVICE) as CompanionDeviceManager }

    override fun onCreate() {
        super.onCreate()
        lightUtil.startShell {
            if (!it) Toast.makeText(
                this,
                R.string.toast_root_access_failed,
                Toast.LENGTH_LONG
            ).show()
        }

        preferences = PreferenceManager.getDefaultSharedPreferences(this)

        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_BATTERY_CHANGED)
        filter.addAction(Intent.ACTION_USER_PRESENT)
        filter.addAction(Intent.ACTION_SCREEN_OFF)
        this.registerReceiver(receiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        this.unregisterReceiver(receiver)
        lightUtil.closeShell()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)

        if (isScreenOn) return

        if (companionDeviceManager.associations.size > 0)
            for (chan in getNotificationChannels(sbn.packageName, Process.myUserHandle())) {
                if (chan.id == sbn.notification.channelId) {
                    if (chan.shouldShowLights()) {
                        notifications.add(Pair(sbn.packageName, sbn.id))
                        return
                    }
                    break
                }
            }

        if (sbn.notification.flags and Notification.FLAG_SHOW_LIGHTS != 0)
            notifications.add(Pair(sbn.packageName, sbn.id))
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
        if (isScreenOn) return
        notifications.remove(Pair(sbn.packageName, sbn.id))
    }

    private fun updateLight() {
        if (!isScreenOn && notifications.size > 0) {
            when (preferences.getString(
                PREF_LIGHT_ON_NOTIFICATION_LIST,
                VALUE_LIGHT_ON_NOTIFICATION_LIST_DISABLED
            )) {
                VALUE_LIGHT_ON_NOTIFICATION_LIST_ALWAYS_ON -> {
                    lightUtil.blink = false
                    lightUtil.setLightAnimated(LIGHT_MAX)
                    return
                }
                VALUE_LIGHT_ON_NOTIFICATION_LIST_BLINK -> {
                    lightUtil.blink = true
                    return
                }
                VALUE_LIGHT_ON_NOTIFICATION_LIST_ALWAYS_ON_BLINK_ON_CHARGE -> {
                    if (isChanging) {
                        lightUtil.blink = true
                    } else {
                        lightUtil.blink = false
                        lightUtil.setLightAnimated(LIGHT_MAX)
                    }
                    return
                }
            }
        }
        lightUtil.blink = false
        if (isChanging && preferences.getBoolean(PREF_LIGHT_ON_CHARGING, true)) {
            lightUtil.setLightAnimated(LIGHT_MAX)
            return
        }
        lightUtil.setLightAnimated(LIGHT_MIN)
    }

    inner class ChargingScreenReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            when (intent.action) {
                Intent.ACTION_BATTERY_CHANGED -> {
                    isChanging = intent.getIntExtra(
                        BatteryManager.EXTRA_STATUS,
                        -1
                    ) == BatteryManager.BATTERY_STATUS_CHARGING && intent.getIntExtra(
                        BatteryManager.EXTRA_LEVEL,
                        -1
                    ) != intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                }
                Intent.ACTION_USER_PRESENT -> isScreenOn = true
                Intent.ACTION_SCREEN_OFF -> if (isScreenOn) {
                    isScreenOn = false
                    notifications.clear()
                }

            }
            updateLight()
        }
    }

    inner class NotificationSet : HashSet<Pair<String, Int>>() {
        override fun add(element: Pair<String, Int>): Boolean {
            val ret = super.add(element)
            if (ret) updateLight()
            return ret
        }

        override fun remove(element: Pair<String, Int>): Boolean {
            val ret = super.remove(element)
            if (ret) updateLight()
            return ret
        }
    }
}