param(
    [string]$ApkPath,
    [string]$OutputPath,
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$moduleRoot = Resolve-Path (Join-Path $scriptDir "..")
$repoRoot = Resolve-Path (Join-Path $moduleRoot "..")
$templateDir = Join-Path $moduleRoot "module"
$workDir = Join-Path $moduleRoot "work"
$stagingDir = Join-Path $workDir "gkd_ksu_sukisu"
$distDir = Join-Path $moduleRoot "dist"

if (-not $OutputPath) {
    $OutputPath = Join-Path $distDir "gkd-ksu-sukisu-module.zip"
}

if (-not $SkipBuild -and -not $ApkPath) {
    Push-Location $repoRoot
    try {
        & .\gradlew.bat app:assembleGkdRelease -PGKD_RENAME_APK_FLAG=1
        if ($LASTEXITCODE -ne 0) {
            throw "Gradle build failed with exit code $LASTEXITCODE"
        }
    }
    finally {
        Pop-Location
    }
}

if (-not $ApkPath) {
    $apkDir = Join-Path $repoRoot "app\build\outputs\apk\gkd\release"
    $apk = Get-ChildItem -Path $apkDir -Filter "*.apk" -File |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    if (-not $apk) {
        throw "No APK found in $apkDir"
    }
    $ApkPath = $apk.FullName
}

$resolvedApk = Resolve-Path $ApkPath

Remove-Item -LiteralPath $workDir -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path $stagingDir | Out-Null
New-Item -ItemType Directory -Path (Join-Path $stagingDir "common") | Out-Null
New-Item -ItemType Directory -Path $distDir -Force | Out-Null

Copy-Item -Path (Join-Path $templateDir "*") -Destination $stagingDir -Recurse -Force
Copy-Item -Path $resolvedApk -Destination (Join-Path $stagingDir "common\gkd.apk") -Force

Remove-Item -LiteralPath $OutputPath -Force -ErrorAction SilentlyContinue
Compress-Archive -Path (Join-Path $stagingDir "*") -DestinationPath $OutputPath -Force

Write-Host "Packaged module: $OutputPath"
Write-Host "Bundled APK: $resolvedApk"
