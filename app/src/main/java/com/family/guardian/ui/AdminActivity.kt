package com.family.guardian.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.ContactsContract
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.family.guardian.GuardianApp
import com.family.guardian.R
import org.json.JSONArray
import org.json.JSONObject

class AdminActivity : AppCompatActivity() {

    private val prefs by lazy { getSharedPreferences(GuardianApp.PREFS, Context.MODE_PRIVATE) }
    private var pinVerified = false

    // Track which contact pick is for
    private var pickMode = PickMode.SOS

    private enum class PickMode { SOS, IMPORTANT }

    // In-memory lists
    private val sosContacts = mutableListOf<Pair<String, String>>()
    private val importantContacts = mutableListOf<Pair<String, String>>()

    // UI containers
    private lateinit var sosListContainer: LinearLayout
    private lateinit var importantListContainer: LinearLayout

    companion object {
        private const val PICK_CONTACT_REQ = 2001
        private const val TEXT_DARK = 0xFF2D1A0A.toInt()
        private const val TEXT_SEC = 0xFF6B5B4F.toInt()
        private const val ACCENT_AMBER = 0xFFE88C00.toInt()
        private const val BAR_BORDER = 0xFFE8DDD0.toInt()
        private const val SOS_RED = 0xFFCC2200.toInt()
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

        // Load existing SOS contacts from JSON (or migrate from old single contact)
        loadSosContacts()
        loadImportantContacts()

        // ---- SOS Contacts (multiple) ----
        sosListContainer = findViewById(R.id.layout_sos_contacts_list)
        refreshSosContactsList()

        findViewById<Button>(R.id.btn_add_sos_contact).setOnClickListener {
            if (sosContacts.size >= 5) {
                Toast.makeText(this, "מקסימום 5 אנשי קשר למצוקה", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            pickMode = PickMode.SOS
            val intent = Intent(Intent.ACTION_PICK,
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
            @Suppress("DEPRECATION")
            startActivityForResult(intent, PICK_CONTACT_REQ)
        }

        // ---- Important Contacts (up to 10) ----
        importantListContainer = findViewById(R.id.layout_important_contacts_list)
        refreshImportantContactsList()

        findViewById<Button>(R.id.btn_add_important_contact).setOnClickListener {
            if (importantContacts.size >= 10) {
                Toast.makeText(this, "מקסימום 10 אנשי קשר חשובים", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            pickMode = PickMode.IMPORTANT
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

            // Save SOS contacts as JSON
            val sosJson = JSONArray()
            for ((name, number) in sosContacts) {
                sosJson.put(JSONObject().apply {
                    put("name", name)
                    put("number", number)
                })
            }

            // Save important contacts as JSON
            val impJson = JSONArray()
            for ((name, number) in importantContacts) {
                impJson.put(JSONObject().apply {
                    put("name", name)
                    put("number", number)
                })
            }

            // Also keep backward compat: first SOS contact in old keys
            val firstSos = sosContacts.firstOrNull()
            prefs.edit()
                .putLong(GuardianApp.KEY_TOUCH_HOLD_MS, holdMs)
                .putString(GuardianApp.KEY_BLACKLIST, etBlacklist.text.toString().trim())
                .putString(GuardianApp.KEY_TRUSTED_INSTALLER, etInstaller.text.toString().trim())
                .putInt(GuardianApp.KEY_BLUE_LIGHT, sbBlue.progress)
                .putString(GuardianApp.KEY_ADMIN_PIN, newPin)
                .putBoolean(GuardianApp.KEY_SETUP_DONE, true)
                .putString(GuardianApp.KEY_SOS_CONTACTS, sosJson.toString())
                .putString(GuardianApp.KEY_IMPORTANT_CONTACTS, impJson.toString())
                .putString(GuardianApp.KEY_SOS_NAME, firstSos?.first ?: "")
                .putString(GuardianApp.KEY_SOS_NUMBER, firstSos?.second ?: "")
                .apply()
            Toast.makeText(this, "הגדרות נשמרו",
                Toast.LENGTH_SHORT).show()
            finish()
        }

        // ---- Cancel ----
        findViewById<Button>(R.id.btn_cancel_admin).setOnClickListener { finish() }
    }

    private fun loadSosContacts() {
        sosContacts.clear()
        val json = prefs.getString(GuardianApp.KEY_SOS_CONTACTS, "") ?: ""
        if (json.isNotEmpty()) {
            try {
                val arr = JSONArray(json)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    sosContacts.add(Pair(obj.getString("name"), obj.getString("number")))
                }
            } catch (_: Exception) {}
        }
        // Migration: if empty but old single contact exists, migrate it
        if (sosContacts.isEmpty()) {
            val oldName = prefs.getString(GuardianApp.KEY_SOS_NAME, "") ?: ""
            val oldNumber = prefs.getString(GuardianApp.KEY_SOS_NUMBER, "") ?: ""
            if (oldNumber.isNotEmpty()) {
                sosContacts.add(Pair(oldName, oldNumber))
            }
        }
    }

    private fun loadImportantContacts() {
        importantContacts.clear()
        val json = prefs.getString(GuardianApp.KEY_IMPORTANT_CONTACTS, "") ?: ""
        if (json.isNotEmpty()) {
            try {
                val arr = JSONArray(json)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    importantContacts.add(Pair(obj.getString("name"), obj.getString("number")))
                }
            } catch (_: Exception) {}
        }
    }

    private fun refreshSosContactsList() {
        sosListContainer.removeAllViews()
        if (sosContacts.isEmpty()) {
            sosListContainer.addView(TextView(this).apply {
                text = "לא נבחרו אנשי קשר למצוקה"
                textSize = 16f
                setTextColor(TEXT_SEC)
                gravity = Gravity.END
                setPadding(0, 8, 0, 8)
            })
        } else {
            for (i in sosContacts.indices) {
                sosListContainer.addView(makeContactRow(sosContacts[i].first,
                    sosContacts[i].second, i, true))
            }
        }
    }

    private fun refreshImportantContactsList() {
        importantListContainer.removeAllViews()
        if (importantContacts.isEmpty()) {
            importantListContainer.addView(TextView(this).apply {
                text = "לא נבחרו אנשי קשר חשובים"
                textSize = 16f
                setTextColor(TEXT_SEC)
                gravity = Gravity.END
                setPadding(0, 8, 0, 8)
            })
        } else {
            for (i in importantContacts.indices) {
                importantListContainer.addView(makeContactRow(importantContacts[i].first,
                    importantContacts[i].second, i, false))
            }
        }
    }

    private fun makeContactRow(name: String, number: String, index: Int, isSos: Boolean): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
            val bg = GradientDrawable().apply {
                setColor(0xFFF5EDE0.toInt())
                cornerRadius = dp(10).toFloat()
            }
            background = bg
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(4)
            }
        }

        // Remove button
        val removeBtn = Button(this).apply {
            text = "\u2716"
            textSize = 16f
            setTextColor(SOS_RED)
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
            setOnClickListener {
                if (isSos) {
                    sosContacts.removeAt(index)
                    refreshSosContactsList()
                } else {
                    importantContacts.removeAt(index)
                    refreshImportantContactsList()
                }
            }
        }
        row.addView(removeBtn)

        // Contact info
        val info = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        info.addView(TextView(this).apply {
            text = name
            textSize = 16f
            setTextColor(TEXT_DARK)
            gravity = Gravity.END
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
        info.addView(TextView(this).apply {
            text = number
            textSize = 14f
            setTextColor(TEXT_SEC)
            gravity = Gravity.END
        })
        row.addView(info)

        return row
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
                        when (pickMode) {
                            PickMode.SOS -> {
                                sosContacts.add(Pair(name, number))
                                refreshSosContactsList()
                            }
                            PickMode.IMPORTANT -> {
                                importantContacts.add(Pair(name, number))
                                refreshImportantContactsList()
                            }
                        }
                    }
                } catch (_: Exception) {
                } finally {
                    cursor?.close()
                }
            }
        }
    }

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
}
