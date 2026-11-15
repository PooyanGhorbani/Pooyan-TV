param(
    [Parameter(Mandatory=$true)][string]$TvAddress,
    [string]$ApkPath = (Join-Path $PSScriptRoot 'pooyan-tv-v0.1-debug.apk')
)
$ErrorActionPreference = 'Stop'
$adb = Join-Path $PSScriptRoot 'platform-tools/adb.exe'
if (!(Test-Path -LiteralPath $adb)) {
    $zip = Join-Path $PSScriptRoot 'platform-tools.zip'
    Invoke-WebRequest 'https://dl.google.com/android/repository/platform-tools-latest-windows.zip' -OutFile $zip
    Expand-Archive -LiteralPath $zip -DestinationPath $PSScriptRoot -Force
}
if (!(Test-Path -LiteralPath $ApkPath)) { throw 'APK file not found.' }
if ($TvAddress -notmatch ':') { $TvAddress += ':5555' }
& $adb connect $TvAddress
if ($LASTEXITCODE -ne 0) { throw 'ADB connection failed. Authorize this computer on the TV.' }
$state = & $adb -s $TvAddress get-state 2>&1
if ($state -ne 'device') { throw 'TV is not authorized or not reachable.' }
& $adb -s $TvAddress install -r $ApkPath
if ($LASTEXITCODE -ne 0) { throw 'Installation failed. No app was uninstalled and no data was erased.' }
& $adb -s $TvAddress shell am start -W -n com.pooyan.namakadetv/.MainActivity
if ($LASTEXITCODE -ne 0) { throw 'Launch failed.' }
& $adb -s $TvAddress shell pidof com.pooyan.namakadetv
