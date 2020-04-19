package com.ztc1997.k30notificationlight

import android.app.Activity
import android.app.AlertDialog
import android.companion.AssociationRequest
import android.companion.CompanionDeviceManager
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentSender
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
    private val companionDeviceManager by lazy { getSystemService(COMPANION_DEVICE_SERVICE) as CompanionDeviceManager }
    private val handler by lazy { Handler() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
    }

    override fun onStart() {
        super.onStart()
        if (companionDeviceManager.associations.size == 0) {
            AlertDialog.Builder(this)
                .setTitle(R.string.dialog_title_companion_device_association)
                .setMessage(R.string.dialog_message_companion_device_association)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    companionDeviceManager.associate(AssociationRequest.Builder().build(), object :
                        CompanionDeviceManager.Callback() {
                        override fun onFailure(error: CharSequence?) {
                            Toast.makeText(
                                this@SettingsActivity,
                                R.string.toast_companion_device_association_failed,
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        override fun onDeviceFound(chooserLauncher: IntentSender?) {
                            try {
                                startIntentSenderForResult(chooserLauncher, 0, null, 0, 0, 0)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }, handler)
                }.show()
        }
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