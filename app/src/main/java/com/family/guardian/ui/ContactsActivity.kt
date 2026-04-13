package com.family.guardian.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.family.guardian.GuardianApp
import org.json.JSONArray
import org.json.JSONObject

class ContactsActivity : AppCompatActivity() {

    private val prefs by lazy { getSharedPreferences(GuardianApp.PREFS, Context.MODE_PRIVATE) }

    companion object {
        private const val BG_WARM      = 0xFFFFF8F0.toInt()
        private const val TEXT_DARK     = 0xFF2D1A0A.toInt()
        private const val TEXT_SEC      = 0xFF6B5B4F.toInt()
        private const val ACCENT_AMBER  = 0xFFE88C00.toInt()
        private const val BAR_BORDER    = 0xFFE8DDD0.toInt()
        private const val CALL_GREEN    = 0xFF2E7D32.toInt()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        // Blue light + brightness
        val blStrength = prefs.getInt(GuardianApp.KEY_BLUE_LIGHT, GuardianApp.DEFAULT_BLUE_LIGHT)
        if (blStrength > 0) {
            val root = window.decorView as ViewGroup
            val overlay = android.view.View(this).apply {
                val alpha = (blStrength * 2.55f).toInt().coerceIn(0, 180)
                setBackgroundColor(android.graphics.Color.argb(alpha, 255, 160, 30))
                isClickable = false; isFocusable = false
                elevation = 1000f
            }
            root.post {
                root.addView(overlay, ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT))
            }
        }
        val lp = window.attributes
        if (lp.screenBrightness < 0.6f) { lp.screenBrightness = 0.6f; window.attributes = lp }

        setContentView(buildLayout())
    }

    private fun buildLayout(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(BG_WARM)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT)
        }

        // Title bar
        val titleBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(8))
        }

        val backBtn = Button(this).apply {
            text = "\u2190"
            textSize = 24f
            setTextColor(TEXT_DARK)
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            layoutParams = LinearLayout.LayoutParams(dp(56), dp(56))
            setOnClickListener { finish() }
        }
        titleBar.addView(backBtn)

        val spacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
        }
        titleBar.addView(spacer)

        titleBar.addView(TextView(this).apply {
            text = "\uD83D\uDC65 אנשי קשר"
            textSize = 24f
            setTextColor(TEXT_DARK)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })

        root.addView(titleBar)

        // Divider
        root.addView(View(this).apply {
            setBackgroundColor(BAR_BORDER)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1)).apply {
                setMargins(dp(16), 0, dp(16), dp(8))
            }
        })

        // Contact list
        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
            setPadding(dp(12), 0, dp(12), dp(12))
        }

        val listLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        val contacts = loadImportantContacts()
        if (contacts.isEmpty()) {
            listLayout.addView(TextView(this).apply {
                text = "אין אנשי קשר.\nבקש ממנהל להוסיף אנשי קשר בהגדרות."
                textSize = 18f
                setTextColor(TEXT_SEC)
                gravity = Gravity.CENTER
                setPadding(dp(20), dp(60), dp(20), dp(60))
            })
        } else {
            for (contact in contacts) {
                listLayout.addView(makeContactRow(contact.first, contact.second))
            }
        }

        scroll.addView(listLayout)
        root.addView(scroll)
        return root
    }

    private fun makeContactRow(name: String, number: String): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            val bg = GradientDrawable().apply {
                setColor(0xFFFFFFFF.toInt())
                cornerRadius = dp(16).toFloat()
                setStroke(dp(1), BAR_BORDER)
            }
            background = bg
            elevation = dp(2).toFloat()
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(8)
            }
        }

        // Contact info
        val infoLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        infoLayout.addView(TextView(this).apply {
            text = name
            textSize = 22f
            setTextColor(TEXT_DARK)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.END
        })
        infoLayout.addView(TextView(this).apply {
            text = number
            textSize = 16f
            setTextColor(TEXT_SEC)
            gravity = Gravity.END
        })
        row.addView(infoLayout)

        // Call button
        val callBtn = Button(this).apply {
            text = "\uD83D\uDCDE חייג"
            textSize = 20f
            setTextColor(0xFFFFFFFF.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            val bg = GradientDrawable().apply {
                setColor(CALL_GREEN)
                cornerRadius = dp(14).toFloat()
            }
            background = bg
            layoutParams = LinearLayout.LayoutParams(dp(120), dp(64)).apply {
                marginStart = dp(12)
            }
            setOnClickListener {
                makeCall(number)
            }
        }
        row.addView(callBtn)

        return row
    }

    private fun makeCall(number: String) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
            == PackageManager.PERMISSION_GRANTED) {
            try {
                val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(callIntent)
            } catch (_: Exception) {
                startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
            }
        } else {
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
        }
    }

    private fun loadImportantContacts(): List<Pair<String, String>> {
        val json = prefs.getString(GuardianApp.KEY_IMPORTANT_CONTACTS, "") ?: ""
        if (json.isEmpty()) return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                Pair(obj.getString("name"), obj.getString("number"))
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
}
