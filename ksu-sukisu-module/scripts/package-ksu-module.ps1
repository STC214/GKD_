param(
    [string]$OutputPath,
    [string]$ApkPath
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$moduleRoot = Resolve-Path (Join-Path $scriptDir "..")
$templateDir = Join-Path $moduleRoot "module"
$repoRoot = Resolve-Path (Join-Path $moduleRoot "..")
$workDir = Join-Path $moduleRoot "work"
$stagingDir = Join-Path $workDir "gkd_ksu_sukisu"
$distDir = Join-Path $moduleRoot "dist"

if (-not $OutputPath) {
    $OutputPath = Join-Path $distDir "gkd-ksu-sukisu-module.zip"
}
if (-not $ApkPath) {
    $ApkPath = Join-Path $repoRoot "app\build\outputs\apk\gkd\release\app-gkd-release.apk"
}
$ApkPath = [System.IO.Path]::GetFullPath($ApkPath)
if (-not (Test-Path -LiteralPath $ApkPath -PathType Leaf)) {
    throw "Release APK not found: $ApkPath. Build the GKD release APK or pass -ApkPath."
}
if ((Get-Item -LiteralPath $ApkPath).Length -lt 1024) {
    throw "Release APK is unexpectedly small: $ApkPath"
}
$OutputPath = [System.IO.Path]::GetFullPath($OutputPath)
$outputParent = Split-Path -Parent $OutputPath

Remove-Item -LiteralPath $workDir -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path $stagingDir | Out-Null
New-Item -ItemType Directory -Path $distDir -Force | Out-Null
New-Item -ItemType Directory -Path $outputParent -Force | Out-Null
Copy-Item -Path (Join-Path $templateDir "*") -Destination $stagingDir -Recurse -Force
New-Item -ItemType Directory -Path (Join-Path $stagingDir "common") -Force | Out-Null
Copy-Item -LiteralPath $ApkPath -Destination (Join-Path $stagingDir "common\gkd.apk") -Force

# Android module scripts must use LF and must not contain a UTF-8 BOM.
Get-ChildItem -LiteralPath $stagingDir -Recurse -File |
    Where-Object { $_.Extension -eq ".sh" -or $_.Name -in @("config.conf", "module.prop", "skip_mount") } |
    ForEach-Object {
        $content = Get-Content -LiteralPath $_.FullName -Raw -Encoding UTF8
        $content = $content -replace "`r`n", "`n"
        [System.IO.File]::WriteAllText($_.FullName, $content, [System.Text.UTF8Encoding]::new($false))
    }

$timestamp = Get-Date -Format "yyyyMMddHHmm"
$versionCode = $timestamp.Substring($timestamp.Length - 9)
$stagedModuleProp = Join-Path $stagingDir "module.prop"
$moduleProp = Get-Content -LiteralPath $stagedModuleProp -Raw -Encoding UTF8
$moduleProp = $moduleProp -replace '(?m)^version=.*$', "version=$timestamp"
$moduleProp = $moduleProp -replace '(?m)^versionCode=.*$', "versionCode=$versionCode"
[System.IO.File]::WriteAllText($stagedModuleProp, $moduleProp, [System.Text.UTF8Encoding]::new($false))

Remove-Item -LiteralPath $OutputPath -Force -ErrorAction SilentlyContinue

Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zipFile = [System.IO.Compression.ZipFile]::Open($OutputPath, [System.IO.Compression.ZipArchiveMode]::Create)
try {
    $stagingFullPath = (Resolve-Path $stagingDir).Path.TrimEnd('\', '/')
    Get-ChildItem -LiteralPath $stagingDir -Recurse -File | ForEach-Object {
        $relativePath = $_.FullName.Substring($stagingFullPath.Length + 1).Replace('\', '/')
        [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile(
            $zipFile,
            $_.FullName,
            $relativePath,
            [System.IO.Compression.CompressionLevel]::Optimal
        ) | Out-Null
    }
}
finally {
    $zipFile.Dispose()
}

$zipCheck = [System.IO.Compression.ZipFile]::OpenRead($OutputPath)
try {
    $entries = $zipCheck.Entries
    $badEntry = $entries | Where-Object { $_.FullName.Contains('\') } | Select-Object -First 1
    if ($badEntry) {
        throw "Invalid zip entry path contains backslash: $($badEntry.FullName)"
    }
    $apkEntries = @($entries | Where-Object { $_.FullName -match '(?i)\.apk$' })
    if ($apkEntries.Count -ne 1 -or $apkEntries[0].FullName -ne "common/gkd.apk") {
        throw "Expected exactly common/gkd.apk in module, found: $(($apkEntries.FullName -join ', '))"
    }
    $expectedEntries = @(
        "action.sh",
        "config.conf",
        "customize.sh",
        "common/gkd.apk",
        "module.prop",
        "service.sh",
        "skip_mount",
        "uninstall.sh"
    )
    $actualEntries = @($entries | ForEach-Object { $_.FullName } | Sort-Object)
    $entryDifference = Compare-Object ($expectedEntries | Sort-Object) $actualEntries
    if ($entryDifference) {
        $details = ($entryDifference | ForEach-Object { "$($_.SideIndicator) $($_.InputObject)" }) -join "; "
        throw "Unexpected keepalive module contents: $details"
    }
}
finally {
    $zipCheck.Dispose()
}

Write-Host "Packaged GKD bundle module: $OutputPath"
Write-Host "Bundled APK: common/gkd.apk ($((Get-Item -LiteralPath $ApkPath).Length) bytes)"
