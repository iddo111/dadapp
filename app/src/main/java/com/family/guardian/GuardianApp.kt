package com.family.guardian

import android.app.*
import android.content.Context

class GuardianApp : Application() {
    companion object {
        const val CHANNEL_GUARDIAN = "guardian_service"
        const val PREFS = "guardian_prefs"
        const val KEY_ADMIN_PIN = "admin_pin"
        const val KEY_TRUSTED_INSTALLER = "trusted_installer_pkg"
        const val KEY_WHITELIST = "app_whitelist"
        const val KEY_BLACKLIST = "app_blacklist"
        const val KEY_SOS_NUMBER = "sos_number"
        const val KEY_SOS_NAME = "sos_name"
        const val KEY_SOS_CONTACTS = "sos_contacts_json"
        const val KEY_IMPORTANT_CONTACTS = "important_contacts_json"
        const val KEY_TOUCH_HOLD_MS = "touch_hold_ms"
        const val KEY_SETUP_DONE = "setup_done"
        const val KEY_BLUE_LIGHT = "blue_light_strength"
        const val KEY_FAMILY_CALL_URL = "family_call_url"
        // Phone number (E.164 or local) the "Family Video Call" tile should
        // call via WhatsApp. If unset, the launcher falls back to the first
        // Important Contact, then the SOS number.
        const val KEY_FAMILY_CALL_PHONE = "family_call_phone"
        const val DEFAULT_PIN = "1234"
        const val DEFAULT_HOLD_MS = 800L
        const val DEFAULT_BLUE_LIGHT = 40
    }
    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_GUARDIAN, "Guardian Service",
                NotificationManager.IMPORTANCE_LOW).apply {
                setShowBadge(false)
            }
        )
        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_ADMIN_PIN)) {
            prefs.edit()
                .putString(KEY_ADMIN_PIN, DEFAULT_PIN)
                .putString(KEY_SOS_NUMBER, "")
                .putString(KEY_SOS_NAME, "")
                .putString(KEY_BLACKLIST, "")
                .putLong(KEY_TOUCH_HOLD_MS, DEFAULT_HOLD_MS)
                .putInt(KEY_BLUE_LIGHT, DEFAULT_BLUE_LIGHT)
                .putBoolean(KEY_SETUP_DONE, false)
                .putString(KEY_WHITELIST,
                    "com.samsung.android.dialer," +
                    "com.samsung.android.messaging," +
                    "com.whatsapp," +
                    "com.sec.android.app.camera," +
                    "com.sec.android.gallery3d," +
                    "com.google.android.dialer," +
                    "com.google.android.mms," +
                    "com.android.dialer," +
                    "com.android.mms")
                .apply()
        }
    }
}
