package com.family.guardian.ui

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.drawable.GradientDrawable
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.*
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
    private lateinit var btnFrontFlash: Button
    private val handler = Handler(Looper.getMainLooper())

    // Flashlight state
    private var flashlightOn = false
    private var frontFlashOn = false
    private var frontFlashOverlay: View? = null
    private var frontFlashWm: WindowManager? = null

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
        blueLightOverlay?.setBackgroundColor(Color.argb(alpha, 255, 160, 30))
        blueLightOverlay?.visibility = View.VISIBLE
        blueLightOverlay?.isClickable = false
        blueLightOverlay?.isFocusable = false
        blueLightOverlay?.elevation = 1000f
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
            setBackgroundColor(BG_WARM)
            layoutParams = ViewGroup.LayoutParams(MATCH, MATCH)
        }

        // ---- Top bar: greeting + clock + battery ----
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(BG_WARM)
            setPadding(dp(20), dp(16), dp(20), dp(8))
        }
        // Greeting
        topBar.addView(TextView(this).apply {
            text = "שלום אבא"
            textSize = 26f
            setTextColor(TEXT_DARK)
            gravity = Gravity.END
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
        // Clock row
        val clockRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(4), 0, 0)
        }
        tvBattery = TextView(this).apply {
            textSize = 16f
            setTextColor(TEXT_SEC)
            layoutParams = LinearLayout.LayoutParams(0, WRAP, 1f)
        }
        tvTime = TextView(this).apply {
            textSize = 52f
            setTextColor(TEXT_DARK)
            typeface = android.graphics.Typeface.MONOSPACE
        }
        clockRow.addView(tvBattery)
        clockRow.addView(tvTime)
        topBar.addView(clockRow)
        // Date
        tvDate = TextView(this).apply {
            textSize = 18f
            setTextColor(TEXT_SEC)
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(MATCH, WRAP)
        }
        topBar.addView(tvDate)
        root.addView(topBar)

        // ---- Divider ----
        root.addView(View(this).apply {
            setBackgroundColor(BAR_BORDER)
            layoutParams = LinearLayout.LayoutParams(MATCH, dp(1)).apply {
                setMargins(dp(16), dp(4), dp(16), dp(4))
            }
        })

        // ---- App grid ----
        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH, 0, 1f)
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        grid = GridLayout(this).apply {
            columnCount = 2
            layoutParams = ViewGroup.LayoutParams(MATCH, WRAP)
        }
        scroll.addView(grid)
        root.addView(scroll)

        // ---- Bottom bar ----
        val bottomOuter = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(BG_WARM)
        }
        // Border on top
        bottomOuter.addView(View(this).apply {
            setBackgroundColor(BAR_BORDER)
            layoutParams = LinearLayout.LayoutParams(MATCH, dp(1))
        })
        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }

        // Flashlight button
        btnFlash = Button(this).apply {
            text = "פנס"
            textSize = 14f
            setTextColor(TEXT_DARK)
            setBackgroundResource(android.R.color.transparent)
            val bg = GradientDrawable().apply {
                setColor(0xFFF5EDE0.toInt())
                cornerRadius = dp(14).toFloat()
                setStroke(dp(1), BAR_BORDER)
            }
            background = bg
            layoutParams = LinearLayout.LayoutParams(dp(70), dp(56)).apply {
                marginEnd = dp(6)
            }
            setOnClickListener { toggleFlashlight() }
        }
        bottom.addView(btnFlash)

        // Front flash button
        btnFrontFlash = Button(this).apply {
            text = "פנס ++"
            textSize = 13f
            setTextColor(TEXT_DARK)
            val bg = GradientDrawable().apply {
                setColor(0xFFF5EDE0.toInt())
                cornerRadius = dp(14).toFloat()
                setStroke(dp(1), BAR_BORDER)
            }
            background = bg
            layoutParams = LinearLayout.LayoutParams(dp(76), dp(56)).apply {
                marginEnd = dp(8)
            }
            setOnClickListener { toggleFrontFlash() }
        }
        bottom.addView(btnFrontFlash)

        // SOS button
        val sosBtn = Button(this).apply {
            text = "SOS"
            textSize = 22f
            setTextColor(0xFFFFFFFF.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            val bg = GradientDrawable().apply {
                setColor(SOS_RED)
                cornerRadius = dp(16).toFloat()
                setStroke(dp(3), ACCENT_AMBER)
            }
            background = bg
            layoutParams = LinearLayout.LayoutParams(0, dp(56), 1f).apply {
                marginEnd = dp(8)
            }
            setOnClickListener {
                startActivity(Intent(this@LauncherActivity, SosActivity::class.java))
            }
        }
        bottom.addView(sosBtn)

        // Admin button (hidden -- 3s long press)
        val adminBtn = Button(this).apply {
            text = "..."
            textSize = 14f
            setTextColor(0xFFCCC0B0.toInt())  // very dim
            setBackgroundColor(BG_WARM)
            layoutParams = LinearLayout.LayoutParams(dp(48), dp(56))
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
        val whitelist = (prefs.getString(GuardianApp.KEY_WHITELIST, "") ?: "")
            .split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val holdMs = prefs.getLong(GuardianApp.KEY_TOUCH_HOLD_MS, GuardianApp.DEFAULT_HOLD_MS)
        for (pkg in whitelist) {
            if (!isInstalled(pkg)) continue
            grid.addView(makeAppTile(pkg, holdMs))
        }
        if (grid.childCount == 0) {
            grid.addView(TextView(this).apply {
                text = "לחץ לחיצה ארוכה על ... להגדרות"
                textSize = 16f
                setTextColor(TEXT_SEC)
                gravity = Gravity.CENTER
                setPadding(dp(20), dp(60), dp(20), dp(60))
            })
        }
    }

    private fun makeAppTile(pkg: String, holdMs: Long): View {
        val pm = packageManager
        val appInfo = try { pm.getApplicationInfo(pkg, 0) } catch (_: Exception) { null }
        val label = try { pm.getApplicationLabel(appInfo!!).toString() } catch (_: Exception) {
            pkg.substringAfterLast('.')
        }
        val icon = try { pm.getApplicationIcon(pkg) } catch (_: Exception) { null }

        val cell = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(16), dp(8), dp(16))
            val bg = GradientDrawable().apply {
                setColor(CARD_WHITE)
                cornerRadius = dp(20).toFloat()
                setStroke(dp(1), BAR_BORDER)
            }
            background = bg
            elevation = dp(4).toFloat()
            val spec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            layoutParams = GridLayout.LayoutParams(spec, spec).apply {
                width = 0; height = WRAP
                setMargins(dp(6), dp(6), dp(6), dp(6))
            }
        }

        // App icon
        val iv = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(64), dp(64)).apply {
                gravity = Gravity.CENTER
                bottomMargin = dp(8)
            }
        }
        if (icon != null) {
            iv.setImageDrawable(icon)
        }
        cell.addView(iv)

        // App label
        cell.addView(TextView(this).apply {
            text = label
            textSize = 16f
            setTextColor(TEXT_DARK)
            gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            maxLines = 2
        })

        // Touch guard
        var holdRunnable: Runnable? = null
        cell.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.alpha = 0.6f
                    v.scaleX = 0.95f; v.scaleY = 0.95f
                    holdRunnable = Runnable {
                        v.alpha = 1f; v.scaleX = 1f; v.scaleY = 1f
                        launchApp(pkg)
                    }
                    handler.postDelayed(holdRunnable!!, holdMs)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    holdRunnable?.let { handler.removeCallbacks(it) }
                    v.alpha = 1f; v.scaleX = 1f; v.scaleY = 1f
                    true
                }
                else -> false
            }
        }
        return cell
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
                btnFlash.text = if (flashlightOn) "פנס ON" else "פנס"
                val bg = btnFlash.background as? GradientDrawable
                if (flashlightOn) {
                    bg?.setColor(ACCENT_AMBER)
                    btnFlash.setTextColor(0xFFFFFFFF.toInt())
                } else {
                    bg?.setColor(0xFFF5EDE0.toInt())
                    btnFlash.setTextColor(TEXT_DARK)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("Launcher", "Flashlight error: ${e.message}")
        }
    }

    // ---- Front flash (software white screen) ----
    private fun toggleFrontFlash() {
        if (frontFlashOn) {
            // Turn off
            try {
                frontFlashOverlay?.let { ov ->
                    (getSystemService(Context.WINDOW_SERVICE) as WindowManager).removeView(ov)
                }
            } catch (_: Exception) {}
            frontFlashOverlay = null
            frontFlashOn = false
            // Restore brightness
            val lp = window.attributes
            lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            window.attributes = lp
            val bg = btnFrontFlash.background as? GradientDrawable
            bg?.setColor(0xFFF5EDE0.toInt())
            btnFrontFlash.setTextColor(TEXT_DARK)
        } else {
            // Turn on
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                !Settings.canDrawOverlays(this)) {
                try {
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")))
                } catch (_: Exception) {}
                return
            }
            val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val overlay = View(this).apply {
                setBackgroundColor(0xFFFFFFFF.toInt())
                setOnClickListener { toggleFrontFlash() }  // tap to dismiss
            }
            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                screenBrightness = 1.0f
            }
            wm.addView(overlay, params)
            frontFlashOverlay = overlay
            frontFlashOn = true
            // Also max window brightness
            val lp = window.attributes
            lp.screenBrightness = 1.0f
            window.attributes = lp
            val bg = btnFrontFlash.background as? GradientDrawable
            bg?.setColor(ACCENT_AMBER)
            btnFrontFlash.setTextColor(0xFFFFFFFF.toInt())
        }
    }

    // ---- Clock ----
    private fun updateClock() {
        val c = Calendar.getInstance()
        tvTime.text = "%02d:%02d".format(
            c.get(Calendar.HOUR_OF_DAY),
            c.get(Calendar.MINUTE))
        val dow = c.get(Calendar.DAY_OF_WEEK) - 1  // 0=Sunday
        tvDate.text = "יום ${hebrewDays[dow]}"
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
        // Clean up front flash if active
        if (frontFlashOn) {
            try {
                frontFlashOverlay?.let {
                    (getSystemService(Context.WINDOW_SERVICE) as WindowManager).removeView(it)
                }
            } catch (_: Exception) {}
        }
        super.onDestroy()
    }
}
