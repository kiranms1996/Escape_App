param()
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $projectRoot

# Prefer the optional portable tools used for the recorded build. They are not in the source ZIP.
$portableJdk = Join-Path $projectRoot '.tools\jdk'
if (Test-Path -LiteralPath $portableJdk) {
    $env:JAVA_HOME = (Get-ChildItem -LiteralPath $portableJdk -Directory | Select-Object -First 1).FullName
}
if (-not $env:JAVA_HOME -or -not (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin\java.exe'))) {
    throw 'Set JAVA_HOME to your JDK 17 installation. See README.md.'
}
$portableSdk = Join-Path $projectRoot '.tools\android-sdk'
if (Test-Path -LiteralPath $portableSdk) { $env:ANDROID_HOME = $portableSdk }
elseif (-not $env:ANDROID_HOME) { $env:ANDROID_HOME = Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
if (-not (Test-Path -LiteralPath (Join-Path $env:ANDROID_HOME 'platforms\android-36\android.jar'))) {
    throw 'Install Android SDK Platform 36 and set ANDROID_HOME to your SDK directory. See README.md.'
}
& .\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --console=plain
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Output "Debug APK: $projectRoot\app\build\outputs\apk\debug\app-debug.apk"
