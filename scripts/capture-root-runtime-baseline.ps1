[CmdletBinding()]
param(
    [string]$Serial,
    [string]$OutputRoot,
    [switch]$IncludeUiAutomatorDump
)

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
if ([string]::IsNullOrWhiteSpace($OutputRoot)) {
    $OutputRoot = Join-Path $repoRoot "local-assets\diagnostics\root-runtime"
}

$adbCommand = Get-Command adb -ErrorAction SilentlyContinue
if ($null -eq $adbCommand) {
    throw "adb was not found in PATH. Install Android platform-tools first."
}
$adb = $adbCommand.Source

$deviceLines = @(& $adb devices | Select-Object -Skip 1 | Where-Object { $_ -match "\sdevice$" })
if ([string]::IsNullOrWhiteSpace($Serial)) {
    if ($deviceLines.Count -ne 1) {
        throw "Expected exactly one connected device, found $($deviceLines.Count). Pass -Serial when multiple devices are connected."
    }
    $Serial = ($deviceLines[0] -split "\s+")[0]
} elseif (-not ($deviceLines | Where-Object { ($_ -split "\s+")[0] -eq $Serial })) {
    throw "Device '$Serial' is not connected and authorized."
}

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$outputDir = Join-Path $OutputRoot "${timestamp}_$($Serial -replace '[^A-Za-z0-9._-]', '_')"
New-Item -ItemType Directory -Path $outputDir -Force | Out-Null
$adbTarget = @("-s", $Serial)

function Save-AdbText {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][string[]]$Arguments
    )

    $path = Join-Path $outputDir $Name
    $oldErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        @(& $adb @adbTarget @Arguments 2>&1) | Set-Content -LiteralPath $path -Encoding utf8
    } finally {
        $ErrorActionPreference = $oldErrorActionPreference
    }
}

$commit = (& git -C $repoRoot rev-parse HEAD).Trim()
$branch = (& git -C $repoRoot branch --show-current).Trim()
$deviceState = (& $adb @adbTarget get-state).Trim()
$rootIdentity = @(& $adb @adbTarget shell su -c id 2>&1) -join "`n"

$manifest = [ordered]@{
    capturedAt = (Get-Date).ToString("o")
    repository = $repoRoot
    commit = $commit
    branch = $branch
    serial = $Serial
    deviceState = $deviceState
    rootIdentity = $rootIdentity.Trim()
    appId = "li.songe.gkd"
}
$manifest | ConvertTo-Json -Depth 3 | Set-Content -LiteralPath (Join-Path $outputDir "manifest.json") -Encoding utf8

Save-AdbText -Name "getprop.txt" -Arguments @("shell", "getprop")
Save-AdbText -Name "root-id.txt" -Arguments @("shell", "su", "-c", "id")
Save-AdbText -Name "package.txt" -Arguments @("shell", "dumpsys", "package", "li.songe.gkd")
Save-AdbText -Name "activity-activities.txt" -Arguments @("shell", "dumpsys", "activity", "activities")
Save-AdbText -Name "activity-processes.txt" -Arguments @("shell", "dumpsys", "activity", "processes")
Save-AdbText -Name "window.txt" -Arguments @("shell", "dumpsys", "window", "windows")
Save-AdbText -Name "accessibility.txt" -Arguments @("shell", "dumpsys", "accessibility")
Save-AdbText -Name "display.txt" -Arguments @("shell", "dumpsys", "display")
Save-AdbText -Name "input-method.txt" -Arguments @("shell", "dumpsys", "input_method")
Save-AdbText -Name "wm-size.txt" -Arguments @("shell", "wm", "size")
Save-AdbText -Name "wm-density.txt" -Arguments @("shell", "wm", "density")
Save-AdbText -Name "logcat.txt" -Arguments @("logcat", "-d", "-v", "threadtime")

if ($IncludeUiAutomatorDump) {
    $remoteHierarchy = "/data/local/tmp/gkd-window-$timestamp.xml"
    $oldErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        & $adb @adbTarget shell uiautomator dump $remoteHierarchy 2>&1 |
            Set-Content -LiteralPath (Join-Path $outputDir "uiautomator-status.txt") -Encoding utf8
        & $adb @adbTarget pull $remoteHierarchy (Join-Path $outputDir "window.xml") 2>&1 |
            Add-Content -LiteralPath (Join-Path $outputDir "uiautomator-status.txt") -Encoding utf8
        & $adb @adbTarget shell rm -f $remoteHierarchy 2>&1 | Out-Null
    } finally {
        $ErrorActionPreference = $oldErrorActionPreference
    }
} else {
    @(
        "Skipped by default."
        "GKD Automation mode owns the UiAutomation channel; an external uiautomator dump may fail or disturb the runtime."
        "Pass -IncludeUiAutomatorDump only when GKD Automation is stopped or when testing this conflict explicitly."
    ) | Set-Content -LiteralPath (Join-Path $outputDir "uiautomator-status.txt") -Encoding utf8
}

Write-Host "Baseline diagnostics captured to: $outputDir"
Write-Host "Root identity: $($rootIdentity.Trim())"
Write-Host "Export GKD configuration/backup and in-app logs separately; this script never reads private app data."
