<#
.SYNOPSIS
Safely install Neon Launcher onto an Android TV / Google TV device via ADB.

.DESCRIPTION
Downloads the latest GitHub Release APK and verifies its SHA-256 checksum
unless -Apk is provided. Prints the uninstall/rollback command before acting
and requires explicit confirmation.
#>
[CmdletBinding()]
param(
    [Parameter(Position = 0)]
    [string]$Target = '',

    [string]$Serial = '',

    [string]$Apk = '',

    [switch]$DryRun,

    [switch]$Help
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if ($Help) {
    Get-Help -Full $MyInvocation.MyCommand.Path
    exit 0
}

$ScriptDir = $PSScriptRoot
. (Join-Path $ScriptDir '_common.ps1')

$Config = Get-NeonConfig -ScriptDir $ScriptDir
$GitHubRepo = $Config['GITHUB_REPO']
$AppPackage = $Config['APP_PACKAGE']
$ApkAssetName = $Config['APK_ASSET_NAME']

$ApkUrl = "https://github.com/$GitHubRepo/releases/latest/download/$ApkAssetName"
$ShaUrl = "$ApkUrl.sha256"

if (-not [string]::IsNullOrWhiteSpace($Apk) -and -not (Test-Path -LiteralPath $Apk)) {
    throw "Local APK file not found: $Apk"
}

if ([string]::IsNullOrWhiteSpace($Apk) -and $GitHubRepo -eq 'OWNER/neon-launcher') {
    Write-Warning "GITHUB_REPO in scripts\config.env is still set to 'OWNER/neon-launcher'. Update scripts\config.env or pass -Apk <path>."
}

if (-not $DryRun) {
    Assert-CommandAvailable -Name 'adb' -Hint 'Install Android Platform Tools and ensure adb.exe is in PATH.'
}

$ResolvedTarget = Resolve-AdbTarget -DryRun ([bool]$DryRun) -Target $Target -Serial $Serial
$AdbPrefix = if ($ResolvedTarget -and $ResolvedTarget -ne '<device>') { "adb -s $ResolvedTarget" } else { 'adb' }
$RollbackCmd = "$AdbPrefix uninstall $AppPackage"

Write-Host '=== Neon Launcher Install Plan ==='
Write-Host "Package         : $AppPackage"
Write-Host "Target device   : $ResolvedTarget"
if (-not [string]::IsNullOrWhiteSpace($Apk)) {
    Write-Host "APK source      : Local file ($Apk)"
} else {
    Write-Host "APK source      : $ApkUrl"
    Write-Host "Checksum source : $ShaUrl"
}
Write-Host ''
Write-Host 'Rollback / uninstall command (printed before making any changes):'
Write-Host "  $RollbackCmd"
Write-Host ''

if ($DryRun) {
    Write-Host '[DRY RUN] Planned commands:'
    if ([string]::IsNullOrWhiteSpace($Apk)) {
        Write-Host "  1. Download $ApkUrl and $ShaUrl"
        Write-Host "  2. Verify SHA-256 checksum of $ApkAssetName"
    }
    Write-Host "  3. $AdbPrefix install -r <apk-path>"
    Write-Host '[DRY RUN] Complete. No downloads or device changes performed.'
    exit 0
}

$TempDir = $null
try {
    $ApkToInstall = $Apk
    if ([string]::IsNullOrWhiteSpace($ApkToInstall)) {
        $TempDir = Join-Path ([System.IO.Path]::GetTempPath()) ("neon-launcher-" + [System.Guid]::NewGuid().ToString('N'))
        $null = New-Item -ItemType Directory -Path $TempDir -Force
        $ApkToInstall = Join-Path $TempDir $ApkAssetName
        $ShaFile = Join-Path $TempDir "$ApkAssetName.sha256"

        Write-Host 'Downloading release APK and SHA-256 checksum...'
        Invoke-WebRequest -Uri $ApkUrl -OutFile $ApkToInstall -UseBasicParsing
        Invoke-WebRequest -Uri $ShaUrl -OutFile $ShaFile -UseBasicParsing

        $ExpectedSha = ((Get-Content -LiteralPath $ShaFile -Raw).Trim() -split '\s+')[0].ToLowerInvariant()
        $ActualSha = (Get-FileHash -LiteralPath $ApkToInstall -Algorithm SHA256).Hash.ToLowerInvariant()
        if ([string]::IsNullOrWhiteSpace($ExpectedSha) -or $ExpectedSha -ne $ActualSha) {
            throw "SHA-256 verification FAILED! Expected: '$ExpectedSha', Actual: '$ActualSha'"
        }
        Write-Host "SHA-256 verified: $ActualSha"
    } else {
        $SidecarSha = "$Apk.sha256"
        if (Test-Path -LiteralPath $SidecarSha) {
            $ExpectedSha = ((Get-Content -LiteralPath $SidecarSha -Raw).Trim() -split '\s+')[0].ToLowerInvariant()
            $ActualSha = (Get-FileHash -LiteralPath $ApkToInstall -Algorithm SHA256).Hash.ToLowerInvariant()
            if ([string]::IsNullOrWhiteSpace($ExpectedSha) -or $ExpectedSha -ne $ActualSha) {
                throw "Local APK SHA-256 mismatch against $SidecarSha!"
            }
            Write-Host "Local SHA-256 verified: $ActualSha"
        }
    }

    if (-not (Confirm-NeonAction -Prompt "Install $AppPackage on target device ($ResolvedTarget)?")) {
        exit 1
    }

    if ($ResolvedTarget -and $ResolvedTarget -ne '<device>') {
        & adb -s $ResolvedTarget install -r $ApkToInstall
    } else {
        & adb install -r $ApkToInstall
    }

    if ($LASTEXITCODE -ne 0) {
        Write-Warning "Install failed. If switching between debug-fallback and release-signed APKs, run '$RollbackCmd' first and retry (see docs\TROUBLESHOOTING.md)."
        exit $LASTEXITCODE
    }

    Write-Host ''
    Write-Host 'Neon Launcher installed successfully.'
    Write-Host 'Next steps:'
    Write-Host '  - Open Neon Launcher on your TV or run .\scripts\set-default-launcher.ps1 -DryRun'
    Write-Host "  - To uninstall later: $RollbackCmd"
}
finally {
    if ($TempDir -and (Test-Path -LiteralPath $TempDir)) {
        Remove-Item -LiteralPath $TempDir -Recurse -Force -ErrorAction SilentlyContinue
    }
}
