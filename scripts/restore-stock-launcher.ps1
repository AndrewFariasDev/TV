<#
.SYNOPSIS
Safely restore the device's stock TV launcher.

.DESCRIPTION
Re-enables any disabled stock launcher package first, disables
dev.neonlauncher/.HomeAlias, and restores the stock HOME role/activity.
Prints exact commands and re-enable instructions before making changes.
#>
[CmdletBinding()]
param(
    [Parameter(Position = 0)]
    [string]$Target = '',

    [string]$Serial = '',

    [string]$StockPackage = '',

    [string]$StockComponent = '',

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

if (-not [string]::IsNullOrWhiteSpace($StockComponent) -and [string]::IsNullOrWhiteSpace($StockPackage)) {
    $StockPackage = ($StockComponent -split '/')[0]
}

if (-not $DryRun) {
    Assert-CommandAvailable -Name 'adb' -Hint 'Install Android Platform Tools and ensure adb.exe is in PATH.'
}

$ResolvedTarget = Resolve-AdbTarget -DryRun ([bool]$DryRun) -Target $Target -Serial $Serial
$AdbPrefix = if ($ResolvedTarget -and $ResolvedTarget -ne '<device>') { "adb -s $ResolvedTarget" } else { 'adb' }

$ApiLevel = 0
$DisabledStockFound = $false

if (-not $DryRun) {
    $SdkRaw = Invoke-AdbShell -TargetSerial $ResolvedTarget -ShellArgs @('getprop', 'ro.build.version.sdk')
    if ($SdkRaw -match '^\d+$') {
        $ApiLevel = [int]$SdkRaw
    }

    $DisabledRaw = Invoke-AdbShell -TargetSerial $ResolvedTarget -ShellArgs @('pm', 'list', 'packages', '-d')
    $DisabledPkgs = @()
    foreach ($line in ($DisabledRaw -split "`n")) {
        $trimmed = $line.Trim()
        if ($trimmed -match '^package:(.+)$') {
            $pkg = $Matches[1].Trim()
            if ($pkg -ne $AppPackage) {
                $DisabledPkgs += $pkg
            }
        }
    }

    if (-not [string]::IsNullOrWhiteSpace($StockPackage)) {
        if ($DisabledPkgs -contains $StockPackage) {
            $DisabledStockFound = $true
        }
    } else {
        $DisabledLaunchers = @($DisabledPkgs | Where-Object { $_ -match 'tvlauncher|launcherx|tv\.launcher|launcher' })
        if ($DisabledLaunchers.Count -eq 1) {
            $StockPackage = $DisabledLaunchers[0]
            $DisabledStockFound = $true
        } elseif ($DisabledLaunchers.Count -gt 1) {
            Write-Host 'Multiple disabled launcher packages detected:'
            foreach ($dl in $DisabledLaunchers) {
                Write-Host "  - $dl"
            }
            throw 'Re-run with -StockPackage <package> to choose which one to re-enable.'
        }
    }

    if ([string]::IsNullOrWhiteSpace($StockComponent)) {
        $ActiveHomes = @(Get-HomeLaunchers -TargetSerial $ResolvedTarget | Where-Object { $_ -notlike "$AppPackage/*" })
        if (-not [string]::IsNullOrWhiteSpace($StockPackage)) {
            $Match = $ActiveHomes | Where-Object { ($_ -split '/')[0] -eq $StockPackage } | Select-Object -First 1
            if ($Match) { $StockComponent = $Match }
        } elseif ($ActiveHomes.Count -ge 1) {
            $StockComponent = $ActiveHomes[0]
            $StockPackage = ($StockComponent -split '/')[0]
        }
    }
}

$DisplayPkg = if (-not [string]::IsNullOrWhiteSpace($StockPackage)) { $StockPackage } else { '<stock-launcher-pkg>' }
$DisplayComp = if (-not [string]::IsNullOrWhiteSpace($StockComponent)) { $StockComponent } else { '<stock-launcher-pkg>/<stock-launcher-activity>' }

Write-Host '=== Neon Launcher Restore Stock HOME Plan ==='
Write-Host "Target device      : $ResolvedTarget"
Write-Host "Neon HomeAlias     : $HomeAlias"
Write-Host "Stock package      : $DisplayPkg"
Write-Host "Stock component    : $DisplayComp"
Write-Host ''
Write-Host 'Planned commands (re-enables stock launcher BEFORE disabling HomeAlias):'
if ($DryRun -or $DisabledStockFound -or -not [string]::IsNullOrWhiteSpace($StockPackage)) {
    Write-Host "  1. $AdbPrefix shell pm enable $DisplayPkg"
}
Write-Host "  2. $AdbPrefix shell pm disable-user --user 0 $HomeAlias"
if ($DryRun -or -not [string]::IsNullOrWhiteSpace($StockComponent)) {
    Write-Host "  3. $AdbPrefix shell cmd package set-home-activity $DisplayComp"
}
if ($DryRun -or ($ApiLevel -ge 29 -and -not [string]::IsNullOrWhiteSpace($StockPackage))) {
    Write-Host "  4. $AdbPrefix shell cmd role add-role-holder android.app.role.HOME $DisplayPkg   (API 29+)"
}
Write-Host ''
Write-Host 'Command to switch back to Neon Launcher later:'
Write-Host "  $AdbPrefix shell pm enable $HomeAlias"
Write-Host '  .\scripts\set-default-launcher.ps1'
Write-Host ''

if ($DryRun) {
    Write-Host '[DRY RUN] Complete. No device settings were modified.'
    exit 0
}

if (-not (Confirm-NeonAction -Prompt "Restore stock launcher and disable $HomeAlias on $ResolvedTarget?")) {
    exit 1
}

if (-not [string]::IsNullOrWhiteSpace($StockPackage)) {
    Write-Host "Ensuring stock launcher $StockPackage is enabled..."
    & adb -s $ResolvedTarget shell pm enable $StockPackage 2>$null
    if ($LASTEXITCODE -ne 0) {
        $null = & adb -s $ResolvedTarget shell pm enable --user 0 $StockPackage 2>$null
    }
}

Write-Host "Disabling $HomeAlias..."
& adb -s $ResolvedTarget shell pm disable-user --user 0 $HomeAlias 2>$null
if ($LASTEXITCODE -ne 0) {
    $null = & adb -s $ResolvedTarget shell pm disable $HomeAlias 2>$null
}

if ([string]::IsNullOrWhiteSpace($StockComponent)) {
    $ProbedHomes = @(Get-HomeLaunchers -TargetSerial $ResolvedTarget | Where-Object { $_ -notlike "$AppPackage/*" })
    if (-not [string]::IsNullOrWhiteSpace($StockPackage)) {
        $Match = $ProbedHomes | Where-Object { ($_ -split '/')[0] -eq $StockPackage } | Select-Object -First 1
        if ($Match) { $StockComponent = $Match }
    } elseif ($ProbedHomes.Count -ge 1) {
        $StockComponent = $ProbedHomes[0]
        $StockPackage = ($StockComponent -split '/')[0]
    }
}

if (-not [string]::IsNullOrWhiteSpace($StockComponent)) {
    Write-Host "Restoring HOME activity to $StockComponent..."
    $null = & adb -s $ResolvedTarget shell cmd package set-home-activity $StockComponent 2>$null
}

if ($ApiLevel -ge 29 -and -not [string]::IsNullOrWhiteSpace($StockPackage)) {
    Write-Host "Restoring HOME role holder to $StockPackage (API $ApiLevel)..."
    $null = & adb -s $ResolvedTarget shell cmd role add-role-holder android.app.role.HOME $StockPackage 2>$null
}

Write-Host 'Launching HOME intent...'
$null = & adb -s $ResolvedTarget shell am start -a android.intent.action.MAIN -c android.intent.category.HOME 2>$null

Write-Host ''
Write-Host 'Stock launcher restore complete. Press HOME on your remote to confirm.'
Write-Host "To re-enable Neon Launcher later: $AdbPrefix shell pm enable $HomeAlias"
