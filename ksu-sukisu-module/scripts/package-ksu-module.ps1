param(
    [string]$OutputPath,
    [string]$ApkPath,
    [string]$ExpectedApkSha256
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
    $apkDir = Join-Path $repoRoot "app\build\outputs\apk\gkd\release"
    $metadata = Get-Content -LiteralPath (Join-Path $apkDir "output-metadata.json") -Raw -Encoding UTF8 | ConvertFrom-Json
    if (@($metadata.elements).Count -ne 1) { throw "Expected one release APK in output metadata" }
    $ApkPath = [System.IO.Path]::GetFullPath((Join-Path $apkDir $metadata.elements[0].outputFile))
    $apkDirPrefix = [System.IO.Path]::GetFullPath($apkDir).TrimEnd('\', '/') + [System.IO.Path]::DirectorySeparatorChar
    if (-not $ApkPath.StartsWith($apkDirPrefix, [StringComparison]::OrdinalIgnoreCase)) { throw "APK metadata path escapes release directory" }
}
$ApkPath = [System.IO.Path]::GetFullPath($ApkPath)
if (-not (Test-Path -LiteralPath $ApkPath -PathType Leaf)) {
    throw "Release APK not found: $ApkPath. Build the GKD release APK or pass -ApkPath."
}
if ((Get-Item -LiteralPath $ApkPath).Length -lt 1024) {
    throw "Release APK is unexpectedly small: $ApkPath"
}
$apkSha256 = (Get-FileHash -LiteralPath $ApkPath -Algorithm SHA256).Hash.ToLowerInvariant()
if ($ExpectedApkSha256 -and $apkSha256 -ne $ExpectedApkSha256.ToLowerInvariant()) {
    throw "APK SHA256 mismatch: expected $ExpectedApkSha256, got $apkSha256"
}
Add-Type -AssemblyName System.IO.Compression.FileSystem
$apkCheck = [System.IO.Compression.ZipFile]::OpenRead($ApkPath)
try {
    if (-not $apkCheck.GetEntry("AndroidManifest.xml") -or -not $apkCheck.GetEntry("classes.dex")) {
        throw "Input is not a packaged Android application"
    }
}
finally { $apkCheck.Dispose() }
$OutputPath = [System.IO.Path]::GetFullPath($OutputPath)
$outputParent = Split-Path -Parent $OutputPath
if ([System.IO.Path]::GetExtension($OutputPath) -ne ".zip") { throw "Module output must use .zip" }
if ($OutputPath -eq $ApkPath) { throw "Module output must not overwrite APK" }
foreach ($protectedDir in @($workDir, $templateDir)) {
    $prefix = [System.IO.Path]::GetFullPath($protectedDir).TrimEnd('\', '/') + [System.IO.Path]::DirectorySeparatorChar
    if ($ApkPath.StartsWith($prefix, [StringComparison]::OrdinalIgnoreCase) -or $OutputPath.StartsWith($prefix, [StringComparison]::OrdinalIgnoreCase)) {
        throw "APK and output must stay outside module staging/template directories"
    }
}

$expectedWorkDir = [System.IO.Path]::GetFullPath((Join-Path $moduleRoot "work"))
if ([System.IO.Path]::GetFullPath($workDir) -ne $expectedWorkDir) {
    throw "Unexpected staging cleanup path: $workDir"
}
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

$stagedModuleProp = Join-Path $stagingDir "module.prop"
$moduleProp = Get-Content -LiteralPath $stagedModuleProp -Raw -Encoding UTF8
if ($moduleProp -notmatch '(?m)^version=\S+$') { throw "Missing module version" }
if ($moduleProp -notmatch '(?m)^versionCode=([0-9]+)$') { throw "Missing numeric module versionCode" }
$moduleVersionCode = 0
if (-not [int]::TryParse($Matches[1], [ref]$moduleVersionCode) -or $moduleVersionCode -le 0) { throw "Invalid module versionCode" }

# Do not destroy the last good bundle if archive creation or verification fails.
$finalOutputPath = $OutputPath
$OutputPath = Join-Path $outputParent (([System.IO.Path]::GetFileName($finalOutputPath)) + "." + [guid]::NewGuid().ToString("N") + ".tmp")

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
    $apkStream = $apkEntries[0].Open()
    $sha256 = [System.Security.Cryptography.SHA256]::Create()
    try {
        $bundledHash = [System.BitConverter]::ToString($sha256.ComputeHash($apkStream)).Replace('-', '').ToLowerInvariant()
        if ($bundledHash -ne $apkSha256) { throw "Bundled APK hash differs from input APK" }
    }
    finally { $apkStream.Dispose(); $sha256.Dispose() }
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

Move-Item -LiteralPath $OutputPath -Destination $finalOutputPath -Force
Write-Host "Packaged GKD bundle module: $finalOutputPath"
Write-Host "Bundled APK: common/gkd.apk ($((Get-Item -LiteralPath $ApkPath).Length) bytes)"
Write-Host "Bundled APK SHA256: $apkSha256"
