package com.family.guardian.ui

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.GradientDrawable
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.*
import android.provider.ContactsContract
import android.provider.Settings
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.family.guardian.GuardianApp
import com.family.guardian.service.GuardianNotificationService
import java.util.Calendar

class LauncherActivity : AppCompatActivity() {

    private val prefs by lazy { getSharedPreferences(GuardianApp.PREFS, Context.MODE_PRIVATE) }
    private lateinit var grid: GridLayout
    private lateinit var tvTime: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvBattery: TextView
    private lateinit var btnFlash: Button
    private val handler = Handler(Looper.getMainLooper())

    // Flashlight state
    private var flashlightOn = false

    // Blue light filter overlay
    private var blueLightOverlay: View? = null

    companion object {
        private const val PERM_REQ = 1001
        // Colors
        private const val BG_WARM      = 0xFFFFF8F0.toInt()
        private const val TEXT_DARK     = 0xFF2D1A0A.toInt()
        private const val TEXT_SEC      = 0xFF6B5B4F.toInt()
        private const val ACCENT_AMBER  = 0xFFE88C00.toInt()
        private const val CARD_WHITE    = 0xFFFFFFFF.toInt()
        private const val BAR_BG        = 0xFFFFF8F0.toInt()
        private const val BAR_BORDER    = 0xFFE8DDD0.toInt()
        private const val SOS_RED       = 0xFFCC2200.toInt()
    }

    private val hebrewDays = arrayOf(
        "ראשון",   // Sunday
        "שני",               // Monday
        "שלישי",   // Tuesday
        "רביעי",   // Wednesday
        "חמישי",   // Thursday
        "שישי",         // Friday
        "שבת"                // Saturday
    )

    private val hebrewMonths = arrayOf(
        "ינואר","פברואר","מרץ","אפריל","מאי","יוני",
        "יולי","אוגוסט","ספטמבר","אוקטובר","נובמבר","דצמבר"
    )

    private fun timeOfDayGreeting(): String {
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (h) {
            in 5..11  -> "בוקר טוב אבא"
            in 12..16 -> "צהריים טובים אבא"
            in 17..20 -> "ערב טוב אבא"
            else      -> "לילה טוב אבא"
        }
    }

    private lateinit var tvGreeting: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        supportActionBar?.hide()
        setContentView(buildLayout())
        updateClock()
        startClockTick()
        registerBatteryReceiver()
        buildAppGrid()
        startGuardianService()
        requestAllPermissions()
    }

    override fun onResume() {
        super.onResume()
        buildAppGrid()
        enforceBrightness()
        applyBlueLightFilter()
    }

    // ---- Brightness floor ----
    private fun enforceBrightness() {
        val lp = window.attributes
        if (lp.screenBrightness < 0.6f) {
            lp.screenBrightness = 0.6f
            window.attributes = lp
        }
    }

    // ---- Blue light filter ----
    // IMPORTANT: touch pass-through is non-trivial here. The overlay sits in
    // decorView above all tiles; on Samsung One UI an elevated opaque view
    // can swallow touches even with isClickable=false (the RenderNode outline
    // becomes a hit region). We explicitly consume nothing via an empty
    // touch-listener that returns false, and drop the extreme elevation.
    private fun applyBlueLightFilter() {
        val strength = prefs.getInt(GuardianApp.KEY_BLUE_LIGHT, GuardianApp.DEFAULT_BLUE_LIGHT)
        if (strength <= 0) {
            blueLightOverlay?.visibility = View.GONE
            return
        }
        if (blueLightOverlay == null) {
            blueLightOverlay = View(this)
            val root = window.decorView as ViewGroup
            root.addView(blueLightOverlay, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT))
        }
        // Warm orange overlay
        val alpha = (strength * 2.55f).toInt().coerceIn(0, 180)
        blueLightOverlay?.apply {
            setBackgroundColor(Color.argb(alpha, 255, 160, 30))
            visibility = View.VISIBLE
            isClickable = false
            isFocusable = false
            isLongClickable = false
            // Do NOT raise elevation — forces Samsung's touch dispatcher to
            // pick the overlay as a hit-target even with isClickable=false.
            elevation = 0f
            // Explicitly return false from touch dispatch so every DOWN/MOVE
            // falls through to tiles underneath.
            setOnTouchListener { _, _ -> false }
            // Keep screen-readers from catching this decorative layer.
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
    }

    // ---- Permissions ----
    private fun requestAllPermissions() {
        val needed = mutableListOf<String>()
        val perms = arrayOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.SEND_SMS,
            Manifest.permission.CAMERA,
            Manifest.permission.READ_CONTACTS
        )
        for (p in perms) {
            if (ContextCompat.checkSelfPermission(this, p)
                != PackageManager.PERMISSION_GRANTED) {
                needed.add(p)
            }
        }
        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), PERM_REQ)
        }
        // Battery optimization exemption
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(PowerManager::class.java)
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:$packageName")))
                } catch (_: Exception) {}
            }
        }
        // Overlay permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            try {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")))
            } catch (_: Exception) {}
        }
    }

    private fun startGuardianService() {
        try {
            startForegroundService(Intent(this, GuardianNotificationService::class.java))
        } catch (_: Exception) {}
    }

    // ---- Layout ----
    private fun buildLayout(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            // Cheerful multi-color gradient — sunrise sky
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(
                    0xFFFFE4B5.toInt(),   // sunny yellow
                    0xFFFFD8A8.toInt(),   // peach
                    0xFFFFBF9B.toInt(),   // coral
                    0xFFFFE0EC.toInt(),   // pink mist
                    0xFFE0F0FF.toInt()    // sky blue
                )
            )
            layoutParams = ViewGroup.LayoutParams(MATCH, MATCH)
        }

        // ---- Decorative banner: cheerful drawing strip ----
        root.addView(buildDecorBanner())

        // ---- Top CARD: greeting + clock + battery + date ----
        val topCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val bg = GradientDrawable().apply {
                setColor(0xFFFFFFFF.toInt())
                cornerRadius = dp(24).toFloat()
                setStroke(dp(1), 0xFFF0E4D0.toInt())
            }
            background = bg
            elevation = dp(6).toFloat()
            setPadding(dp(22), dp(18), dp(22), dp(18))
            layoutParams = LinearLayout.LayoutParams(MATCH, WRAP).apply {
                setMargins(dp(14), dp(16), dp(14), dp(8))
            }
        }
        // Greeting row with heart
        val greetingRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(MATCH, WRAP)
        }
        greetingRow.addView(TextView(this).apply {
            text = "❤️"
            textSize = 26f
            layoutParams = LinearLayout.LayoutParams(WRAP, WRAP)
        })
        tvGreeting = TextView(this).apply {
            text = timeOfDayGreeting()
            textSize = 30f
            setTextColor(TEXT_DARK)
            gravity = Gravity.END
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, WRAP, 1f)
        }
        greetingRow.addView(tvGreeting)
        topCard.addView(greetingRow)

        // Clock row: big time on right, battery chip on left
        val clockRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(6), 0, 0)
        }
        tvBattery = TextView(this).apply {
            textSize = 15f
            setTextColor(TEXT_SEC)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            val chipBg = GradientDrawable().apply {
                setColor(0xFFF6EEDD.toInt())
                cornerRadius = dp(14).toFloat()
            }
            background = chipBg
            setPadding(dp(14), dp(8), dp(14), dp(8))
            layoutParams = LinearLayout.LayoutParams(WRAP, WRAP)
        }
        tvTime = TextView(this).apply {
            textSize = 58f
            setTextColor(TEXT_DARK)
            typeface = android.graphics.Typeface.MONOSPACE
            setShadowLayer(dp(2).toFloat(), 0f, dp(1).toFloat(), 0x22000000)
            maxLines = 1
            isSingleLine = true
            includeFontPadding = false
            layoutParams = LinearLayout.LayoutParams(0, WRAP, 1f)
            gravity = Gravity.END
        }
        clockRow.addView(tvBattery)
        clockRow.addView(tvTime)
        topCard.addView(clockRow)

        // Date
        tvDate = TextView(this).apply {
            textSize = 18f
            setTextColor(TEXT_SEC)
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(MATCH, WRAP)
        }
        topCard.addView(tvDate)
        root.addView(topCard)

        // ---- App grid ----
        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH, 0, 1f)
            setPadding(dp(8), dp(4), dp(8), dp(8))
            isVerticalScrollBarEnabled = false
        }
        grid = GridLayout(this).apply {
            columnCount = 2
            layoutParams = ViewGroup.LayoutParams(MATCH, WRAP)
        }
        scroll.addView(grid)
        root.addView(scroll)

        // ---- Bottom CARD ----
        val bottomOuter = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val bg = GradientDrawable().apply {
                setColor(0xFFFFFFFF.toInt())
                cornerRadius = dp(24).toFloat()
                setStroke(dp(1), 0xFFF0E4D0.toInt())
            }
            background = bg
            elevation = dp(8).toFloat()
            layoutParams = LinearLayout.LayoutParams(MATCH, WRAP).apply {
                setMargins(dp(14), dp(4), dp(14), dp(16))
            }
        }
        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))
        }

        // Flashlight button - warm amber gradient, rounded, big
        btnFlash = Button(this).apply {
            text = "\uD83D\uDD26\nפנס"
            textSize = 18f
            setTextColor(TEXT_DARK)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            val bg = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(0xFFFFC947.toInt(), 0xFFFFA726.toInt())
            ).apply {
                cornerRadius = dp(20).toFloat()
                setStroke(dp(2), 0xFFE88C00.toInt())
            }
            background = bg
            elevation = dp(3).toFloat()
            stateListAnimator = null
            layoutParams = LinearLayout.LayoutParams(dp(110), dp(88)).apply {
                marginEnd = dp(10)
            }
        }
        // Short 250 ms hold so a pocket-brush won't fire it, but a
        // normal press still feels snappy. See attachHoldListener doc.
        attachHoldListener(btnFlash, 250L) { toggleFlashlight() }
        bottom.addView(btnFlash)

        // SOS button - bold red gradient, larger, attention-grabbing
        val sosBtn = Button(this).apply {
            text = "🆘 SOS"
            textSize = 26f
            setTextColor(0xFFFFFFFF.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            val bg = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(0xFFE53935.toInt(), 0xFFB71C1C.toInt())
            ).apply {
                cornerRadius = dp(20).toFloat()
                setStroke(dp(3), 0xFFFFD54F.toInt())
            }
            background = bg
            elevation = dp(6).toFloat()
            stateListAnimator = null
            setShadowLayer(dp(2).toFloat(), 0f, dp(1).toFloat(), 0x66000000)
            layoutParams = LinearLayout.LayoutParams(0, dp(88), 1f).apply {
                marginEnd = dp(10)
            }
        }
        // SOS also gets the 250 ms hold so a lap-bump doesn't accidentally
        // trigger an emergency call. SOS itself then has its OWN 3 s
        // countdown with cancel button inside SosActivity, so two gates.
        attachHoldListener(sosBtn, 250L) {
            startActivity(Intent(this@LauncherActivity, SosActivity::class.java))
        }
        bottom.addView(sosBtn)
        // Subtle pulse animation to draw eye to SOS without being annoying
        sosBtn.animate().scaleX(1.03f).scaleY(1.03f)
            .setDuration(900).withEndAction(object : Runnable {
                override fun run() {
                    sosBtn.animate().scaleX(1.0f).scaleY(1.0f)
                        .setDuration(900).withEndAction(object : Runnable {
                            override fun run() {
                                sosBtn.animate().scaleX(1.03f).scaleY(1.03f)
                                    .setDuration(900).withEndAction(this).start()
                            }
                        }).start()
                }
            }).start()

        // Admin button (gear icon, 3s long press)
        val adminBtn = Button(this).apply {
            text = "\u2699\uFE0F"
            textSize = 22f
            setTextColor(0xFF8B7D6B.toInt())
            val bg = GradientDrawable().apply {
                setColor(0xFFF6EEDD.toInt())
                cornerRadius = dp(16).toFloat()
                setStroke(dp(1), BAR_BORDER)
            }
            background = bg
            elevation = dp(2).toFloat()
            stateListAnimator = null
            layoutParams = LinearLayout.LayoutParams(dp(68), dp(68))
        }
        var adminRunnable: Runnable? = null
        val adminHandler = Handler(Looper.getMainLooper())
        adminBtn.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    adminRunnable = Runnable {
                        v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        startActivity(Intent(this@LauncherActivity, AdminActivity::class.java))
                    }
                    adminHandler.postDelayed(adminRunnable!!, 3000)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    adminRunnable?.let { adminHandler.removeCallbacks(it) }
                    true
                }
                else -> false
            }
        }
        bottom.addView(adminBtn)
        bottomOuter.addView(bottom)
        root.addView(bottomOuter)
        return root
    }

    // ---- App grid ----
    private fun buildAppGrid() {
        grid.removeAllViews()

        // Add Contacts tile first (cheerful pink)
        grid.addView(makeSpecialTile("👥 אנשי קשר", 0xFFEC407A.toInt()) {
            startActivity(Intent(this, ContactsActivity::class.java))
        })

        // Add Claude tile (cheerful purple)
        grid.addView(makeSpecialTile("🤖 Claude", 0xFF8E7CC3.toInt()) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://claude.ai")))
            } catch (_: Exception) {
                Toast.makeText(this, "Claude AI Assistant - Connected", Toast.LENGTH_LONG).show()
            }
        })

        // Family video-call tile (teal gradient) — tries Meet → WhatsApp → browser
        grid.addView(makeSpecialTile("📹 שיחת משפחה", 0xFF26A69A.toInt()) {
            openFamilyVideoCall()
        })

        val whitelist = (prefs.getString(GuardianApp.KEY_WHITELIST, "") ?: "")
            .split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val holdMs = prefs.getLong(GuardianApp.KEY_TOUCH_HOLD_MS, GuardianApp.DEFAULT_HOLD_MS)
        // Rainbow tile palette
        val tilePalette = intArrayOf(
            0xFF66BB6A.toInt(),  // green
            0xFF42A5F5.toInt(),  // sky blue
            0xFFFFA726.toInt(),  // orange
            0xFFAB47BC.toInt(),  // purple
            0xFF26C6DA.toInt(),  // teal
            0xFFFFCA28.toInt(),  // amber
            0xFFEF5350.toInt(),  // coral
            0xFF78909C.toInt()   // slate
        )
        var idx = 0
        for (pkg in whitelist) {
            if (!isInstalled(pkg)) continue
            grid.addView(makeAppTile(pkg, holdMs, tilePalette[idx % tilePalette.size]))
            idx++
        }
        if (grid.childCount == 2) {
            // Only special tiles, no apps
            grid.addView(TextView(this).apply {
                text = "לחץ לחיצה ארוכה על \u2699\uFE0F להגדרות"
                textSize = 16f
                setTextColor(TEXT_SEC)
                gravity = Gravity.CENTER
                setPadding(dp(20), dp(60), dp(20), dp(60))
            })
        }
    }

    private fun makeSpecialTile(label: String, bgColor: Int, onClick: () -> Unit): View {
        // Split emoji + label so we can render the emoji huge
        val parts = label.split(" ", limit = 2)
        val emojiStr = if (parts.size == 2) parts[0] else ""
        val labelStr = if (parts.size == 2) parts[1] else label

        val cell = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(22), dp(8), dp(22))
            // soft vertical gradient from base color → slightly darker
            val darker = darken(bgColor, 0.85f)
            val bg = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(bgColor, darker)
            ).apply {
                cornerRadius = dp(24).toFloat()
            }
            background = bg
            elevation = dp(6).toFloat()
            val spec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            layoutParams = GridLayout.LayoutParams(spec, spec).apply {
                width = 0; height = WRAP
                setMargins(dp(8), dp(8), dp(8), dp(8))
            }
        }

        if (emojiStr.isNotEmpty()) {
            cell.addView(TextView(this).apply {
                text = emojiStr
                textSize = 44f
                gravity = Gravity.CENTER
            })
        }
        cell.addView(TextView(this).apply {
            text = labelStr
            textSize = 20f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            maxLines = 2
            setPadding(0, dp(6), 0, 0)
        })

        cell.setOnClickListener { onClick() }
        return cell
    }

    // ---- Family video call (WhatsApp-first) ----
    // Goal: one tap → actual video call ringing on the family member's phone.
    // The old implementation just launched WhatsApp's home screen which
    // required Dad to navigate, find the contact, and tap video — defeating
    // the whole purpose of the tile. This version resolves a target phone
    // number, queries ContactsContract for the hidden WhatsApp video-call
    // data row on that contact, and fires ACTION_VIEW on it — the exact
    // intent the WhatsApp "Video call" button itself emits.
    private fun openFamilyVideoCall() {
        // Priority 1: configured URL (group deep-link or wa.me link)
        val customUrl = prefs.getString(GuardianApp.KEY_FAMILY_CALL_URL, null)
        if (!customUrl.isNullOrBlank() && customUrl.startsWith("http", ignoreCase = true)) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(customUrl))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return
            } catch (_: Exception) {}
        }

        // Priority 2: direct WhatsApp video call to a configured number
        val target = resolveFamilyTargetNumber()
        if (!target.isNullOrBlank()) {
            if (launchWhatsAppVideoCall(target)) return
            // Second-best: chat deep link so the user lands on the thread
            // and the video button is one tap away (better than a cold home).
            val waMe = "https://wa.me/" + target.filter { it.isDigit() || it == '+' }
                .removePrefix("+")
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(waMe))
                    .setPackage("com.whatsapp")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return
            } catch (_: Exception) {}
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(waMe))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return
            } catch (_: Exception) {}
        }

        // Last resort: WhatsApp home, then Business, then toast
        try {
            val i = packageManager.getLaunchIntentForPackage("com.whatsapp")
            if (i != null) {
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(i)
                Toast.makeText(this,
                    "הוסיפו מספר שיחת משפחה בהגדרות",
                    Toast.LENGTH_LONG).show()
                return
            }
        } catch (_: Exception) {}
        try {
            val i = packageManager.getLaunchIntentForPackage("com.whatsapp.w4b")
            if (i != null) {
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(i)
                return
            }
        } catch (_: Exception) {}
        Toast.makeText(this, "WhatsApp לא מותקן", Toast.LENGTH_LONG).show()
    }

    /** Returns the phone number to video-call, or null if none is configured. */
    private fun resolveFamilyTargetNumber(): String? {
        // 1) Explicit pref
        prefs.getString(GuardianApp.KEY_FAMILY_CALL_PHONE, null)
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }
        // 2) If KEY_FAMILY_CALL_URL holds a raw phone rather than a URL
        prefs.getString(GuardianApp.KEY_FAMILY_CALL_URL, null)
            ?.takeIf { it.isNotBlank() && !it.startsWith("http", ignoreCase = true) }
            ?.let { return it }
        // 3) First "important" contact — the natural family-call target
        try {
            val json = prefs.getString(GuardianApp.KEY_IMPORTANT_CONTACTS, "") ?: ""
            if (json.isNotEmpty()) {
                val arr = org.json.JSONArray(json)
                if (arr.length() > 0) {
                    return arr.getJSONObject(0).optString("number").takeIf { it.isNotBlank() }
                }
            }
        } catch (_: Exception) {}
        // 4) SOS number as final fallback
        return prefs.getString(GuardianApp.KEY_SOS_NUMBER, null)
            ?.takeIf { it.isNotBlank() }
    }

    /**
     * Tries to launch WhatsApp's native video-call activity for [phone] by
     * finding the hidden contact data row WhatsApp installs under its
     * special MIMETYPE. Returns true on successful startActivity.
     *
     * This is the same mechanism WhatsApp's own contact detail screen uses
     * when you tap the video icon — an ACTION_VIEW on a ContactsContract.Data
     * row whose MIMETYPE is "vnd.android.cursor.item/vnd.com.whatsapp.video.call".
     */
    private fun launchWhatsAppVideoCall(phone: String): Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED) return false

        val mimeTypes = listOf(
            "vnd.android.cursor.item/vnd.com.whatsapp.video.call",
            "vnd.android.cursor.item/vnd.com.whatsapp.w4b.video.call"
        )

        val dataId = findWhatsAppDataRowId(phone, mimeTypes) ?: return false

        for (mime in mimeTypes) {
            try {
                val uri = android.content.ContentUris.withAppendedId(
                    ContactsContract.Data.CONTENT_URI, dataId)
                startActivity(Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, mime)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return true
            } catch (_: Exception) { /* try next mime */ }
        }
        return false
    }

    /**
     * Walks ContactsContract.Data looking for the WhatsApp video-call row
     * belonging to the contact whose phone matches [phone]. Matching is done
     * via PhoneNumberUtils.compare so +972-55-1234567 matches 0551234567.
     */
    private fun findWhatsAppDataRowId(
        phone: String,
        mimeTypes: List<String>,
    ): Long? {
        return try {
            // Step 1: find contact-ids matching the phone number.
            val phoneUri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phone))
            val contactIds = mutableSetOf<Long>()
            contentResolver.query(
                phoneUri,
                arrayOf(ContactsContract.PhoneLookup._ID),
                null, null, null
            )?.use { c ->
                while (c.moveToNext()) contactIds.add(c.getLong(0))
            }
            if (contactIds.isEmpty()) return null

            // Step 2: for those contacts, find the WhatsApp video-call row.
            val placeholders = contactIds.joinToString(",") { "?" }
            val mimePlaceholders = mimeTypes.joinToString(",") { "?" }
            val selection = "${ContactsContract.Data.CONTACT_ID} IN ($placeholders)" +
                " AND ${ContactsContract.Data.MIMETYPE} IN ($mimePlaceholders)"
            val args = (contactIds.map { it.toString() } + mimeTypes).toTypedArray()
            contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf(ContactsContract.Data._ID),
                selection, args, null
            )?.use { c ->
                if (c.moveToFirst()) return c.getLong(0)
            }
            null
        } catch (_: Exception) { null }
    }

    // ---- Cheerful decorative banner (custom drawing) ----
    private fun buildDecorBanner(): View {
        return object : View(this) {
            private val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
            override fun onDraw(c: android.graphics.Canvas) {
                super.onDraw(c)
                val w = width.toFloat()
                val h = height.toFloat()
                if (w <= 0 || h <= 0) return

                // Sun (right side)
                val sunCx = w * 0.85f
                val sunCy = h * 0.55f
                val sunR  = h * 0.32f
                // rays
                p.color = 0xFFFFC947.toInt()
                p.strokeWidth = dp(3).toFloat()
                p.strokeCap = android.graphics.Paint.Cap.ROUND
                for (i in 0 until 12) {
                    val ang = Math.toRadians(i * 30.0)
                    val sx = sunCx + Math.cos(ang).toFloat() * sunR * 1.15f
                    val sy = sunCy + Math.sin(ang).toFloat() * sunR * 1.15f
                    val ex = sunCx + Math.cos(ang).toFloat() * sunR * 1.55f
                    val ey = sunCy + Math.sin(ang).toFloat() * sunR * 1.55f
                    c.drawLine(sx, sy, ex, ey, p)
                }
                // sun body
                p.style = android.graphics.Paint.Style.FILL
                p.color = 0xFFFFD54F.toInt()
                c.drawCircle(sunCx, sunCy, sunR, p)
                // smile
                p.color = 0xFF7A4A00.toInt()
                p.style = android.graphics.Paint.Style.STROKE
                p.strokeWidth = dp(2.5f.toInt()).toFloat().coerceAtLeast(dp(2).toFloat())
                val sm = android.graphics.RectF(
                    sunCx - sunR*0.45f, sunCy - sunR*0.15f,
                    sunCx + sunR*0.45f, sunCy + sunR*0.55f)
                c.drawArc(sm, 20f, 140f, false, p)
                // eyes
                p.style = android.graphics.Paint.Style.FILL
                c.drawCircle(sunCx - sunR*0.28f, sunCy - sunR*0.2f, dp(3).toFloat(), p)
                c.drawCircle(sunCx + sunR*0.28f, sunCy - sunR*0.2f, dp(3).toFloat(), p)

                // Clouds (left side)
                p.color = 0xFFFFFFFF.toInt()
                val cloudY = h * 0.4f
                drawCloud(c, w * 0.18f, cloudY, h * 0.22f)
                drawCloud(c, w * 0.42f, h * 0.7f, h * 0.16f)

                // Flowers row at the very bottom
                val fy = h - dp(10).toFloat()
                val colors = intArrayOf(
                    0xFFFF6B9D.toInt(), 0xFFFFD93D.toInt(),
                    0xFFA78BFA.toInt(), 0xFF6EE7B7.toInt(),
                    0xFFFB923C.toInt(), 0xFFEF4444.toInt()
                )
                val step = w / (colors.size + 1)
                for (i in colors.indices) {
                    drawFlower(c, step * (i + 1), fy, dp(6).toFloat(), colors[i])
                }
            }
            private fun drawCloud(c: android.graphics.Canvas, cx: Float, cy: Float, r: Float) {
                c.drawCircle(cx, cy, r * 0.7f, p)
                c.drawCircle(cx - r*0.6f, cy + r*0.1f, r * 0.55f, p)
                c.drawCircle(cx + r*0.6f, cy + r*0.1f, r * 0.55f, p)
                c.drawCircle(cx - r*0.2f, cy - r*0.3f, r * 0.5f, p)
                c.drawCircle(cx + r*0.25f, cy - r*0.25f, r * 0.5f, p)
            }
            private fun drawFlower(c: android.graphics.Canvas, cx: Float, cy: Float, r: Float, col: Int) {
                p.color = col
                for (i in 0 until 6) {
                    val ang = Math.toRadians(i * 60.0)
                    val px = cx + Math.cos(ang).toFloat() * r
                    val py = cy + Math.sin(ang).toFloat() * r
                    c.drawCircle(px, py, r * 0.7f, p)
                }
                p.color = 0xFFFFEB3B.toInt()
                c.drawCircle(cx, cy, r * 0.5f, p)
            }
        }.apply {
            layoutParams = LinearLayout.LayoutParams(MATCH, dp(90))
        }
    }

    private fun darken(color: Int, factor: Float): Int {
        val a = (color ushr 24) and 0xFF
        val r = ((color ushr 16) and 0xFF) * factor
        val g = ((color ushr 8)  and 0xFF) * factor
        val b = ( color         and 0xFF) * factor
        return (a shl 24) or (r.toInt().coerceIn(0,255) shl 16) or
               (g.toInt().coerceIn(0,255) shl 8) or b.toInt().coerceIn(0,255)
    }

    private fun makeAppTile(pkg: String, holdMs: Long, tintColor: Int = 0xFFFFFFFF.toInt()): View {
        val pm = packageManager
        val appInfo = try { pm.getApplicationInfo(pkg, 0) } catch (_: Exception) { null }
        val label = try { pm.getApplicationLabel(appInfo!!).toString() } catch (_: Exception) {
            pkg.substringAfterLast('.')
        }
        val icon = try { pm.getApplicationIcon(pkg) } catch (_: Exception) { null }

        val cell = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(18), dp(8), dp(18))
            val darker = darken(tintColor, 0.8f)
            val bg = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(tintColor, darker)
            ).apply {
                cornerRadius = dp(24).toFloat()
            }
            background = bg
            elevation = dp(6).toFloat()
            val spec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            layoutParams = GridLayout.LayoutParams(spec, spec).apply {
                width = 0; height = WRAP
                setMargins(dp(8), dp(8), dp(8), dp(8))
            }
        }

        // Icon on white rounded plate for nice pop
        val iconPlate = FrameLayout(this).apply {
            val bg = GradientDrawable().apply {
                setColor(0xFFFFFFFF.toInt())
                shape = GradientDrawable.OVAL
            }
            background = bg
            elevation = dp(2).toFloat()
            layoutParams = LinearLayout.LayoutParams(dp(76), dp(76)).apply {
                gravity = Gravity.CENTER
                bottomMargin = dp(8)
            }
        }
        val iv = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(dp(54), dp(54)).apply {
                gravity = Gravity.CENTER
            }
        }
        if (icon != null) iv.setImageDrawable(icon)
        iconPlate.addView(iv)
        cell.addView(iconPlate)

        // App label — white on color tile
        cell.addView(TextView(this).apply {
            text = label
            textSize = 17f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            maxLines = 2
        })

        // Instant-tap launch + visual press feedback. Old behavior required
        // the user to HOLD the finger ≥ holdMs or it would cancel — a quick
        // tap produced no result, which is what "dad app lost its touch
        // sensitivity" really meant. Now any tap triggers launch, while the
        // onTouch listener only provides scale/alpha feedback and forwards
        // the event (returns false so the click dispatcher still fires).
        cell.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.alpha = 0.6f; v.scaleX = 0.95f; v.scaleY = 0.95f
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.alpha = 1f; v.scaleX = 1f; v.scaleY = 1f
                }
            }
            false
        }
        cell.setOnClickListener {
            v -> v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            launchApp(pkg)
        }
        return cell
    }

    /**
     * Attach a short hold-to-fire touch listener. Used for SOS and
     * flashlight: still responsive to a deliberate press, but a mere brush
     * won't trigger. [holdMs] around 250 is a good sweet spot — feels snappy
     * yet filters accidental contact.
     */
    private fun attachHoldListener(view: View, holdMs: Long, onFire: () -> Unit) {
        var fireRunnable: Runnable? = null
        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.alpha = 0.75f; v.scaleX = 0.96f; v.scaleY = 0.96f
                    fireRunnable = Runnable {
                        v.alpha = 1f; v.scaleX = 1f; v.scaleY = 1f
                        v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        onFire()
                    }
                    handler.postDelayed(fireRunnable!!, holdMs)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    fireRunnable?.let { handler.removeCallbacks(it) }
                    v.alpha = 1f; v.scaleX = 1f; v.scaleY = 1f
                    true
                }
                else -> false
            }
        }
    }

    // ---- Flashlight (rear) ----
    private fun toggleFlashlight() {
        try {
            val cm = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cm.cameraIdList.firstOrNull { id ->
                val chars = cm.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.LENS_FACING) ==
                    CameraCharacteristics.LENS_FACING_BACK &&
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            if (cameraId != null) {
                flashlightOn = !flashlightOn
                cm.setTorchMode(cameraId, flashlightOn)
                btnFlash.text = if (flashlightOn) "\uD83D\uDD26 ON" else "\uD83D\uDD26 פנס"
                val bg = btnFlash.background as? GradientDrawable
                if (flashlightOn) {
                    bg?.setColor(0xFFFF8F00.toInt())  // darker amber when ON
                    btnFlash.setTextColor(0xFFFFFFFF.toInt())
                } else {
                    bg?.setColor(0xFFFFB300.toInt())  // bright amber
                    btnFlash.setTextColor(TEXT_DARK)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("Launcher", "Flashlight error: ${e.message}")
        }
    }

    // ---- Clock ----
    private fun updateClock() {
        val c = Calendar.getInstance()
        tvTime.text = "%02d:%02d".format(
            c.get(Calendar.HOUR_OF_DAY),
            c.get(Calendar.MINUTE))
        val dow = c.get(Calendar.DAY_OF_WEEK) - 1  // 0=Sunday
        val day = c.get(Calendar.DAY_OF_MONTH)
        val month = hebrewMonths[c.get(Calendar.MONTH)]
        val year = c.get(Calendar.YEAR)
        tvDate.text = "יום ${hebrewDays[dow]}  ·  $day ב$month $year"
        if (::tvGreeting.isInitialized) tvGreeting.text = timeOfDayGreeting()
    }

    private fun startClockTick() {
        handler.postDelayed(object : Runnable {
            override fun run() { updateClock(); handler.postDelayed(this, 30_000) }
        }, 30_000)
    }

    private fun registerBatteryReceiver() {
        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                val lvl = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val sc  = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                val pct = if (sc > 0) lvl * 100 / sc else 0
                tvBattery.text = "BAT $pct%"
                if (pct < 20) tvBattery.setTextColor(SOS_RED)
                else tvBattery.setTextColor(TEXT_SEC)
            }
        }, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    private fun isInstalled(pkg: String) = try {
        packageManager.getPackageInfo(pkg, 0); true
    } catch (_: PackageManager.NameNotFoundException) { false }

    private fun launchApp(pkg: String) {
        packageManager.getLaunchIntentForPackage(pkg)?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(it)
        }
    }

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    private val WRAP  = ViewGroup.LayoutParams.WRAP_CONTENT

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onBackPressed() { /* blocked */ }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
