#!/usr/bin/env python3
"""
generate_dadapp.py -- Family Guardian v2.0
Generates the complete Android project at E:\\DADAPP\\
Run:  py -3.12 generate_dadapp.py
"""
import os, sys, textwrap, shutil

BASE = r"E:\DADAPP"

def w(rel, content):
    full = os.path.join(BASE, rel.replace("/", os.sep))
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with open(full, "w", encoding="utf-8", newline="\n") as f:
        f.write(textwrap.dedent(content).lstrip("\n"))
    print("  " + rel)

# ======================================================================
#  MAIN
# ======================================================================
def main():
    print("=" * 60)
    print("  Family Guardian v2.0 -- Project Generator")
    print("  Target: " + BASE)
    print("=" * 60)
    print()

    # Clean old generated sources (preserves gradle-wrapper.jar, local.properties, .gradle)
    for subdir in [
        os.path.join(BASE, "app", "src"),
        os.path.join(BASE, "app", "build"),
    ]:
        if os.path.isdir(subdir):
            shutil.rmtree(subdir)
            print("  [cleanup] removed " + subdir)

    gen_gradle()
    gen_manifest()
    gen_resources()
    gen_guardian_app()
    gen_launcher_activity()
    gen_admin_activity()
    gen_sos_activity()
    gen_camera_helper()
    gen_service()
    gen_boot_receiver()
    gen_device_admin()
    gen_build_scripts()

    print()
    print("Generation complete.  Next steps:")
    print("  1.  cd E:\\DADAPP")
    print("  2.  powershell -ExecutionPolicy Bypass -File build_dadapp.ps1")
    print("  3.  install_dadapp.bat")

# ======================================================================
#  GRADLE FILES
# ======================================================================
def gen_gradle():
    print("[gradle]")

    w("settings.gradle.kts", """
    pluginManagement {
        repositories { google(); mavenCentral(); gradlePluginPortal() }
    }
    dependencyResolutionManagement {
        repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
        repositories { google(); mavenCentral() }
    }
    rootProject.name = "DADAPP"
    include(":app")
    """)

    w("build.gradle.kts", """
    plugins {
        id("com.android.application") version "8.5.2" apply false
        id("org.jetbrains.kotlin.android") version "2.1.0" apply false
    }
    """)

    w("gradle.properties", """
    org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
    android.useAndroidX=true
    kotlin.code.style=official
    android.suppressUnsupportedCompileSdk=35
    """)

    w("gradle/wrapper/gradle-wrapper.properties", """
    distributionBase=GRADLE_USER_HOME
    distributionPath=wrapper/dists
    distributionUrl=https\\://services.gradle.org/distributions/gradle-8.9-bin.zip
    zipStoreBase=GRADLE_USER_HOME
    zipStorePath=wrapper/dists
    """)

    # gradlew.bat
    w("gradlew.bat", """\
    @rem Gradle startup script for Windows
    @if "%DEBUG%"=="" @echo off
    setlocal
    set DIRNAME=%~dp0
    if "%DIRNAME%"=="" set DIRNAME=.
    set APP_BASE_NAME=%~n0
    set APP_HOME=%DIRNAME%
    set CLASSPATH=%APP_HOME%\\gradle\\wrapper\\gradle-wrapper.jar
    "%JAVA_HOME%\\bin\\java.exe" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
    """)

    w("app/build.gradle.kts", """
    plugins {
        id("com.android.application")
        id("org.jetbrains.kotlin.android")
    }
    android {
        namespace = "com.family.guardian"
        compileSdk = 35
        defaultConfig {
            applicationId = "com.family.guardian"
            minSdk = 26
            targetSdk = 35
            versionCode = 2
            versionName = "2.0.0"
        }
        buildTypes { debug { isDebuggable = true } }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
        kotlinOptions { jvmTarget = "17" }
        packaging { resources { excludes += "META-INF/**" } }
    }
    dependencies {
        implementation("androidx.core:core-ktx:1.13.1")
        implementation("androidx.appcompat:appcompat:1.7.0")
        implementation("com.google.android.material:material:1.12.0")
    }
    """)

# ======================================================================
#  MANIFEST
# ======================================================================
def gen_manifest():
    print("[manifest]")
    w("app/src/main/AndroidManifest.xml", """
    <?xml version="1.0" encoding="utf-8"?>
    <manifest xmlns:android="http://schemas.android.com/apk/res/android">

        <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>
        <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW"/>
        <uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
        <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE"/>
        <uses-permission android:name="android.permission.FOREGROUND_SERVICE_CAMERA"/>
        <uses-permission android:name="android.permission.QUERY_ALL_PACKAGES"/>
        <uses-permission android:name="android.permission.VIBRATE"/>
        <uses-permission android:name="android.permission.SEND_SMS"/>
        <uses-permission android:name="android.permission.CALL_PHONE"/>
        <uses-permission android:name="android.permission.CAMERA"/>
        <uses-permission android:name="android.permission.READ_CONTACTS"/>
        <uses-permission android:name="android.permission.FLASHLIGHT"/>
        <uses-permission android:name="android.permission.RECEIVE_MMS"/>
        <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
            android:maxSdkVersion="28"/>

        <uses-feature android:name="android.hardware.camera" android:required="false"/>
        <uses-feature android:name="android.hardware.camera.autofocus" android:required="false"/>

        <application
            android:name=".GuardianApp"
            android:allowBackup="false"
            android:icon="@android:mipmap/sym_def_app_icon"
            android:label="Family Guardian"
            android:theme="@style/Theme.Guardian">

            <activity
                android:name=".ui.LauncherActivity"
                android:exported="true"
                android:launchMode="singleTask"
                android:screenOrientation="portrait"
                android:stateNotNeeded="true">
                <intent-filter android:priority="999">
                    <action android:name="android.intent.action.MAIN"/>
                    <category android:name="android.intent.category.HOME"/>
                    <category android:name="android.intent.category.DEFAULT"/>
                    <category android:name="android.intent.category.LAUNCHER"/>
                </intent-filter>
            </activity>

            <activity android:name=".ui.AdminActivity"
                android:exported="false" android:screenOrientation="portrait"/>

            <activity android:name=".ui.SosActivity"
                android:exported="false"
                android:showWhenLocked="true"
                android:turnScreenOn="true"
                android:screenOrientation="portrait"/>

            <service android:name=".service.GuardianService"
                android:exported="false"
                android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
                <intent-filter>
                    <action android:name="android.accessibilityservice.AccessibilityService"/>
                </intent-filter>
                <meta-data
                    android:name="android.accessibilityservice"
                    android:resource="@xml/accessibility_service_config"/>
            </service>

            <service android:name=".service.GuardianNotificationService"
                android:exported="false"
                android:foregroundServiceType="specialUse|camera"/>

            <receiver android:name=".receiver.BootReceiver" android:exported="true">
                <intent-filter android:priority="999">
                    <action android:name="android.intent.action.BOOT_COMPLETED"/>
                    <action android:name="android.intent.action.QUICKBOOT_POWERON"/>
                </intent-filter>
            </receiver>

            <receiver android:name=".admin.GuardianDeviceAdmin"
                android:exported="true"
                android:permission="android.permission.BIND_DEVICE_ADMIN">
                <meta-data
                    android:name="android.app.device_admin"
                    android:resource="@xml/device_admin"/>
                <intent-filter>
                    <action android:name="android.app.action.DEVICE_ADMIN_ENABLED"/>
                </intent-filter>
            </receiver>

            <provider
                android:name="androidx.core.content.FileProvider"
                android:authorities="com.family.guardian.fileprovider"
                android:exported="false"
                android:grantUriPermissions="true">
                <meta-data
                    android:name="android.support.FILE_PROVIDER_PATHS"
                    android:resource="@xml/file_paths"/>
            </provider>

        </application>
    </manifest>
    """)

# ======================================================================
#  RESOURCES
# ======================================================================
def gen_resources():
    print("[resources]")
    RES = "app/src/main/res"

    w(RES + "/values/colors.xml", """
    <?xml version="1.0" encoding="utf-8"?>
    <resources>
        <color name="bg_warm">#FFF8F0</color>
        <color name="bg_card">#FFFFFFFF</color>
        <color name="text_dark">#2D1A0A</color>
        <color name="text_secondary">#6B5B4F</color>
        <color name="accent_amber">#E88C00</color>
        <color name="sos_red">#CC2200</color>
        <color name="bar_border">#E8DDD0</color>
        <color name="card_shadow">#22000000</color>
    </resources>
    """)

    w(RES + "/values/themes.xml", """
    <?xml version="1.0" encoding="utf-8"?>
    <resources>
        <style name="Theme.Guardian" parent="Theme.MaterialComponents.Light.NoActionBar">
            <item name="colorPrimary">#E88C00</item>
            <item name="colorPrimaryDark">#C77800</item>
            <item name="colorAccent">#E88C00</item>
            <item name="android:windowBackground">#FFF8F0</item>
            <item name="android:statusBarColor">#FFF8F0</item>
            <item name="android:navigationBarColor">#FFF8F0</item>
            <item name="android:windowLightStatusBar">true</item>
            <item name="android:windowLightNavigationBar">true</item>
        </style>
    </resources>
    """)

    w(RES + "/values/strings.xml", """
    <?xml version="1.0" encoding="utf-8"?>
    <resources>
        <string name="app_name">Family Guardian</string>
        <string name="accessibility_description">Protects against fake popup ads and accidental touches</string>
    </resources>
    """)

    w(RES + "/values/dimens.xml", """
    <?xml version="1.0" encoding="utf-8"?>
    <resources>
        <dimen name="app_cell_size">120dp</dimen>
        <dimen name="icon_size">72dp</dimen>
        <dimen name="tile_radius">20dp</dimen>
    </resources>
    """)

    # App tile background -- light card with warm shadow
    w(RES + "/drawable/app_button_bg.xml", """
    <?xml version="1.0" encoding="utf-8"?>
    <shape xmlns:android="http://schemas.android.com/apk/res/android"
        android:shape="rectangle">
        <solid android:color="#FFFFFFFF"/>
        <corners android:radius="20dp"/>
        <stroke android:width="1dp" android:color="#E8DDD0"/>
    </shape>
    """)

    # SOS button background
    w(RES + "/drawable/sos_button_bg.xml", """
    <?xml version="1.0" encoding="utf-8"?>
    <shape xmlns:android="http://schemas.android.com/apk/res/android"
        android:shape="rectangle">
        <solid android:color="#CC2200"/>
        <corners android:radius="16dp"/>
        <stroke android:width="3dp" android:color="#E88C00"/>
    </shape>
    """)

    # Bottom bar button background
    w(RES + "/drawable/bar_button_bg.xml", """
    <?xml version="1.0" encoding="utf-8"?>
    <shape xmlns:android="http://schemas.android.com/apk/res/android"
        android:shape="rectangle">
        <solid android:color="#F5EDE0"/>
        <corners android:radius="14dp"/>
        <stroke android:width="1dp" android:color="#E8DDD0"/>
    </shape>
    """)

    w(RES + "/xml/accessibility_service_config.xml", """
    <?xml version="1.0" encoding="utf-8"?>
    <accessibility-service
        xmlns:android="http://schemas.android.com/apk/res/android"
        android:description="@string/accessibility_description"
        android:accessibilityEventTypes="typeWindowStateChanged|typeWindowContentChanged"
        android:accessibilityFeedbackType="feedbackGeneric"
        android:accessibilityFlags="flagReportViewIds|flagRetrieveInteractiveWindows"
        android:canRetrieveWindowContent="true"
        android:notificationTimeout="100"
        android:settingsActivity="com.family.guardian.ui.AdminActivity"/>
    """)

    w(RES + "/xml/device_admin.xml", """
    <?xml version="1.0" encoding="utf-8"?>
    <device-admin xmlns:android="http://schemas.android.com/apk/res/android">
        <uses-policies>
            <limit-password/>
            <watch-login/>
            <force-lock/>
        </uses-policies>
    </device-admin>
    """)

    w(RES + "/xml/file_paths.xml", """
    <?xml version="1.0" encoding="utf-8"?>
    <paths>
        <cache-path name="sos_photos" path="sos_photos/"/>
        <files-path name="mms_pdu" path="mms_pdu/"/>
    </paths>
    """)

    # ---- Layouts ----

    # Launcher uses programmatic layout -- no XML needed
    # But we keep a minimal one for reference (not used)

    # Admin layout -- light theme
    w(RES + "/layout/activity_admin.xml", """
    <?xml version="1.0" encoding="utf-8"?>
    <ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="#FFF8F0">

        <LinearLayout
            android:id="@+id/layout_admin_root"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="24dp"
            android:visibility="invisible">

            <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
                android:text="\u05d4\u05d2\u05d3\u05e8\u05d5\u05ea \u05de\u05e0\u05d4\u05dc"
                android:textSize="24sp" android:textStyle="bold"
                android:textColor="#2D1A0A" android:layout_marginBottom="28dp"
                android:gravity="end"/>

            <!-- SOS Contact -->
            <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
                android:text="\u05d0\u05d9\u05e9 \u05e7\u05e9\u05e8 \u05dc\u05de\u05e6\u05d5\u05e7\u05d4"
                android:textSize="14sp" android:textColor="#6B5B4F"
                android:layout_marginBottom="4dp" android:gravity="end"/>
            <TextView android:id="@+id/tv_sos_contact_display"
                android:layout_width="match_parent" android:layout_height="wrap_content"
                android:text="\u05dc\u05d0 \u05e0\u05d1\u05d7\u05e8"
                android:textSize="18sp" android:textColor="#2D1A0A"
                android:background="#F5EDE0" android:padding="14dp"
                android:gravity="end" android:layout_marginBottom="8dp"/>
            <Button android:id="@+id/btn_pick_contact"
                android:layout_width="match_parent" android:layout_height="52dp"
                android:text="\u05d1\u05d7\u05e8 \u05de\u05d0\u05e0\u05e9\u05d9 \u05e7\u05e9\u05e8"
                android:textSize="16sp" android:textColor="#FFFFFF"
                android:backgroundTint="#E88C00"
                android:layout_marginBottom="24dp"/>

            <!-- Touch hold duration -->
            <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
                android:text="\u05de\u05e9\u05da \u05dc\u05d7\u05d9\u05e6\u05d4 \u05dc\u05e4\u05ea\u05d9\u05d7\u05ea \u05d0\u05e4\u05dc\u05d9\u05e7\u05e6\u05d9\u05d4"
                android:textSize="14sp" android:textColor="#6B5B4F"
                android:layout_marginBottom="4dp" android:gravity="end"/>
            <TextView android:id="@+id/tv_hold_duration"
                android:layout_width="match_parent" android:layout_height="wrap_content"
                android:textSize="16sp" android:textColor="#E88C00"
                android:gravity="end" android:layout_marginBottom="4dp"/>
            <SeekBar android:id="@+id/sb_hold_duration"
                android:layout_width="match_parent" android:layout_height="wrap_content"
                android:layout_marginBottom="24dp"/>

            <!-- App Whitelist Picker -->
            <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
                android:text="\u05d0\u05e4\u05dc\u05d9\u05e7\u05e6\u05d9\u05d5\u05ea \u05de\u05d5\u05e8\u05e9\u05d5\u05ea"
                android:textSize="14sp" android:textColor="#6B5B4F"
                android:layout_marginBottom="4dp" android:gravity="end"/>
            <TextView android:id="@+id/tv_whitelist_count"
                android:layout_width="match_parent" android:layout_height="wrap_content"
                android:textSize="16sp" android:textColor="#2D1A0A"
                android:gravity="end" android:layout_marginBottom="8dp"/>
            <Button android:id="@+id/btn_pick_apps"
                android:layout_width="match_parent" android:layout_height="52dp"
                android:text="\u05d1\u05d7\u05e8 \u05d0\u05e4\u05dc\u05d9\u05e7\u05e6\u05d9\u05d5\u05ea"
                android:textSize="16sp" android:textColor="#FFFFFF"
                android:backgroundTint="#E88C00"
                android:layout_marginBottom="24dp"/>

            <!-- Blacklist -->
            <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
                android:text="\u05e8\u05e9\u05d9\u05de\u05d4 \u05e9\u05d7\u05d5\u05e8\u05d4 (\u05d4\u05e1\u05e8\u05d4 \u05d0\u05d5\u05d8\u05d5\u05de\u05d8\u05d9\u05ea)"
                android:textSize="14sp" android:textColor="#6B5B4F"
                android:layout_marginBottom="4dp" android:gravity="end"/>
            <EditText android:id="@+id/et_blacklist"
                android:layout_width="match_parent" android:layout_height="100dp"
                android:gravity="top|end" android:textColor="#2D1A0A"
                android:background="#F5EDE0" android:textSize="12sp"
                android:inputType="textMultiLine" android:padding="12dp"
                android:hint="com.example.scam,com.junk.app"
                android:layout_marginBottom="24dp"/>

            <!-- Trusted Installer -->
            <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
                android:text="Trusted installer package"
                android:textSize="14sp" android:textColor="#6B5B4F"
                android:layout_marginBottom="4dp" android:gravity="end"/>
            <EditText android:id="@+id/et_trusted_installer"
                android:layout_width="match_parent" android:layout_height="wrap_content"
                android:textColor="#2D1A0A" android:background="#F5EDE0"
                android:textSize="14sp" android:padding="12dp"
                android:hint="e.g. com.android.shell"
                android:layout_marginBottom="24dp"/>

            <!-- Blue light filter -->
            <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
                android:text="\u05e1\u05d9\u05e0\u05d5\u05df \u05d0\u05d5\u05e8 \u05db\u05d7\u05d5\u05dc"
                android:textSize="14sp" android:textColor="#6B5B4F"
                android:layout_marginBottom="4dp" android:gravity="end"/>
            <TextView android:id="@+id/tv_blue_light"
                android:layout_width="match_parent" android:layout_height="wrap_content"
                android:textSize="16sp" android:textColor="#E88C00"
                android:gravity="end" android:layout_marginBottom="4dp"/>
            <SeekBar android:id="@+id/sb_blue_light"
                android:layout_width="match_parent" android:layout_height="wrap_content"
                android:layout_marginBottom="24dp"/>

            <!-- Admin PIN -->
            <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
                android:text="\u05e7\u05d5\u05d3 \u05d2\u05d9\u05e9\u05d4"
                android:textSize="14sp" android:textColor="#6B5B4F"
                android:layout_marginBottom="4dp" android:gravity="end"/>
            <EditText android:id="@+id/et_new_pin"
                android:layout_width="match_parent" android:layout_height="wrap_content"
                android:inputType="numberPassword" android:textColor="#2D1A0A"
                android:background="#F5EDE0" android:textSize="20sp"
                android:padding="12dp" android:gravity="end"
                android:layout_marginBottom="32dp"/>

            <!-- Buttons -->
            <LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
                android:orientation="horizontal">
                <Button android:id="@+id/btn_cancel_admin"
                    android:layout_width="0dp" android:layout_height="56dp"
                    android:layout_weight="1"
                    android:text="\u05d1\u05d9\u05d8\u05d5\u05dc"
                    android:backgroundTint="#E8DDD0" android:textColor="#6B5B4F"
                    android:layout_marginEnd="8dp"/>
                <Button android:id="@+id/btn_save_admin"
                    android:layout_width="0dp" android:layout_height="56dp"
                    android:layout_weight="2"
                    android:text="\u05e9\u05de\u05d5\u05e8 \u05d4\u05d2\u05d3\u05e8\u05d5\u05ea"
                    android:textStyle="bold" android:backgroundTint="#E88C00"
                    android:textColor="#FFFFFF"/>
            </LinearLayout>
        </LinearLayout>
    </ScrollView>
    """)

    # SOS layout -- light theme
    w(RES + "/layout/activity_sos.xml", """
    <?xml version="1.0" encoding="utf-8"?>
    <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical"
        android:gravity="center"
        android:background="#FFF0E8"
        android:padding="32dp">

        <TextView android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:text="SOS" android:textSize="72sp" android:textStyle="bold"
            android:textColor="#CC2200" android:layout_marginBottom="8dp"/>

        <TextView android:id="@+id/tv_sos_contact_name"
            android:layout_width="match_parent" android:layout_height="wrap_content"
            android:textSize="22sp" android:textColor="#2D1A0A"
            android:gravity="center" android:layout_marginBottom="8dp"/>

        <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
            android:text="\u05dc\u05d7\u05e5 \u05dc\u05d4\u05ea\u05e7\u05e9\u05e8 \u05d5\u05dc\u05e9\u05dc\u05d5\u05d7 \u05d4\u05d5\u05d3\u05e2\u05d4"
            android:textSize="18sp" android:textColor="#6B5B4F"
            android:gravity="center" android:layout_marginBottom="40dp"/>

        <TextView android:id="@+id/tv_sos_countdown"
            android:layout_width="wrap_content" android:layout_height="wrap_content"
            android:text="3" android:textSize="96sp" android:textStyle="bold"
            android:textColor="#CC2200" android:fontFamily="monospace"
            android:visibility="gone" android:layout_marginBottom="32dp"/>

        <TextView android:id="@+id/tv_sos_status"
            android:layout_width="match_parent" android:layout_height="wrap_content"
            android:textSize="16sp" android:textColor="#6B5B4F"
            android:gravity="center" android:visibility="gone"
            android:layout_marginBottom="16dp"/>

        <Button android:id="@+id/btn_sos_confirm"
            android:layout_width="260dp" android:layout_height="90dp"
            android:text="\u05e7\u05e8\u05d9\u05d0\u05d4 \u05dc\u05e2\u05d6\u05e8\u05d4"
            android:textSize="22sp" android:textStyle="bold"
            android:textColor="#FFFFFF" android:backgroundTint="#CC2200"
            android:layout_marginBottom="20dp"/>

        <Button android:id="@+id/btn_sos_cancel"
            android:layout_width="200dp" android:layout_height="52dp"
            android:text="\u05d1\u05d9\u05d8\u05d5\u05dc"
            android:textSize="18sp"
            android:backgroundTint="#E8DDD0" android:textColor="#6B5B4F"/>
    </LinearLayout>
    """)

    # Launcher layout -- not used (built programmatically), but keep empty
    w(RES + "/layout/activity_launcher.xml", """
    <?xml version="1.0" encoding="utf-8"?>
    <FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="#FFF8F0"/>
    """)


# ======================================================================
#  GUARDIAN APP (Application class)
# ======================================================================
def gen_guardian_app():
    print("[GuardianApp]")
    SRC = "app/src/main/java/com/family/guardian"
    w(SRC + "/GuardianApp.kt", """
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
            const val KEY_TOUCH_HOLD_MS = "touch_hold_ms"
            const val KEY_SETUP_DONE = "setup_done"
            const val KEY_BLUE_LIGHT = "blue_light_strength"
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
    """)


# ======================================================================
#  LAUNCHER ACTIVITY
# ======================================================================
def gen_launcher_activity():
    print("[LauncherActivity]")
    SRC = "app/src/main/java/com/family/guardian"

    # Hebrew day names encoded as \\uXXXX for the Kotlin source
    # (Python interprets \\u as literal \u in the output file,
    #  which Kotlin then interprets as Unicode.  But we actually
    #  want the Hebrew chars to appear literally in the .kt file
    #  so we use the Python \u escapes which Python resolves when
    #  writing UTF-8.)

    w(SRC + "/ui/LauncherActivity.kt", """
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
            "\u05e8\u05d0\u05e9\u05d5\u05df",   // Sunday
            "\u05e9\u05e0\u05d9",               // Monday
            "\u05e9\u05dc\u05d9\u05e9\u05d9",   // Tuesday
            "\u05e8\u05d1\u05d9\u05e2\u05d9",   // Wednesday
            "\u05d7\u05de\u05d9\u05e9\u05d9",   // Thursday
            "\u05e9\u05d9\u05e9\u05d9",         // Friday
            "\u05e9\u05d1\u05ea"                // Saturday
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
                text = "\u05e9\u05dc\u05d5\u05dd \u05d0\u05d1\u05d0"
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
                text = "\u05e4\u05e0\u05e1"
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
                text = "\u05e4\u05e0\u05e1 ++"
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
                    text = "\u05dc\u05d7\u05e5 \u05dc\u05d7\u05d9\u05e6\u05d4 \u05d0\u05e8\u05d5\u05db\u05d4 \u05e2\u05dc ... \u05dc\u05d4\u05d2\u05d3\u05e8\u05d5\u05ea"
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
                    btnFlash.text = if (flashlightOn) "\u05e4\u05e0\u05e1 ON" else "\u05e4\u05e0\u05e1"
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
            tvDate.text = "\u05d9\u05d5\u05dd ${hebrewDays[dow]}"
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
    """)


# ======================================================================
#  ADMIN ACTIVITY
# ======================================================================
def gen_admin_activity():
    print("[AdminActivity]")
    SRC = "app/src/main/java/com/family/guardian"

    w(SRC + "/ui/AdminActivity.kt", """
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
                .setTitle("\u05d4\u05d2\u05d3\u05e8\u05d5\u05ea \u05de\u05e0\u05d4\u05dc")
                .setMessage("\u05d4\u05db\u05e0\u05e1 \u05e7\u05d5\u05d3 \u05d2\u05d9\u05e9\u05d4")
                .setView(input)
                .setPositiveButton("\u05d0\u05d9\u05e9\u05d5\u05e8") { _, _ ->
                    val entered = input.text.toString()
                    val saved = prefs.getString(GuardianApp.KEY_ADMIN_PIN, GuardianApp.DEFAULT_PIN)
                    if (entered == saved) {
                        pinVerified = true
                        loadSettings()
                    } else {
                        Toast.makeText(this, "\u05e7\u05d5\u05d3 \u05e9\u05d2\u05d5\u05d9",
                            Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
                .setNegativeButton("\u05d1\u05d9\u05d8\u05d5\u05dc") { _, _ -> finish() }
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
                Toast.makeText(this, "\u05d4\u05d2\u05d3\u05e8\u05d5\u05ea \u05e0\u05e9\u05de\u05e8\u05d5",
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
                tv.text = "$name\\n$number"
            } else if (number.isNotEmpty()) {
                tv.text = number
            } else {
                tv.text = "\u05dc\u05d0 \u05e0\u05d1\u05d7\u05e8"
            }
        }

        private fun updateWhitelistCount(tv: TextView) {
            val whitelist = (prefs.getString(GuardianApp.KEY_WHITELIST, "") ?: "")
                .split(",").map { it.trim() }.filter { it.isNotEmpty() }
            tv.text = "${whitelist.size} \u05d0\u05e4\u05dc\u05d9\u05e7\u05e6\u05d9\u05d5\u05ea \u05e0\u05d1\u05d7\u05e8\u05d5"
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
                .setTitle("\u05d1\u05d7\u05e8 \u05d0\u05e4\u05dc\u05d9\u05e7\u05e6\u05d9\u05d5\u05ea")
                .setMultiChoiceItems(labels, checked) { _, which, isChecked ->
                    checked[which] = isChecked
                }
                .setPositiveButton("\u05e9\u05de\u05d5\u05e8") { _, _ ->
                    val selected = pkgs.filterIndexed { i, _ -> checked[i] }
                    prefs.edit()
                        .putString(GuardianApp.KEY_WHITELIST, selected.joinToString(","))
                        .apply()
                    updateWhitelistCount(tvCount)
                }
                .setNegativeButton("\u05d1\u05d9\u05d8\u05d5\u05dc", null)
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
    """)


# ======================================================================
#  SOS ACTIVITY
# ======================================================================
def gen_sos_activity():
    print("[SosActivity]")
    SRC = "app/src/main/java/com/family/guardian"

    w(SRC + "/ui/SosActivity.kt", """
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
                tvName.text = "\u05de\u05ea\u05e7\u05e9\u05e8 \u05dc: $sosName"
            } else if (sosNumber.isNotEmpty()) {
                tvName.text = "\u05de\u05ea\u05e7\u05e9\u05e8 \u05dc: $sosNumber"
            }

            btnSos.setOnClickListener {
                if (sosNumber.isEmpty()) {
                    Toast.makeText(this,
                        "\u05de\u05e1\u05e4\u05e8 \u05d7\u05d9\u05e8\u05d5\u05dd \u05dc\u05d0 \u05d4\u05d5\u05d2\u05d3\u05e8",
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
            tvStatus.text = "\u05de\u05e6\u05dc\u05dd..."
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
                    tvStatus.text = "\u05e9\u05d5\u05dc\u05d7 \u05d4\u05d5\u05d3\u05e2\u05d4..."
                    try {
                        val sms = SmsManager.getDefault()
                        val msg = "SOS! " +
                            "\u05e2\u05d6\u05e8\u05d4 \u05e0\u05d3\u05e8\u05e9\u05ea. " +
                            "\u05d4\u05d5\u05d3\u05e2\u05d4 \u05d0\u05d5\u05d8\u05d5\u05de\u05d8\u05d9\u05ea."
                        sms.sendTextMessage(number, null, msg, null, null)
                    } catch (e: Exception) {
                        android.util.Log.e("SOS", "SMS failed: ${e.message}")
                    }

                    // 3. Make the call
                    tvStatus.text = "\u05de\u05ea\u05e7\u05e9\u05e8..."
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
    """)


# ======================================================================
#  CAMERA HELPER
# ======================================================================
def gen_camera_helper():
    print("[CameraHelper]")
    SRC = "app/src/main/java/com/family/guardian"

    w(SRC + "/util/CameraHelper.kt", """
    package com.family.guardian.util

    import android.content.Context
    import android.graphics.ImageFormat
    import android.hardware.camera2.*
    import android.media.ImageReader
    import android.os.Handler
    import android.os.HandlerThread
    import android.util.Log
    import java.io.File
    import java.io.FileOutputStream
    import java.util.concurrent.CountDownLatch
    import java.util.concurrent.TimeUnit

    /**
     * Minimal Camera2 helper for SOS photo capture.
     * Captures a single JPEG from either front or rear camera.
     * Blocking call -- must be invoked from a background thread.
     */
    class CameraHelper(private val context: Context) {

        companion object {
            private const val TAG = "CameraHelper"
            private const val WIDTH = 640
            private const val HEIGHT = 480
            private const val TIMEOUT_SEC = 8L
        }

        fun capturePhoto(front: Boolean): File? {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val facing = if (front)
                CameraCharacteristics.LENS_FACING_FRONT
            else
                CameraCharacteristics.LENS_FACING_BACK

            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.LENS_FACING) == facing
            } ?: run {
                Log.w(TAG, "No ${if (front) "front" else "rear"} camera found")
                return null
            }

            val thread = HandlerThread("CamCapture").apply { start() }
            val handler = Handler(thread.looper)

            val imageReader = ImageReader.newInstance(WIDTH, HEIGHT, ImageFormat.JPEG, 1)
            val latchOpen = CountDownLatch(1)
            val latchCapture = CountDownLatch(1)

            var cameraDevice: CameraDevice? = null
            var result: File? = null

            // Set up image callback
            imageReader.setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                try {
                    val buffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    val dir = File(context.cacheDir, "sos_photos")
                    dir.mkdirs()
                    val file = File(dir, "sos_${if (front) "front" else "rear"}_${System.currentTimeMillis()}.jpg")
                    FileOutputStream(file).use { it.write(bytes) }
                    result = file
                    Log.d(TAG, "Photo saved: ${file.absolutePath}")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to save photo: ${e.message}")
                } finally {
                    image.close()
                    latchCapture.countDown()
                }
            }, handler)

            try {
                cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                    override fun onOpened(camera: CameraDevice) {
                        cameraDevice = camera
                        latchOpen.countDown()
                    }
                    override fun onDisconnected(camera: CameraDevice) {
                        camera.close()
                        latchOpen.countDown()
                    }
                    override fun onError(camera: CameraDevice, error: Int) {
                        Log.e(TAG, "Camera open error: $error")
                        camera.close()
                        latchOpen.countDown()
                    }
                }, handler)

                if (!latchOpen.await(TIMEOUT_SEC, TimeUnit.SECONDS)) {
                    Log.e(TAG, "Camera open timed out")
                    thread.quitSafely()
                    return null
                }

                val cam = cameraDevice ?: run {
                    thread.quitSafely()
                    return null
                }

                val latchSession = CountDownLatch(1)
                var captureSession: CameraCaptureSession? = null

                @Suppress("DEPRECATION")
                cam.createCaptureSession(
                    listOf(imageReader.surface),
                    object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(session: CameraCaptureSession) {
                            captureSession = session
                            latchSession.countDown()
                        }
                        override fun onConfigureFailed(session: CameraCaptureSession) {
                            Log.e(TAG, "Session config failed")
                            latchSession.countDown()
                        }
                    },
                    handler
                )

                if (!latchSession.await(TIMEOUT_SEC, TimeUnit.SECONDS)) {
                    Log.e(TAG, "Session config timed out")
                    cam.close()
                    thread.quitSafely()
                    return null
                }

                val session = captureSession ?: run {
                    cam.close()
                    thread.quitSafely()
                    return null
                }

                val captureRequest = cam.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                    addTarget(imageReader.surface)
                    set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
                }.build()

                session.capture(captureRequest, null, handler)

                latchCapture.await(TIMEOUT_SEC, TimeUnit.SECONDS)

                session.close()
                cam.close()

            } catch (e: SecurityException) {
                Log.e(TAG, "Camera permission denied: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Camera error: ${e.message}")
            } finally {
                imageReader.close()
                thread.quitSafely()
            }

            return result
        }
    }
    """)


# ======================================================================
#  GUARDIAN SERVICE + NOTIFICATION SERVICE
# ======================================================================
def gen_service():
    print("[GuardianService]")
    SRC = "app/src/main/java/com/family/guardian"

    w(SRC + "/service/GuardianService.kt", """
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
                "\u05d5\u05d9\u05e8\u05d5\u05e1 \u05d6\u05d5\u05d4\u05d4",
                "\u05d4\u05d8\u05dc\u05e4\u05d5\u05df \u05e9\u05dc\u05da \u05e0\u05d2\u05d5\u05e2",
                "\u05e0\u05e7\u05d4 \u05e2\u05db\u05e9\u05d9\u05d5",
                "\u05d0\u05d6\u05d4\u05e8\u05ea \u05d0\u05d1\u05d8\u05d7\u05d4",
                "\u05d4\u05e1\u05e8 \u05d5\u05d9\u05e8\u05d5\u05e1"
            )

            val INSTALLER_PACKAGES = listOf(
                "com.android.packageinstaller",
                "com.google.android.packageinstaller",
                "com.samsung.android.packageinstaller"
            )

            val DISMISS_LABELS = listOf(
                "close", "dismiss", "cancel", "ok", "no thanks",
                "\u05e1\u05d2\u05d5\u05e8", "\u05d1\u05d9\u05d8\u05d5\u05dc",
                "\u05dc\u05d0 \u05ea\u05d5\u05d3\u05d4", "\u05d0\u05d9\u05e9\u05d5\u05e8"
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
                .setContentText("\u05e4\u05e2\u05d9\u05dc \u05d5\u05de\u05d2\u05df")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setContentIntent(pi)
                .setOngoing(true)
                .build()
            startForeground(2, notification)
            return START_STICKY
        }
        override fun onBind(intent: Intent?) = null
    }
    """)


# ======================================================================
#  BOOT RECEIVER
# ======================================================================
def gen_boot_receiver():
    print("[BootReceiver]")
    SRC = "app/src/main/java/com/family/guardian"

    w(SRC + "/receiver/BootReceiver.kt", """
    package com.family.guardian.receiver

    import android.content.BroadcastReceiver
    import android.content.Context
    import android.content.Intent
    import com.family.guardian.service.GuardianService

    class BootReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            GuardianService.start(context)
        }
    }
    """)


# ======================================================================
#  DEVICE ADMIN
# ======================================================================
def gen_device_admin():
    print("[GuardianDeviceAdmin]")
    SRC = "app/src/main/java/com/family/guardian"

    w(SRC + "/admin/GuardianDeviceAdmin.kt", """
    package com.family.guardian.admin

    import android.app.admin.DeviceAdminReceiver
    import android.content.Context
    import android.content.Intent

    class GuardianDeviceAdmin : DeviceAdminReceiver() {
        override fun onEnabled(context: Context, intent: Intent) {}
        override fun onDisabled(context: Context, intent: Intent) {}
    }
    """)


# ======================================================================
#  BUILD SCRIPTS
# ======================================================================
def gen_build_scripts():
    print("[build scripts]")

    w("build_dadapp.ps1", """\
    # build_dadapp.ps1 -- Build Family Guardian APK
    $ErrorActionPreference = "Stop"

    # Find JAVA_HOME from Android Studio
    $studioLocations = @(
        "$env:ProgramFiles\\Android\\Android Studio",
        "$env:ProgramFiles\\Android\\Android Studio\\jbr",
        "${env:ProgramFiles(x86)}\\Android\\Android Studio",
        "$env:LOCALAPPDATA\\Android\\android-studio"
    )

    # Try Android Studio bundled JBR first
    $javaHome = $null
    foreach ($loc in $studioLocations) {
        $jbrPath = Join-Path $loc "jbr"
        if (Test-Path (Join-Path $jbrPath "bin\\java.exe")) {
            $javaHome = $jbrPath
            break
        }
        if (Test-Path (Join-Path $loc "bin\\java.exe")) {
            $javaHome = $loc
            break
        }
    }
    if (-not $javaHome -and $env:JAVA_HOME) {
        $javaHome = $env:JAVA_HOME
    }
    if (-not $javaHome) {
        Write-Host "ERROR: Cannot find JAVA_HOME. Install Android Studio or set JAVA_HOME." -ForegroundColor Red
        exit 1
    }

    Write-Host "JAVA_HOME = $javaHome" -ForegroundColor Green
    $env:JAVA_HOME = $javaHome

    Push-Location $PSScriptRoot
    try {
        Write-Host "Building Family Guardian v2.0..." -ForegroundColor Cyan
        & .\\gradlew.bat assembleDebug --no-daemon 2>&1 | ForEach-Object { Write-Host $_ }
        if ($LASTEXITCODE -ne 0) {
            Write-Host "BUILD FAILED" -ForegroundColor Red
            exit 1
        }
        $apk = Get-ChildItem -Recurse -Filter "*.apk" -Path "app\\build\\outputs" | Select-Object -First 1
        if ($apk) {
            Write-Host "APK: $($apk.FullName)" -ForegroundColor Green
        } else {
            Write-Host "WARNING: APK not found in expected location" -ForegroundColor Yellow
        }
    } finally {
        Pop-Location
    }
    """)

    w("install_dadapp.bat", """\
    @echo off
    echo === Family Guardian v2.0 Installer ===
    echo.

    REM Save adb devices output to temp file (handles special chars in serial)
    adb devices > "%TEMP%\\adb_devs.txt" 2>&1

    REM Find first device line
    set DEVICE_SERIAL=
    for /f "skip=1 tokens=1,2" %%a in (%TEMP%\\adb_devs.txt) do (
        if "%%b"=="device" (
            if not defined DEVICE_SERIAL (
                set "DEVICE_SERIAL=%%a"
            )
        )
    )

    if not defined DEVICE_SERIAL (
        echo ERROR: No device found. Connect via USB or wireless adb.
        pause
        exit /b 1
    )

    echo Device: %DEVICE_SERIAL%
    echo.

    set APK_PATH=app\\build\\outputs\\apk\\debug\\app-debug.apk
    if not exist "%APK_PATH%" (
        echo ERROR: APK not found at %APK_PATH%
        echo Run build_dadapp.ps1 first.
        pause
        exit /b 1
    )

    echo Installing...
    adb -s %DEVICE_SERIAL% install -r -t "%APK_PATH%"

    if %ERRORLEVEL% EQU 0 (
        echo.
        echo SUCCESS -- Family Guardian v2.0 installed.
    ) else (
        echo.
        echo INSTALL FAILED -- check adb output above.
    )
    pause
    """)


# ======================================================================
if __name__ == "__main__":
    main()
