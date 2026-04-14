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
        // Sky palette — the app now looks like a cheerful summer sky.
        // Keep TEXT_DARK readable against cloud-white cards.
        private const val SKY_TOP       = 0xFF7EC8F5.toInt()   // high sky
        private const val SKY_MID       = 0xFFBDE4FA.toInt()   // mid sky
        private const val SKY_LOW       = 0xFFE6F5FF.toInt()   // horizon haze
        private const val TEXT_DARK     = 0xFF1A365D.toInt()   // deep-sky navy
        private const val TEXT_SEC      = 0xFF4A6B8A.toInt()   // muted blue-grey
        private const val CLOUD_WHITE   = 0xFFFFFFFF.toInt()
        private const val CLOUD_SHADOW  = 0xFFDCEAF5.toInt()
        private const val SOS_RED       = 0xFFE53935.toInt()
        private const val BAR_BORDER    = 0xFFC9DEEF.toInt()
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
    // New design: single screen, no scrolling.
    // Stack (top → bottom):
    //   1. Sky banner with clouds, birds, sun, butterflies (decorative)
    //   2. EMERGENCY ROW — SOS, Family Video Call, Flashlight (+ admin gear)
    //   3. Compact greeting + clock + battery + date
    //   4. App grid — fills the remaining space, cells auto-size
    private fun buildLayout(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            // Soft daytime sky — high sky → horizon haze
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(SKY_TOP, SKY_MID, SKY_LOW)
            )
            layoutParams = ViewGroup.LayoutParams(MATCH, MATCH)
        }

        // 1) Sky banner — the decorative drawing strip
        root.addView(buildDecorBanner())

        // 2) EMERGENCY ROW — anchored at the top so Dad never scrolls for it.
        //    Order (LTR): Flashlight | SOS (wide, centered) | Video call | Admin gear
        //    The app is Hebrew-RTL, but these three icons are universal and
        //    muscle memory matters more than reading order. We lay them out
        //    with SOS big in the middle, the two helpers flanking it.
        root.addView(buildEmergencyRow())

        // 3) Compact greeting / clock / battery / date
        root.addView(buildGreetingStrip())

        // 4) App grid — fills the rest, NO ScrollView (fit-to-screen).
        grid = GridLayout(this).apply {
            columnCount = 3
            useDefaultMargins = false
            // Grid takes all remaining vertical space, rows stretch to fit.
            layoutParams = LinearLayout.LayoutParams(MATCH, 0, 1f).apply {
                setMargins(dp(10), dp(4), dp(10), dp(12))
            }
        }
        root.addView(grid)

        return root
    }

    // ---- Emergency row (top-anchored) ----
    private fun buildEmergencyRow(): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            // Cloud-shaped rounded card behind the three emergency buttons
            val bg = GradientDrawable().apply {
                setColor(0xF2FFFFFF.toInt())  // near-white cloud with slight transparency
                cornerRadius = dp(28).toFloat()
                setStroke(dp(1), 0xFFE0EEFA.toInt())
            }
            background = bg
            elevation = dp(6).toFloat()
            setPadding(dp(10), dp(10), dp(10), dp(10))
            layoutParams = LinearLayout.LayoutParams(MATCH, WRAP).apply {
                setMargins(dp(12), dp(6), dp(12), dp(6))
            }
        }

        // Flashlight — amber sun-coloured pill on the start side
        btnFlash = Button(this).apply {
            text = "\uD83D\uDD26\nפנס"
            textSize = 15f
            setTextColor(TEXT_DARK)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            val bg = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(0xFFFFE082.toInt(), 0xFFFFB300.toInt())
            ).apply {
                cornerRadius = dp(22).toFloat()
                setStroke(dp(2), 0xFFFFA000.toInt())
            }
            background = bg
            elevation = dp(3).toFloat()
            stateListAnimator = null
            includeFontPadding = false
            layoutParams = LinearLayout.LayoutParams(0, dp(78), 1f).apply {
                marginEnd = dp(6)
            }
        }
        attachHoldListener(btnFlash, 250L) { toggleFlashlight() }
        row.addView(btnFlash)

        // SOS — bold red centre, widest slot
        val sosBtn = Button(this).apply {
            text = "🆘 SOS"
            textSize = 24f
            setTextColor(0xFFFFFFFF.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            val bg = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(0xFFEF5350.toInt(), 0xFFC62828.toInt())
            ).apply {
                cornerRadius = dp(22).toFloat()
                setStroke(dp(3), 0xFFFFE082.toInt())
            }
            background = bg
            elevation = dp(6).toFloat()
            stateListAnimator = null
            setShadowLayer(dp(2).toFloat(), 0f, dp(1).toFloat(), 0x66000000)
            includeFontPadding = false
            layoutParams = LinearLayout.LayoutParams(0, dp(78), 1.6f).apply {
                marginStart = dp(6); marginEnd = dp(6)
            }
        }
        attachHoldListener(sosBtn, 250L) {
            startActivity(Intent(this@LauncherActivity, SosActivity::class.java))
        }
        row.addView(sosBtn)
        // Gentle pulse so the eye finds it fast
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

        // Family video call — teal/sky pill on the end side
        val callBtn = Button(this).apply {
            text = "\uD83D\uDCF9\nשיחה"
            textSize = 15f
            setTextColor(0xFFFFFFFF.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            val bg = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(0xFF4FC3F7.toInt(), 0xFF0288D1.toInt())
            ).apply {
                cornerRadius = dp(22).toFloat()
                setStroke(dp(2), 0xFF0277BD.toInt())
            }
            background = bg
            elevation = dp(3).toFloat()
            stateListAnimator = null
            includeFontPadding = false
            layoutParams = LinearLayout.LayoutParams(0, dp(78), 1f).apply {
                marginStart = dp(6); marginEnd = dp(6)
            }
        }
        // Instant tap — fewer gates on the family call; it just opens WhatsApp video.
        callBtn.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            openFamilyVideoCall()
        }
        row.addView(callBtn)

        // Admin gear — small, 3s hold to enter settings
        val adminBtn = Button(this).apply {
            text = "\u2699\uFE0F"
            textSize = 18f
            setTextColor(TEXT_SEC)
            val bg = GradientDrawable().apply {
                setColor(0xFFEAF4FB.toInt())
                cornerRadius = dp(16).toFloat()
                setStroke(dp(1), BAR_BORDER)
            }
            background = bg
            elevation = dp(2).toFloat()
            stateListAnimator = null
            layoutParams = LinearLayout.LayoutParams(dp(54), dp(54))
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
        row.addView(adminBtn)
        return row
    }

    // ---- Compact greeting / clock strip ----
    private fun buildGreetingStrip(): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val bg = GradientDrawable().apply {
                setColor(0xE6FFFFFF.toInt())  // soft-white cloud card
                cornerRadius = dp(22).toFloat()
                setStroke(dp(1), 0xFFE0EEFA.toInt())
            }
            background = bg
            elevation = dp(4).toFloat()
            setPadding(dp(16), dp(10), dp(16), dp(10))
            layoutParams = LinearLayout.LayoutParams(MATCH, WRAP).apply {
                setMargins(dp(12), dp(4), dp(12), dp(6))
            }
        }

        // Left: battery chip + greeting stacked
        val leftCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, WRAP, 1f)
        }
        tvBattery = TextView(this).apply {
            textSize = 13f
            setTextColor(TEXT_SEC)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            val chipBg = GradientDrawable().apply {
                setColor(0xFFE3F2FD.toInt())
                cornerRadius = dp(12).toFloat()
            }
            background = chipBg
            setPadding(dp(12), dp(4), dp(12), dp(4))
            layoutParams = LinearLayout.LayoutParams(WRAP, WRAP)
        }
        leftCol.addView(tvBattery)
        tvGreeting = TextView(this).apply {
            text = timeOfDayGreeting()
            textSize = 20f
            setTextColor(TEXT_DARK)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, dp(6), 0, 0)
            layoutParams = LinearLayout.LayoutParams(WRAP, WRAP)
        }
        leftCol.addView(tvGreeting)
        card.addView(leftCol)

        // Right: clock (big) + date
        val rightCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(WRAP, WRAP)
        }
        tvTime = TextView(this).apply {
            textSize = 44f
            setTextColor(TEXT_DARK)
            typeface = android.graphics.Typeface.MONOSPACE
            setShadowLayer(dp(2).toFloat(), 0f, dp(1).toFloat(), 0x22000000)
            maxLines = 1
            isSingleLine = true
            includeFontPadding = false
            gravity = Gravity.END
        }
        rightCol.addView(tvTime)
        tvDate = TextView(this).apply {
            textSize = 14f
            setTextColor(TEXT_SEC)
            gravity = Gravity.END
        }
        rightCol.addView(tvDate)
        card.addView(rightCol)

        return card
    }

    // ---- App grid ----
    // Video-call moved to top emergency row; grid now holds Contacts, Claude,
    // and the whitelisted apps. Grid is 3 columns, cells auto-stretch to fill
    // available height (no scrolling) via GridLayout rowSpec weight=1f.
    private fun buildAppGrid() {
        grid.removeAllViews()
        val tiles = mutableListOf<View>()

        // Contacts (sky-pink cloud tile)
        tiles.add(makeSpecialTile("👥 אנשי קשר", 0xFFF48FB1.toInt()) {
            startActivity(Intent(this, ContactsActivity::class.java))
        })
        // Claude (lavender cloud tile)
        tiles.add(makeSpecialTile("🤖 Claude", 0xFFB39DDB.toInt()) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://claude.ai")))
            } catch (_: Exception) {
                Toast.makeText(this, "Claude AI Assistant - Connected", Toast.LENGTH_LONG).show()
            }
        })

        val whitelist = (prefs.getString(GuardianApp.KEY_WHITELIST, "") ?: "")
            .split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val holdMs = prefs.getLong(GuardianApp.KEY_TOUCH_HOLD_MS, GuardianApp.DEFAULT_HOLD_MS)
        // Sky-themed tile palette — soft cloud-coloured pastels
        val tilePalette = intArrayOf(
            0xFF81D4FA.toInt(),  // light sky
            0xFFA5D6A7.toInt(),  // meadow green
            0xFFFFCC80.toInt(),  // sunrise orange
            0xFFCE93D8.toInt(),  // lilac
            0xFF80DEEA.toInt(),  // lagoon teal
            0xFFFFE082.toInt(),  // sun yellow
            0xFFF48FB1.toInt(),  // blossom pink
            0xFFBCAAA4.toInt()   // soft taupe
        )
        var idx = 0
        for (pkg in whitelist) {
            if (!isInstalled(pkg)) continue
            tiles.add(makeAppTile(pkg, holdMs, tilePalette[idx % tilePalette.size]))
            idx++
        }

        // Lay out in 3 columns, rows stretch to fit the remaining space.
        val cols = 3
        val rows = (tiles.size + cols - 1) / cols
        grid.columnCount = cols
        grid.rowCount = rows.coerceAtLeast(1)
        for ((i, tile) in tiles.withIndex()) {
            val lp = GridLayout.LayoutParams(
                GridLayout.spec(i / cols, 1, GridLayout.FILL, 1f),
                GridLayout.spec(i % cols, 1, GridLayout.FILL, 1f)
            ).apply {
                width = 0; height = 0
                setMargins(dp(6), dp(6), dp(6), dp(6))
            }
            tile.layoutParams = lp
            grid.addView(tile)
        }

        if (tiles.size <= 2) {
            grid.addView(TextView(this).apply {
                text = "לחץ לחיצה ארוכה על \u2699\uFE0F להגדרות"
                textSize = 15f
                setTextColor(TEXT_SEC)
                gravity = Gravity.CENTER
                setPadding(dp(20), dp(40), dp(20), dp(40))
            })
        }
    }

    private fun makeSpecialTile(label: String, bgColor: Int, onClick: () -> Unit): View {
        // Split emoji + label so we can render the emoji huge
        val parts = label.split(" ", limit = 2)
        val emojiStr = if (parts.size == 2) parts[0] else ""
        val labelStr = if (parts.size == 2) parts[1] else label

        // Sizes tuned for 3-column fit-to-screen. buildAppGrid overrides the
        // final layoutParams, so this cell just sets visual styling.
        val cell = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(6), dp(10), dp(6), dp(10))
            val darker = darken(bgColor, 0.85f)
            val bg = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(bgColor, darker)
            ).apply {
                cornerRadius = dp(22).toFloat()
            }
            background = bg
            elevation = dp(5).toFloat()
        }

        if (emojiStr.isNotEmpty()) {
            cell.addView(TextView(this).apply {
                text = emojiStr
                textSize = 32f
                gravity = Gravity.CENTER
                includeFontPadding = false
            })
        }
        cell.addView(TextView(this).apply {
            text = labelStr
            textSize = 15f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            maxLines = 2
            setPadding(0, dp(4), 0, 0)
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

    // ---- Sky banner: clouds + sun + birds + butterflies ----
    // A single View renders the whole whimsical sky strip on a Canvas, so no
    // PNG assets, no density scaling issues, and it respects screen width
    // automatically. Composition:
    //   - Sun with smile, front-right
    //   - Two clouds, front-left + upper-mid
    //   - Two birds (M-shaped strokes), mid-air
    //   - Two butterflies (four symmetric ellipses + body), one near each cloud
    private fun buildDecorBanner(): View {
        return object : View(this) {
            private val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
            override fun onDraw(c: android.graphics.Canvas) {
                super.onDraw(c)
                val w = width.toFloat()
                val h = height.toFloat()
                if (w <= 0 || h <= 0) return

                // --- Sun (right side) ---
                val sunCx = w * 0.86f
                val sunCy = h * 0.52f
                val sunR  = h * 0.32f
                // rays
                p.color = 0xFFFFC947.toInt()
                p.strokeWidth = dp(3).toFloat()
                p.strokeCap = android.graphics.Paint.Cap.ROUND
                p.style = android.graphics.Paint.Style.STROKE
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
                p.strokeWidth = dp(2).toFloat()
                val sm = android.graphics.RectF(
                    sunCx - sunR*0.45f, sunCy - sunR*0.15f,
                    sunCx + sunR*0.45f, sunCy + sunR*0.55f)
                c.drawArc(sm, 20f, 140f, false, p)
                // eyes
                p.style = android.graphics.Paint.Style.FILL
                c.drawCircle(sunCx - sunR*0.28f, sunCy - sunR*0.2f, dp(3).toFloat(), p)
                c.drawCircle(sunCx + sunR*0.28f, sunCy - sunR*0.2f, dp(3).toFloat(), p)

                // --- Clouds ---
                p.color = 0xFFFFFFFF.toInt()
                drawCloud(c, w * 0.18f, h * 0.40f, h * 0.22f)
                drawCloud(c, w * 0.46f, h * 0.68f, h * 0.15f)
                drawCloud(c, w * 0.62f, h * 0.28f, h * 0.14f)

                // --- Birds (simple V/M strokes) ---
                p.style = android.graphics.Paint.Style.STROKE
                p.strokeWidth = dp(2).toFloat()
                p.strokeCap = android.graphics.Paint.Cap.ROUND
                p.color = 0xFF2E4A6B.toInt()
                drawBird(c, w * 0.33f, h * 0.20f, dp(14).toFloat())
                drawBird(c, w * 0.55f, h * 0.48f, dp(10).toFloat())
                drawBird(c, w * 0.72f, h * 0.78f, dp(12).toFloat())

                // --- Butterflies ---
                drawButterfly(c, w * 0.10f, h * 0.78f, h * 0.14f, 0xFFE91E63.toInt())
                drawButterfly(c, w * 0.40f, h * 0.20f, h * 0.12f, 0xFF9C27B0.toInt())
                drawButterfly(c, w * 0.68f, h * 0.58f, h * 0.11f, 0xFFFF9800.toInt())
            }
            private fun drawCloud(c: android.graphics.Canvas, cx: Float, cy: Float, r: Float) {
                p.style = android.graphics.Paint.Style.FILL
                p.color = 0xFFFFFFFF.toInt()
                c.drawCircle(cx, cy, r * 0.7f, p)
                c.drawCircle(cx - r*0.6f, cy + r*0.1f, r * 0.55f, p)
                c.drawCircle(cx + r*0.6f, cy + r*0.1f, r * 0.55f, p)
                c.drawCircle(cx - r*0.2f, cy - r*0.3f, r * 0.5f, p)
                c.drawCircle(cx + r*0.25f, cy - r*0.25f, r * 0.5f, p)
            }
            private fun drawBird(c: android.graphics.Canvas, cx: Float, cy: Float, r: Float) {
                // Two arcs meeting in the middle → seagull silhouette.
                p.style = android.graphics.Paint.Style.STROKE
                p.strokeWidth = dp(2).toFloat()
                p.color = 0xFF2E4A6B.toInt()
                val left = android.graphics.RectF(cx - r, cy - r * 0.5f, cx, cy + r * 0.5f)
                val right = android.graphics.RectF(cx, cy - r * 0.5f, cx + r, cy + r * 0.5f)
                c.drawArc(left, 200f, 100f, false, p)
                c.drawArc(right, 240f, 100f, false, p)
            }
            private fun drawButterfly(
                c: android.graphics.Canvas, cx: Float, cy: Float, r: Float, col: Int,
            ) {
                p.style = android.graphics.Paint.Style.FILL
                // Upper wings
                p.color = col
                c.drawOval(cx - r, cy - r * 0.9f, cx - r * 0.05f, cy + r * 0.1f, p)
                c.drawOval(cx + r * 0.05f, cy - r * 0.9f, cx + r, cy + r * 0.1f, p)
                // Lower wings (slightly smaller + darker)
                p.color = darken(col, 0.75f)
                c.drawOval(cx - r * 0.85f, cy - r * 0.05f, cx - r * 0.05f, cy + r * 0.8f, p)
                c.drawOval(cx + r * 0.05f, cy - r * 0.05f, cx + r * 0.85f, cy + r * 0.8f, p)
                // Body
                p.color = 0xFF2E2E2E.toInt()
                c.drawOval(cx - r * 0.08f, cy - r * 0.75f, cx + r * 0.08f, cy + r * 0.75f, p)
                // Antennae
                p.style = android.graphics.Paint.Style.STROKE
                p.strokeWidth = dp(1).toFloat()
                c.drawLine(cx - r * 0.03f, cy - r * 0.7f, cx - r * 0.25f, cy - r * 1.1f, p)
                c.drawLine(cx + r * 0.03f, cy - r * 0.7f, cx + r * 0.25f, cy - r * 1.1f, p)
            }
        }.apply {
            layoutParams = LinearLayout.LayoutParams(MATCH, dp(110))
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

        // 3-column fit-to-screen sizing; buildAppGrid overrides the final
        // layoutParams, so this block only handles visual styling.
        val cell = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(6), dp(8), dp(6), dp(8))
            val darker = darken(tintColor, 0.85f)
            val bg = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(tintColor, darker)
            ).apply {
                cornerRadius = dp(22).toFloat()
            }
            background = bg
            elevation = dp(5).toFloat()
        }

        // Icon on cloud-white circular plate
        val iconPlate = FrameLayout(this).apply {
            val bg = GradientDrawable().apply {
                setColor(0xFFFFFFFF.toInt())
                shape = GradientDrawable.OVAL
            }
            background = bg
            elevation = dp(2).toFloat()
            layoutParams = LinearLayout.LayoutParams(dp(56), dp(56)).apply {
                gravity = Gravity.CENTER
                bottomMargin = dp(4)
            }
        }
        val iv = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(dp(40), dp(40)).apply {
                gravity = Gravity.CENTER
            }
        }
        if (icon != null) iv.setImageDrawable(icon)
        iconPlate.addView(iv)
        cell.addView(iconPlate)

        // App label — white on colour tile
        cell.addView(TextView(this).apply {
            text = label
            textSize = 14f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            maxLines = 2
            includeFontPadding = false
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
