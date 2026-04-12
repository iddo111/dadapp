package com.family.guardian.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.app.NotificationCompat
import com.family.guardian.GuardianApp
import com.family.guardian.ui.LauncherActivity

class GuardianService : AccessibilityService() {

    companion object {
        const val TAG = "GuardianService"

        val SCAM_PHRASES = listOf(
            "virus detected", "your phone is infected", "speed up now",
            "clean memory", "battery damaged", "phone has virus",
            "security alert", "click to clean", "ram booster",
            "phone cleaner", "speed booster", "junk files found",
            "remove virus", "scan now", "your device is at risk",
            "critical alert", "memory full", "performance issue",
            "וירוס זוהה",
            "הטלפון שלך נגוע",
            "נקה עכשיו",
            "אזהרת אבטחה",
            "הסר וירוס"
        )

        val INSTALLER_PACKAGES = listOf(
            "com.android.packageinstaller",
            "com.google.android.packageinstaller",
            "com.samsung.android.packageinstaller"
        )

        val DISMISS_LABELS = listOf(
            "close", "dismiss", "cancel", "ok", "no thanks",
            "סגור", "ביטול",
            "לא תודה", "אישור"
        )

        private var lastRedirectTime = 0L
        private const val REDIRECT_COOLDOWN_MS = 2000L

        fun start(context: Context) {
            context.startForegroundService(
                Intent(context, GuardianNotificationService::class.java))
        }
    }

    private val prefs by lazy { getSharedPreferences(GuardianApp.PREFS, Context.MODE_PRIVATE) }

    override fun onServiceConnected() {
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                         AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                         AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
        }
        serviceInfo = info
        Log.d(TAG, "Guardian accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                handleWindowChange(pkg, event)
            }
        }
    }

    private fun handleWindowChange(pkg: String, event: AccessibilityEvent) {
        // 1. Block package installer
        if (INSTALLER_PACKAGES.any { pkg.startsWith(it) }) {
            val trustedInstaller = prefs.getString(GuardianApp.KEY_TRUSTED_INSTALLER, "") ?: ""
            if (trustedInstaller.isEmpty()) {
                Log.d(TAG, "Blocking package installer from $pkg")
                blockInstaller()
                return
            }
        }

        // 2. Whitelist enforcement + blacklist auto-uninstall
        val whitelist = getWhitelist()
        if (whitelist.isNotEmpty() && !isSystemApp(pkg) && !isWhitelisted(pkg, whitelist)) {
            if (pkg != packageName) {
                val now = System.currentTimeMillis()
                if (now - lastRedirectTime > REDIRECT_COOLDOWN_MS) {
                    lastRedirectTime = now
                    Log.d(TAG, "Non-whitelisted app foreground: $pkg")
                    goHome()
                    // Auto-uninstall if on blacklist
                    if (isBlacklisted(pkg)) {
                        uninstallPackage(pkg)
                    }
                }
                return
            }
        }

        // 3. Scam popup detection
        val root = rootInActiveWindow ?: return
        if (containsScamContent(root)) {
            Log.d(TAG, "SCAM POPUP detected in $pkg")
            dismissScamPopup(root, pkg)
        }
    }

    private fun containsScamContent(node: AccessibilityNodeInfo): Boolean {
        val text = extractAllText(node).lowercase()
        return SCAM_PHRASES.any { phrase -> text.contains(phrase.lowercase()) }
    }

    private fun extractAllText(node: AccessibilityNodeInfo): String {
        val sb = StringBuilder()
        try {
            node.text?.let { sb.append(it) }
            node.contentDescription?.let { sb.append(" ").append(it) }
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                sb.append(" ").append(extractAllText(child))
                child.recycle()
            }
        } catch (_: Exception) {}
        return sb.toString()
    }

    private fun dismissScamPopup(root: AccessibilityNodeInfo, pkg: String) {
        var dismissed = false
        for (label in DISMISS_LABELS) {
            val nodes = root.findAccessibilityNodeInfosByText(label)
            for (node in nodes) {
                if (node.isClickable) {
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    dismissed = true
                    Log.d(TAG, "Clicked dismiss button: $label")
                    break
                }
                node.recycle()
            }
            if (dismissed) break
        }
        if (!dismissed) {
            performGlobalAction(GLOBAL_ACTION_BACK)
            goHome()
        }
        try {
            val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
            am.killBackgroundProcesses(pkg)
        } catch (_: Exception) {}
    }

    private fun blockInstaller() {
        performGlobalAction(GLOBAL_ACTION_BACK)
        performGlobalAction(GLOBAL_ACTION_BACK)
        goHome()
    }

    private fun goHome() {
        val intent = Intent(this, LauncherActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
    }

    private fun uninstallPackage(pkg: String) {
        try {
            val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            Log.d(TAG, "Launched uninstall dialog for $pkg")
        } catch (e: Exception) {
            Log.e(TAG, "Uninstall failed for $pkg: ${e.message}")
        }
    }

    private fun getWhitelist(): List<String> {
        val raw = prefs.getString(GuardianApp.KEY_WHITELIST, "") ?: ""
        return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    private fun getBlacklist(): List<String> {
        val raw = prefs.getString(GuardianApp.KEY_BLACKLIST, "") ?: ""
        return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    private fun isWhitelisted(pkg: String, whitelist: List<String>): Boolean {
        return whitelist.any { pkg == it || pkg.startsWith(it) }
    }

    private fun isBlacklisted(pkg: String): Boolean {
        return getBlacklist().any { pkg == it || pkg.startsWith(it) }
    }

    private fun isSystemApp(pkg: String): Boolean {
        return try {
            val ai = packageManager.getApplicationInfo(pkg, 0)
            (ai.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
        } catch (_: Exception) { false }
    }

    override fun onInterrupt() {}
}

/**
 * Foreground notification service -- keeps the process alive.
 */
class GuardianNotificationService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val pi = PendingIntent.getActivity(this, 0,
            Intent(this, LauncherActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, GuardianApp.CHANNEL_GUARDIAN)
            .setContentTitle("Family Guardian")
            .setContentText("פעיל ומגן")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
        startForeground(2, notification)
        return START_STICKY
    }
    override fun onBind(intent: Intent?) = null
}
