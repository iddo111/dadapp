package com.family.guardian.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.provider.ContactsContract
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.family.guardian.GuardianApp
import com.family.guardian.R

class AdminActivity : AppCompatActivity() {

    private val prefs by lazy { getSharedPreferences(GuardianApp.PREFS, Context.MODE_PRIVATE) }
    private var pinVerified = false

    companion object {
        private const val PICK_CONTACT_REQ = 2001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)
        showPinDialog()
    }

    private fun showPinDialog() {
        val input = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                        android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = "PIN"
            textSize = 24f
            gravity = Gravity.CENTER
            setPadding(32, 24, 32, 24)
        }
        AlertDialog.Builder(this)
            .setTitle("הגדרות מנהל")
            .setMessage("הכנס קוד גישה")
            .setView(input)
            .setPositiveButton("אישור") { _, _ ->
                val entered = input.text.toString()
                val saved = prefs.getString(GuardianApp.KEY_ADMIN_PIN, GuardianApp.DEFAULT_PIN)
                if (entered == saved) {
                    pinVerified = true
                    loadSettings()
                } else {
                    Toast.makeText(this, "קוד שגוי",
                        Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .setNegativeButton("ביטול") { _, _ -> finish() }
            .setOnCancelListener { finish() }
            .show()
    }

    private fun loadSettings() {
        val root = findViewById<LinearLayout>(R.id.layout_admin_root)
        root.visibility = View.VISIBLE

        // ---- SOS Contact ----
        val tvContact = findViewById<TextView>(R.id.tv_sos_contact_display)
        updateContactDisplay(tvContact)

        findViewById<Button>(R.id.btn_pick_contact).setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK,
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
            @Suppress("DEPRECATION")
            startActivityForResult(intent, PICK_CONTACT_REQ)
        }

        // ---- Touch hold ----
        val sbHold = findViewById<SeekBar>(R.id.sb_hold_duration)
        val tvHold = findViewById<TextView>(R.id.tv_hold_duration)
        val currentHold = prefs.getLong(GuardianApp.KEY_TOUCH_HOLD_MS, GuardianApp.DEFAULT_HOLD_MS)
        sbHold.max = 20
        sbHold.progress = ((currentHold - 200) / 100).toInt().coerceIn(0, 20)
        tvHold.text = "${currentHold}ms"
        sbHold.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, p: Int, u: Boolean) {
                tvHold.text = "${200 + p * 100}ms"
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })

        // ---- App whitelist picker ----
        val tvCount = findViewById<TextView>(R.id.tv_whitelist_count)
        updateWhitelistCount(tvCount)

        findViewById<Button>(R.id.btn_pick_apps).setOnClickListener {
            showAppPickerDialog(tvCount)
        }

        // ---- Blacklist ----
        val etBlacklist = findViewById<EditText>(R.id.et_blacklist)
        etBlacklist.setText(prefs.getString(GuardianApp.KEY_BLACKLIST, ""))

        // ---- Trusted installer ----
        val etInstaller = findViewById<EditText>(R.id.et_trusted_installer)
        etInstaller.setText(prefs.getString(GuardianApp.KEY_TRUSTED_INSTALLER, ""))

        // ---- Blue light ----
        val sbBlue = findViewById<SeekBar>(R.id.sb_blue_light)
        val tvBlue = findViewById<TextView>(R.id.tv_blue_light)
        val currentBlue = prefs.getInt(GuardianApp.KEY_BLUE_LIGHT, GuardianApp.DEFAULT_BLUE_LIGHT)
        sbBlue.max = 100
        sbBlue.progress = currentBlue
        tvBlue.text = "$currentBlue%"
        sbBlue.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, p: Int, u: Boolean) {
                tvBlue.text = "$p%"
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })

        // ---- PIN ----
        val etNewPin = findViewById<EditText>(R.id.et_new_pin)
        etNewPin.setText(prefs.getString(GuardianApp.KEY_ADMIN_PIN, GuardianApp.DEFAULT_PIN))

        // ---- Save ----
        findViewById<Button>(R.id.btn_save_admin).setOnClickListener {
            val holdMs = (200 + sbHold.progress * 100).toLong()
            val newPin = etNewPin.text.toString().trim().ifEmpty { GuardianApp.DEFAULT_PIN }
            prefs.edit()
                .putLong(GuardianApp.KEY_TOUCH_HOLD_MS, holdMs)
                .putString(GuardianApp.KEY_BLACKLIST, etBlacklist.text.toString().trim())
                .putString(GuardianApp.KEY_TRUSTED_INSTALLER, etInstaller.text.toString().trim())
                .putInt(GuardianApp.KEY_BLUE_LIGHT, sbBlue.progress)
                .putString(GuardianApp.KEY_ADMIN_PIN, newPin)
                .putBoolean(GuardianApp.KEY_SETUP_DONE, true)
                .apply()
            Toast.makeText(this, "הגדרות נשמרו",
                Toast.LENGTH_SHORT).show()
            finish()
        }

        // ---- Cancel ----
        findViewById<Button>(R.id.btn_cancel_admin).setOnClickListener { finish() }
    }

    private fun updateContactDisplay(tv: TextView) {
        val name = prefs.getString(GuardianApp.KEY_SOS_NAME, "") ?: ""
        val number = prefs.getString(GuardianApp.KEY_SOS_NUMBER, "") ?: ""
        if (name.isNotEmpty() && number.isNotEmpty()) {
            tv.text = "$name\n$number"
        } else if (number.isNotEmpty()) {
            tv.text = number
        } else {
            tv.text = "לא נבחר"
        }
    }

    private fun updateWhitelistCount(tv: TextView) {
        val whitelist = (prefs.getString(GuardianApp.KEY_WHITELIST, "") ?: "")
            .split(",").map { it.trim() }.filter { it.isNotEmpty() }
        tv.text = "${whitelist.size} אפליקציות נבחרו"
    }

    private fun showAppPickerDialog(tvCount: TextView) {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val apps = pm.queryIntentActivities(intent, 0)
            .map { it.activityInfo }
            .filter { it.packageName != packageName }
            .sortedBy { pm.getApplicationLabel(it.applicationInfo).toString().lowercase() }

        val currentWhitelist = (prefs.getString(GuardianApp.KEY_WHITELIST, "") ?: "")
            .split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableSet()

        val labels = apps.map { pm.getApplicationLabel(it.applicationInfo).toString() }.toTypedArray()
        val pkgs = apps.map { it.packageName }
        val checked = BooleanArray(apps.size) { pkgs[it] in currentWhitelist }

        AlertDialog.Builder(this)
            .setTitle("בחר אפליקציות")
            .setMultiChoiceItems(labels, checked) { _, which, isChecked ->
                checked[which] = isChecked
            }
            .setPositiveButton("שמור") { _, _ ->
                val selected = pkgs.filterIndexed { i, _ -> checked[i] }
                prefs.edit()
                    .putString(GuardianApp.KEY_WHITELIST, selected.joinToString(","))
                    .apply()
                updateWhitelistCount(tvCount)
            }
            .setNegativeButton("ביטול", null)
            .show()
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_CONTACT_REQ && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                var cursor: Cursor? = null
                try {
                    cursor = contentResolver.query(uri, arrayOf(
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                    ), null, null, null)
                    if (cursor != null && cursor.moveToFirst()) {
                        val name = cursor.getString(0) ?: ""
                        val number = cursor.getString(1) ?: ""
                        prefs.edit()
                            .putString(GuardianApp.KEY_SOS_NAME, name)
                            .putString(GuardianApp.KEY_SOS_NUMBER, number)
                            .apply()
                        val tv = findViewById<TextView>(R.id.tv_sos_contact_display)
                        updateContactDisplay(tv)
                    }
                } catch (_: Exception) {
                } finally {
                    cursor?.close()
                }
            }
        }
    }
}
