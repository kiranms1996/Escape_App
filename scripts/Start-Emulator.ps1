param([switch]$NoWindow)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $projectRoot
if (Test-Path -LiteralPath '.tools\jdk') {
    $env:JAVA_HOME = (Get-ChildItem '.tools\jdk' -Directory | Select-Object -First 1).FullName
}
if (Test-Path -LiteralPath '.tools\android-sdk') { $env:ANDROID_HOME = Join-Path $projectRoot '.tools\android-sdk' }
if (-not $env:ANDROID_HOME -or -not $env:JAVA_HOME) { throw 'Set JAVA_HOME and ANDROID_HOME first; see README.md.' }
$env:ANDROID_AVD_HOME = Join-Path $projectRoot '.tools\avd'
New-Item -ItemType Directory -Force $env:ANDROID_AVD_HOME | Out-Null
$emulatorPath = Join-Path $env:ANDROID_HOME 'emulator\emulator.exe'
if (-not (Test-Path -LiteralPath $emulatorPath)) { throw 'Install the SDK emulator package first.' }
$avdName = 'Escape_API_36'
if (-not (Test-Path -LiteralPath (Join-Path $env:ANDROID_AVD_HOME "$avdName.ini"))) {
    $avdManager = Join-Path $env:ANDROID_HOME 'cmdline-tools\latest\bin\avdmanager.bat'
    'no' | & $avdManager create avd --name $avdName --package 'system-images;android-36;google_apis;x86_64' --device pixel_5
    if ($LASTEXITCODE -ne 0) { throw 'AVD creation failed. Install the Android 16 Google APIs x86_64 image first.' }
}
$emulatorArgs = @('-avd', $avdName, '-port', '5556', '-gpu', 'software', '-memory', '2048',
    '-cores', '2', '-no-boot-anim', '-no-snapshot', '-no-audio', '-camera-front', 'none', '-camera-back', 'none')
if ($NoWindow) { $emulatorArgs += '-no-window' }
Start-Process -FilePath $emulatorPath -ArgumentList $emulatorArgs -WindowStyle Hidden `
    -RedirectStandardOutput (Join-Path $projectRoot '.tools\emulator.stdout.log') `
    -RedirectStandardError (Join-Path $projectRoot '.tools\emulator.stderr.log') -PassThru | Select-Object Id,ProcessName
Write-Output 'Test device serial: emulator-5556. Wait for Android to boot before installing the APK.'
