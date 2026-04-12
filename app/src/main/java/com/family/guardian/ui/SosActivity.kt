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

        val sosNumber = prefs.getString(GuardianApp.KEY_SOS_NUMBER, "") ?: ""
        val sosName = prefs.getString(GuardianApp.KEY_SOS_NAME, "") ?: ""

        // Show contact name
        val tvName = findViewById<TextView>(R.id.tv_sos_contact_name)
        if (sosName.isNotEmpty()) {
            tvName.text = "מתקשר ל: $sosName"
        } else if (sosNumber.isNotEmpty()) {
            tvName.text = "מתקשר ל: $sosNumber"
        }

        btnSos.setOnClickListener {
            if (sosNumber.isEmpty()) {
                Toast.makeText(this,
                    "מספר חירום לא הוגדר",
                    Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            startCountdown(sosNumber)
        }

        btnCancel.setOnClickListener {
            countdownJob?.cancel()
            finish()
        }

        if (sosNumber.isNotEmpty()) {
            startCountdown(sosNumber)
        }
    }

    private fun startCountdown(sosNumber: String) {
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
                executeSos(sosNumber)
            }
        }.start()
    }

    private fun executeSos(number: String) {
        btnCancel.isEnabled = false
        tvStatus.visibility = View.VISIBLE

        // 1. Capture photos (best effort)
        tvStatus.text = "מצלם..."
        Thread {
            var rearFile: java.io.File? = null
            var frontFile: java.io.File? = null
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
                try {
                    val helper = CameraHelper(this)
                    rearFile = helper.capturePhoto(false)
                    frontFile = helper.capturePhoto(true)
                } catch (e: Exception) {
                    android.util.Log.e("SOS", "Camera capture failed: ${e.message}")
                }
            }

            runOnUiThread {
                // 2. Send SMS
                tvStatus.text = "שולח הודעה..."
                try {
                    val sms = SmsManager.getDefault()
                    val msg = "SOS! " +
                        "עזרה נדרשת. " +
                        "הודעה אוטומטית."
                    sms.sendTextMessage(number, null, msg, null, null)
                } catch (e: Exception) {
                    android.util.Log.e("SOS", "SMS failed: ${e.message}")
                }

                // 3. Make the call
                tvStatus.text = "מתקשר..."
                makeCall(number)
            }
        }.start()
    }

    private fun makeCall(number: String) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
            == PackageManager.PERMISSION_GRANTED) {
            try {
                val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(callIntent)
            } catch (e: Exception) {
                android.util.Log.e("SOS", "Call failed: ${e.message}")
                // Fallback to dial
                startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
            }
        } else {
            // Permission denied -- fall back to dial (user must press call)
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
        }
        finish()
    }

    override fun onDestroy() {
        countdownJob?.cancel()
        super.onDestroy()
    }
}
