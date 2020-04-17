package com.ztc1997.k30notificationlight

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.preference.Preference
import android.preference.PreferenceFragment
import android.widget.Toast

const val PREF_ENABLE_DISABLE_SERVICE = "pref_enable_disable_service"
const val PREF_LIGHT_ON_NOTIFICATION = "pref_light_on_notification"
const val PREF_LIGHT_ON_CHARGING = "pref_light_on_charging"
const val PREF_TEST_LIGHT = "pref_test_light"

class SettingsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
    }

    class SettingsFragment : PreferenceFragment() {
        private val lightUtil = LightUtil()

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.pref_settings)

            lightUtil.startShell {
                if (!it) Toast.makeText(
                    context,
                    R.string.toast_root_access_failed,
                    Toast.LENGTH_LONG
                ).show()
            }

            findPreference(PREF_ENABLE_DISABLE_SERVICE).onPreferenceClickListener =
                Preference.OnPreferenceClickListener {
                    Toast.makeText(
                        context,
                        R.string.summary_pref_enable_disable_service,
                        Toast.LENGTH_LONG
                    ).show()
                    startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                    true
                }

            findPreference(PREF_TEST_LIGHT).onPreferenceClickListener =
                Preference.OnPreferenceClickListener {
                    lightUtil.light = true
                    Handler().postDelayed({ lightUtil.light = false }, 1000)
                    true
                }
        }

        override fun onDestroy() {
            super.onDestroy()
            lightUtil.closeShell()
        }
    }
}