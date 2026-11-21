<#
.SYNOPSIS
Set Neon Launcher as the default HOME launcher on Android TV via ADB.

.DESCRIPTION
Detects the current stock HOME launcher, prints exact rollback/restore commands
before acting, requires confirmation, and applies non-destructive ADB commands
by default. Only disables the stock launcher if -DisableStock is passed with
double warning and double confirmation.
#>
[CmdletBinding()]
param(
    [Parameter(Position = 0)]
    [string]$Target = '',

    [string]$Serial = '',

    [switch]$DisableStock,

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
$AppPackage = $Config['APP_PACKAGE']
$HomeAlias = $Config['HOME_ALIAS']

if (-not $DryRun) {
    Assert-CommandAvailable -Name 'adb' -Hint 'Install Android Platform Tools and ensure adb.exe is in PATH.'
}

$ResolvedTarget = Resolve-AdbTarget -DryRun ([bool]$DryRun) -Target $Target -Serial $Serial
$AdbPrefix = if ($ResolvedTarget -and $ResolvedTarget -ne '<device>') { "adb -s $ResolvedTarget" } else { 'adb' }

$StockComponent = '<stock-launcher-pkg>/<stock-launcher-activity>'
$StockPackage = '<stock-launcher-pkg>'
$ApiLevel = 0

if (-not $DryRun) {
    $PmPath = Invoke-AdbShell -TargetSerial $ResolvedTarget -ShellArgs @('pm', 'path', $AppPackage)
    if ($PmPath -notmatch '^package:') {
        throw "$AppPackage is not installed on $ResolvedTarget. Run .\scripts\install.ps1 first."
    }

    $SdkRaw = Invoke-AdbShell -TargetSerial $ResolvedTarget -ShellArgs @('getprop', 'ro.build.version.sdk')
    if ($SdkRaw -match '^\d+$') {
        $ApiLevel = [int]$SdkRaw
    }

    $Detected = @(Get-HomeLaunchers -TargetSerial $ResolvedTarget | Where-Object { $_ -notlike "$AppPackage/*" })
    if ($Detected.Count -eq 1) {
        $StockComponent = $Detected[0]
        $StockPackage = ($StockComponent -split '/')[0]
    } elseif ($Detected.Count -gt 1) {
        Write-Host 'Detected multiple candidate stock HOME launchers:'
        foreach ($h in $Detected) {
            Write-Host "  - $h"
        }
        if ($DisableStock) {
            throw 'Refusing -DisableStock automatically when multiple stock HOME launchers exist.'
        }
        $StockComponent = $Detected[0]
        $StockPackage = ($StockComponent -split '/')[0]
    } else {
        if ($DisableStock) {
            throw 'Could not detect an active stock launcher package; refusing -DisableStock for safety.'
        }
    }
}

Write-Host '=== Neon Launcher Default HOME Setup Plan ==='
Write-Host "Target device      : $ResolvedTarget"
Write-Host "Neon HomeAlias     : $HomeAlias"
Write-Host "Detected stock HOME: $StockComponent"
Write-Host ("Android API level  : " + $(if ($ApiLevel -gt 0) { $ApiLevel } else { 'unknown (dry-run)' }))
Write-Host ''
Write-Host 'Planned non-destructive commands:'
Write-Host "  1. $AdbPrefix shell pm enable $HomeAlias"
Write-Host "  2. $AdbPrefix shell cmd package set-home-activity $HomeAlias"
if ($DryRun -or $ApiLevel -ge 29) {
    Write-Host "  3. $AdbPrefix shell cmd role add-role-holder android.app.role.HOME $AppPackage   (API 29+)"
}
if ($DisableStock) {
    Write-Host "  4. [DESTRUCTIVE LAST RESORT] $AdbPrefix shell pm disable-user --user 0 $StockPackage"
}
Write-Host ''
Write-Host 'Rollback / restore commands (printed BEFORE making any changes):'
Write-Host '  Option A (helper script):'
Write-Host "    .\scripts\restore-stock-launcher.ps1 -StockPackage $StockPackage -StockComponent $StockComponent"
Write-Host '  Option B (direct ADB commands):'
if ($DisableStock) {
    Write-Host "    $AdbPrefix shell pm enable $StockPackage"
}
Write-Host "    $AdbPrefix shell pm disable-user --user 0 $HomeAlias"
Write-Host "    $AdbPrefix shell cmd package set-home-activity $StockComponent"
if ($DryRun -or $ApiLevel -ge 29) {
    Write-Host "    $AdbPrefix shell cmd role add-role-holder android.app.role.HOME $StockPackage"
}
Write-Host ''

if ($DryRun) {
    Write-Host '[DRY RUN] Complete. No device settings were modified.'
    exit 0
}

if (-not (Confirm-NeonAction -Prompt "Apply non-destructive default HOME settings on $ResolvedTarget?")) {
    exit 1
}

if ($DisableStock) {
    Write-Host ''
    Write-Warning "WARNING 1/2: You passed -DisableStock. Disabling '$StockPackage' removes the built-in TV fallback launcher."
    Write-Host "Exact command to re-enable your stock launcher: $AdbPrefix shell pm enable $StockPackage"
    if (-not (Confirm-NeonAction -Prompt "Confirm (1/2) you understand the risk of disabling $StockPackage?")) {
        exit 1
    }

    Write-Host ''
    Write-Warning "WARNING 2/2: Final confirmation before running 'pm disable-user --user 0 $StockPackage'."
    Write-Host "Re-enable command: $AdbPrefix shell pm enable $StockPackage"
    if (-not (Confirm-NeonAction -Prompt "Confirm (2/2) disable $StockPackage now?")) {
        exit 1
    }
}

Write-Host "Enabling $HomeAlias..."
$null = Invoke-AdbShell -TargetSerial $ResolvedTarget -ShellArgs @('pm', 'enable', $HomeAlias)

Write-Host 'Setting HOME activity via cmd package set-home-activity...'
& adb -s $ResolvedTarget shell cmd package set-home-activity $HomeAlias
if ($LASTEXITCODE -ne 0) {
    Write-Warning "'cmd package set-home-activity' failed or is unsupported on this firmware."
}

if ($ApiLevel -ge 29) {
    Write-Host "Adding HOME role holder via cmd role (API $ApiLevel)..."
    & adb -s $ResolvedTarget shell cmd role add-role-holder android.app.role.HOME $AppPackage
    if ($LASTEXITCODE -ne 0) {
        Write-Warning "'cmd role add-role-holder' failed or is restricted on this firmware."
    }
}

if ($DisableStock) {
    Write-Host "Disabling stock launcher $StockPackage for user 0..."
    & adb -s $ResolvedTarget shell pm disable-user --user 0 $StockPackage
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to disable $StockPackage."
    }
    Write-Host "Reminder: To re-enable later, run: $AdbPrefix shell pm enable $StockPackage"
}

Write-Host ''
Write-Host 'Done. Press the HOME button on your TV remote to verify.'
Write-Host 'If the TV still opens the vendor launcher, review docs\INSTALL.md and docs\COMPATIBILITY.md.'
