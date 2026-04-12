# build_dadapp.ps1 -- Build Family Guardian APK
$ErrorActionPreference = "Stop"

# Find JAVA_HOME from Android Studio
$studioLocations = @(
    "$env:ProgramFiles\Android\Android Studio",
    "$env:ProgramFiles\Android\Android Studio\jbr",
    "${env:ProgramFiles(x86)}\Android\Android Studio",
    "$env:LOCALAPPDATA\Android\android-studio"
)

# Try Android Studio bundled JBR first
$javaHome = $null
foreach ($loc in $studioLocations) {
    $jbrPath = Join-Path $loc "jbr"
    if (Test-Path (Join-Path $jbrPath "bin\java.exe")) {
        $javaHome = $jbrPath
        break
    }
    if (Test-Path (Join-Path $loc "bin\java.exe")) {
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
    & .\gradlew.bat assembleDebug --no-daemon 2>&1 | ForEach-Object { Write-Host $_ }
    if ($LASTEXITCODE -ne 0) {
        Write-Host "BUILD FAILED" -ForegroundColor Red
        exit 1
    }
    $apk = Get-ChildItem -Recurse -Filter "*.apk" -Path "app\build\outputs" | Select-Object -First 1
    if ($apk) {
        Write-Host "APK: $($apk.FullName)" -ForegroundColor Green
    } else {
        Write-Host "WARNING: APK not found in expected location" -ForegroundColor Yellow
    }
} finally {
    Pop-Location
}
