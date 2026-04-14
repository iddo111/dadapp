package com.family.guardian.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.telephony.SmsManager
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.family.guardian.GuardianApp
import com.family.guardian.R
import com.family.guardian.util.CameraHelper
import org.json.JSONArray

class SosActivity : AppCompatActivity() {

    private val prefs by lazy { getSharedPreferences(GuardianApp.PREFS, Context.MODE_PRIVATE) }
    private var countdownJob: CountDownTimer? = null
    private lateinit var tvCountdown: TextView
    private lateinit var tvStatus: TextView
    private lateinit var btnSos: Button
    private lateinit var btnCancel: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true); setTurnScreenOn(true)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
        // Blue light + brightness
        val blStrength = prefs.getInt(GuardianApp.KEY_BLUE_LIGHT, GuardianApp.DEFAULT_BLUE_LIGHT)
        if (blStrength > 0) {
            val root = window.decorView as android.view.ViewGroup
            val overlay = View(this).apply {
                val alpha = (blStrength * 2.55f).toInt().coerceIn(0, 180)
                setBackgroundColor(android.graphics.Color.argb(alpha, 255, 160, 30))
                isClickable = false; isFocusable = false
                elevation = 1000f
            }
            root.post {
                root.addView(overlay, android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT))
            }
        }
        val lp = window.attributes
        if (lp.screenBrightness < 0.6f) { lp.screenBrightness = 0.6f; window.attributes = lp }

        setContentView(R.layout.activity_sos)

        tvCountdown = findViewById(R.id.tv_sos_countdown)
        tvStatus = findViewById(R.id.tv_sos_status)
        btnSos = findViewById(R.id.btn_sos_confirm)
        btnCancel = findViewById(R.id.btn_sos_cancel)

        val sosContacts = loadSosContacts()

        // Show contact names
        val tvName = findViewById<TextView>(R.id.tv_sos_contact_name)
        if (sosContacts.isNotEmpty()) {
            val names = sosContacts.joinToString(", ") { it.first.ifEmpty { it.second } }
            tvName.text = "מתקשר ל: $names"
        } else {
            tvName.text = "לא הוגדרו אנשי קשר למצוקה"
        }

        btnSos.setOnClickListener {
            if (sosContacts.isEmpty()) {
                Toast.makeText(this,
                    "מספר חירום לא הוגדר",
                    Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            startCountdown(sosContacts)
        }

        btnCancel.setOnClickListener {
            countdownJob?.cancel()
            finish()
        }

        if (sosContacts.isNotEmpty()) {
            startCountdown(sosContacts)
        }
    }

    private fun loadSosContacts(): List<Pair<String, String>> {
        // Try new JSON format first
        val json = prefs.getString(GuardianApp.KEY_SOS_CONTACTS, "") ?: ""
        if (json.isNotEmpty()) {
            try {
                val arr = JSONArray(json)
                val result = mutableListOf<Pair<String, String>>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    result.add(Pair(obj.getString("name"), obj.getString("number")))
                }
                if (result.isNotEmpty()) return result
            } catch (_: Exception) {}
        }
        // Fallback to old single contact
        val oldNumber = prefs.getString(GuardianApp.KEY_SOS_NUMBER, "") ?: ""
        val oldName = prefs.getString(GuardianApp.KEY_SOS_NAME, "") ?: ""
        return if (oldNumber.isNotEmpty()) listOf(Pair(oldName, oldNumber)) else emptyList()
    }

    private fun startCountdown(contacts: List<Pair<String, String>>) {
        tvCountdown.visibility = View.VISIBLE
        btnSos.isEnabled = false
        countdownJob?.cancel()
        countdownJob = object : CountDownTimer(3000, 1000) {
            override fun onTick(remaining: Long) {
                val sec = ((remaining / 1000) + 1).toInt()
                tvCountdown.text = sec.toString()
            }
            override fun onFinish() {
                tvCountdown.text = "!"
                executeSos(contacts)
            }
        }.start()
    }

    private fun executeSos(contacts: List<Pair<String, String>>) {
        btnCancel.isEnabled = false
        tvStatus.visibility = View.VISIBLE

        // 1. CALL FIRST - most important, do it immediately
        tvStatus.text = "מתקשר..."
        val firstNumber = contacts.first().second
        makeCall(firstNumber)

        // 2. Send SMS to ALL contacts in background
        Thread {
            for ((name, number) in contacts) {
                try {
                    val sms = SmsManager.getDefault()
                    val msg = "SOS! " +
                        "עזרה נדרשת. " +
                        "הודעה אוטומטית."
                    sms.sendTextMessage(number, null, msg, null, null)
                } catch (e: Exception) {
                    android.util.Log.e("SOS", "SMS failed to $number: ${e.message}")
                }
            }

            // 3. Capture photos (best effort, after call started)
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
                try {
                    val helper = CameraHelper(this)
                    helper.capturePhoto(false)
                    helper.capturePhoto(true)
                } catch (e: Exception) {
                    android.util.Log.e("SOS", "Camera capture failed: ${e.message}")
                }
            }
        }.start()
    }

    private fun makeCall(number: String) {
        val cleanNumber = number.replace("[^0-9+]".toRegex(), "")
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
            == PackageManager.PERMISSION_GRANTED) {
            try {
                val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$cleanNumber")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(callIntent)
            } catch (e: Exception) {
                android.util.Log.e("SOS", "Call failed: ${e.message}")
                startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNumber")))
            }
        } else {
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNumber")))
        }
        // DON'T finish() - let the call screen take over naturally
    }

    override fun onDestroy() {
        countdownJob?.cancel()
        super.onDestroy()
    }
}
