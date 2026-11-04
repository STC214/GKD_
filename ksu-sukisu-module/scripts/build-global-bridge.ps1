param(
    [string]$SdkRoot = $env:ANDROID_HOME,
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$NdkVersion = "28.2.13676358",
    [switch]$Include32Bit
)
$ErrorActionPreference = "Stop"
if (-not $SdkRoot -or -not $JavaHome) { throw "Set ANDROID_HOME and JAVA_HOME" }
$root = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot "../global"))
$out = Join-Path $root "build"
New-Item -ItemType Directory -Force (Join-Path $out "classes"),(Join-Path $out "dex") | Out-Null
$android = Join-Path $SdkRoot "platforms/android-37.0/android.jar"
$source = Join-Path $root "java/li/songe/gkd/global/GlobalBridge.java"
& (Join-Path $JavaHome "bin/javac.exe") --release 17 -cp $android -d (Join-Path $out "classes") $source
if ($LASTEXITCODE -ne 0) { throw "Global helper Java compilation failed" }
$classes = @(Get-ChildItem -LiteralPath (Join-Path $out "classes") -Recurse -Filter '*.class' | ForEach-Object FullName)
& (Join-Path $SdkRoot "build-tools/37.0.0/d8.bat") --min-api 26 --lib $android --output (Join-Path $out "dex") @classes
if ($LASTEXITCODE -ne 0) { throw "Global helper DEX compilation failed" }
Copy-Item -LiteralPath (Join-Path $out "dex/classes.dex") -Destination (Join-Path $out "global_bridge.dex") -Force
$toolchain = Join-Path $SdkRoot "ndk/$NdkVersion/toolchains/llvm/prebuilt/windows-x86_64/bin"
$architectures = @(@("arm64-v8a", "aarch64-linux-android26"), @("x86_64", "x86_64-linux-android26"))
if ($Include32Bit) {
    $architectures += @(@("armeabi-v7a", "armv7a-linux-androideabi26"), @("x86", "i686-linux-android26"))
}
foreach ($arch in $architectures) {
    & (Join-Path $toolchain "clang++.exe") --target=$($arch[1]) -std=c++17 -O2 -fPIC -fvisibility=hidden -fvisibility-inlines-hidden -ffunction-sections -fdata-sections -shared -static-libstdc++ "-Wl,-z,max-page-size=16384" "-Wl,--exclude-libs,ALL" "-Wl,--gc-sections" -Wall -Wextra (Join-Path $root "native/global_bridge.cpp") -llog -ldl -o (Join-Path $out ($arch[0] + '.so'))
    if ($LASTEXITCODE -ne 0) { throw "Native compilation failed: $($arch[0])" }
    & (Join-Path $toolchain "llvm-strip.exe") --strip-unneeded (Join-Path $out ($arch[0] + '.so'))
    if ($LASTEXITCODE -ne 0) { throw "Native strip failed" }
    & (Join-Path $toolchain "llvm-readelf.exe") -h -d (Join-Path $out ($arch[0] + '.so'))
    if ($LASTEXITCODE -ne 0) { throw "ELF validation failed" }
}
$artifacts = @((Join-Path $out "global_bridge.dex")) + @($architectures | ForEach-Object { Join-Path $out ($_[0] + '.so') })
Get-FileHash -LiteralPath $artifacts -Algorithm SHA256
