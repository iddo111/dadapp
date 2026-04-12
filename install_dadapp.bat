@echo off
echo === Family Guardian v2.0 Installer ===
echo.

REM Save adb devices output to temp file (handles special chars in serial)
adb devices > "%TEMP%\adb_devs.txt" 2>&1

REM Find first device line
set DEVICE_SERIAL=
for /f "skip=1 tokens=1,2" %%a in (%TEMP%\adb_devs.txt) do (
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

set APK_PATH=app\build\outputs\apk\debug\app-debug.apk
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
