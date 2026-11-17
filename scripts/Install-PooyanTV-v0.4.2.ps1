# Installs the parallel v0.4.2 preview WITHOUT uninstalling or modifying v0.1.
param(
    [Parameter(Mandatory=$true)][string]$TvAddress,
    [string]$ApkPath = (Join-Path $PSScriptRoot 'app-debug.apk')
)
$ErrorActionPreference = 'Stop'
$adb = Get-Command adb -ErrorAction SilentlyContinue
if (!$adb) { throw 'Install Android Platform Tools and add adb to PATH first.' }
if (!(Test-Path -LiteralPath $ApkPath)) { throw 'APK not found: ' + $ApkPath }
if ($TvAddress -notmatch ':') { $TvAddress += ':5555' }
& adb connect $TvAddress
if ($LASTEXITCODE -ne 0) { throw 'ADB connection failed. Approve it on your television.' }
$state = & adb -s $TvAddress get-state
if ($LASTEXITCODE -ne 0 -or $state -ne 'device') { throw 'Television is not authorized.' }
& adb -s $TvAddress install -r $ApkPath
if ($LASTEXITCODE -ne 0) { throw 'Install failed; no uninstall was attempted.' }
& adb -s $TvAddress shell am start -W -n com.pooyan.namakadetv.preview042worker/com.pooyan.namakadetv.MainActivity
if ($LASTEXITCODE -ne 0) { throw 'Launch failed.' }
Write-Host 'Pooyan TV v0.4.2 preview installation launched. v0.1 was left untouched.'

