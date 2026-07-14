[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern("^[A-Za-z0-9._-]+$")]
    [string]$Scenario,
    [string]$Serial,
    [string]$OutputRoot
)

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
if ([string]::IsNullOrWhiteSpace($OutputRoot)) {
    $OutputRoot = Join-Path $repoRoot "local-assets\diagnostics\root-runtime\window-scenarios"
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
$outputDir = Join-Path $OutputRoot "${timestamp}_${Scenario}"
New-Item -ItemType Directory -Path $outputDir -Force | Out-Null
$adbTarget = @("-s", $Serial)

function Save-AdbText {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][string[]]$Arguments
    )

    @(& $adb @adbTarget @Arguments 2>&1) |
        Set-Content -LiteralPath (Join-Path $outputDir $Name) -Encoding utf8
}

$manifest = [ordered]@{
    capturedAt = (Get-Date).ToString("o")
    scenario = $Scenario
    commit = (& git -C $repoRoot rev-parse HEAD).Trim()
    serial = $Serial
    appId = "li.songe.gkd"
}
$manifest | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $outputDir "manifest.json") -Encoding utf8

Save-AdbText -Name "activity.txt" -Arguments @("shell", "dumpsys", "activity", "activities")
Save-AdbText -Name "window.txt" -Arguments @("shell", "dumpsys", "window", "windows")
Save-AdbText -Name "accessibility.txt" -Arguments @("shell", "dumpsys", "accessibility")
Save-AdbText -Name "input-method.txt" -Arguments @("shell", "dumpsys", "input_method")
Save-AdbText -Name "display.txt" -Arguments @("shell", "dumpsys", "display")
Save-AdbText -Name "wm-size.txt" -Arguments @("shell", "wm", "size")
Save-AdbText -Name "wm-density.txt" -Arguments @("shell", "wm", "density")
Save-AdbText -Name "rotation.txt" -Arguments @("shell", "settings", "get", "system", "user_rotation")
Save-AdbText -Name "accelerometer-rotation.txt" -Arguments @("shell", "settings", "get", "system", "accelerometer_rotation")

Write-Host "Window scenario captured to: $outputDir"
