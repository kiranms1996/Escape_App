# Dot-source for manual, observed UI testing on the isolated Android emulator.
# These are host test tools; no UI inspection is part of the shipped app.
param([string]$Serial = 'emulator-5556')
$projectRoot = Split-Path -Parent $PSScriptRoot
$script:TestAdb = Join-Path $projectRoot '.tools\android-sdk\platform-tools\adb.exe'
if (-not (Test-Path -LiteralPath $script:TestAdb)) {
    $script:TestAdb = Join-Path $env:ANDROID_HOME 'platform-tools\adb.exe'
}
$script:TestSerial = $Serial
if ($Serial -notlike 'emulator-*') { throw 'This helper is restricted to emulators, not personal phones.' }

function Invoke-TestAdb {
    & $script:TestAdb -s $script:TestSerial @args
    if ($LASTEXITCODE -ne 0) { throw "ADB failed: $args" }
}

function Get-AndroidUi {
    $enabledServices = (Invoke-TestAdb shell settings get secure enabled_accessibility_services) -join ''
    if ($enabledServices -like '*com.escape.security*') {
        $inspection = (Invoke-TestAdb shell am instrument -w -e inspectUi true com.escape.security.harness.test/com.escape.security.UiDumpRunner) -join "`n"
        if ($inspection -notmatch '(?s)ESCAPE_UI_XML_BEGIN(.*?)ESCAPE_UI_XML_END') {
            throw "Accessibility-preserving inspector failed. Install the live-tests harness first. $inspection"
        }
        return [xml]$Matches[1]
    }
    $dumpStatus = (Invoke-TestAdb shell uiautomator dump /sdcard/escape-test-ui.xml) -join "`n"
    if ($dumpStatus -notmatch 'dumped to') { throw "UI dump did not complete: $dumpStatus" }
    $rawXml = (Invoke-TestAdb shell cat /sdcard/escape-test-ui.xml) -join "`n"
    return [xml]$rawXml
}

function Show-AndroidUi {
    $ui = Get-AndroidUi
    $ui.SelectNodes('//node') | Where-Object { $_.text -or $_.'content-desc' } | ForEach-Object {
        "text=$($_.text); desc=$($_.'content-desc'); id=$($_.'resource-id'); enabled=$($_.enabled); bounds=$($_.bounds)"
    }
}

function Click-AndroidText {
    param([Parameter(Mandatory)][string]$Text)
    $ui = Get-AndroidUi
    $matchedNodes = @($ui.SelectNodes('//node') | Where-Object { $_.text -eq $Text -or $_.'content-desc' -eq $Text })
    if ($matchedNodes.Count -ne 1) { throw "Expected one observed node for '$Text'; found $($matchedNodes.Count)." }
    $node = $matchedNodes[0]
    while ($node -and $node.Name -eq 'node' -and $node.clickable -ne 'true') { $node = $node.ParentNode }
    if (-not $node -or $node.Name -ne 'node') { throw "No clickable node for '$Text'." }
    if ($node.enabled -eq 'false') { throw "'$Text' is disabled." }
    if ($node.bounds -notmatch '^\[(\d+),(\d+)\]\[(\d+),(\d+)\]$') { throw 'Invalid observed bounds.' }
    $x = ([int]$Matches[1] + [int]$Matches[3]) / 2
    $y = ([int]$Matches[2] + [int]$Matches[4]) / 2
    Invoke-TestAdb shell input tap ([int]$x) ([int]$y)
}

function Assert-AndroidText {
    param([Parameter(Mandatory)][string]$Text)
    $ui = Get-AndroidUi
    if (-not ($ui.SelectNodes('//node') | Where-Object { $_.text -like "*$Text*" -or $_.'content-desc' -like "*$Text*" })) {
        throw "Expected visible text '$Text' was absent."
    }
    Write-Output "PASS: visible '$Text'"
}
