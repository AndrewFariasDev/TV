<#
.SYNOPSIS
Collect non-sensitive device diagnostics via read-only ADB queries.

.DESCRIPTION
Outputs a clean, sanitized Markdown block ready to paste into a GitHub Compatibility Report.
Never queries or prints serial numbers, MAC addresses, IP addresses, or accounts.
#>
[CmdletBinding()]
param(
    [Parameter(Position = 0)]
    [string]$Target = '',

    [string]$Serial = '',

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

if ($DryRun) {
    Write-Host '[DRY RUN] Read-only ADB queries that diagnose.ps1 runs (no device state is modified):'
    Write-Host '  1. adb -s <target> shell getprop ro.product.manufacturer / ro.product.brand / ro.product.model'
    Write-Host '  2. adb -s <target> shell getprop ro.build.version.release / ro.build.version.sdk'
    Write-Host '  3. adb -s <target> shell getprop ro.product.cpu.abi'
    Write-Host '  4. adb -s <target> shell pm has-feature android.software.leanback (fallback: pm list features)'
    Write-Host '  5. adb -s <target> shell cmd package query-activities -a android.intent.action.MAIN -c android.intent.category.HOME (fallback: pm resolve-activity)'
    Write-Host '  6. adb -s <target> shell getprop ro.config.low_ram / dalvik.vm.heapgrowthlimit / /proc/meminfo MemTotal'
    Write-Host '  7. adb -s <target> shell pm path dev.neonlauncher (mapped to Installed / Not installed)'
    Write-Host '  8. adb -s <target> shell cmd package help (probe set-home-activity) & cmd role get-role-holders android.app.role.HOME (read-only)'
    Write-Host '[DRY RUN] No network connection or device query was executed.'
    exit 0
}

Assert-CommandAvailable -Name 'adb' -Hint 'Install Android Platform Tools and ensure adb.exe is in PATH.'
$ResolvedTarget = Resolve-AdbTarget -DryRun $false -Target $Target -Serial $Serial -Silent $true

function Get-DeviceProp {
    param([string]$PropName)
    $val = Invoke-AdbShell -TargetSerial $ResolvedTarget -ShellArgs @('getprop', $PropName)
    if ([string]::IsNullOrWhiteSpace($val)) { return 'unknown' }
    return $val
}

$Manufacturer = Get-DeviceProp 'ro.product.manufacturer'
$Brand = Get-DeviceProp 'ro.product.brand'
$Model = Get-DeviceProp 'ro.product.model'
$AndroidRel = Get-DeviceProp 'ro.build.version.release'
$AndroidSdk = Get-DeviceProp 'ro.build.version.sdk'
$PrimaryAbi = Get-DeviceProp 'ro.product.cpu.abi'
$LowRam = Get-DeviceProp 'ro.config.low_ram'
$HeapLimit = Get-DeviceProp 'dalvik.vm.heapgrowthlimit'

$MemInfo = Invoke-AdbShell -TargetSerial $ResolvedTarget -ShellArgs @('cat', '/proc/meminfo')
$RamMb = 'unknown'
if ($MemInfo -match 'MemTotal:\s+(\d+)\s+kB') {
    $RamMb = "$([math]::Floor([int64]$Matches[1] / 1024)) MiB"
}

$Leanback = Invoke-AdbShell -TargetSerial $ResolvedTarget -ShellArgs @('pm', 'has-feature', 'android.software.leanback')
if ($Leanback -ne 'true' -and $Leanback -ne 'false') {
    $Features = Invoke-AdbShell -TargetSerial $ResolvedTarget -ShellArgs @('pm', 'list', 'features')
    $Leanback = if ($Features -match 'android\.software\.leanback') { 'true' } else { 'false' }
}

$PmPath = Invoke-AdbShell -TargetSerial $ResolvedTarget -ShellArgs @('pm', 'path', $AppPackage)
$NeonStatus = if ($PmPath -match '^package:') { 'Installed' } else { 'Not installed' }

$HomeLaunchers = @(Get-HomeLaunchers -TargetSerial $ResolvedTarget)

$PkgHelp = Invoke-AdbShell -TargetSerial $ResolvedTarget -ShellArgs @('cmd', 'package', 'help')
$SetHomeSupport = if ($PkgHelp -match 'set-home-activity') {
    'Supported (reported by cmd package help)'
} else {
    'Unsupported or unavailable'
}

$RoleOut = Invoke-AdbShell -TargetSerial $ResolvedTarget -ShellArgs @('cmd', 'role', 'get-role-holders', 'android.app.role.HOME')
if ($RoleOut -match "Can't find service|Unknown service|not found|inaccessible") {
    $RoleSupport = 'Unsupported (role service unavailable)'
} elseif (-not [string]::IsNullOrWhiteSpace($RoleOut)) {
    $RoleSupport = 'Supported (read-only probe succeeded)'
} else {
    $RoleHelp = Invoke-AdbShell -TargetSerial $ResolvedTarget -ShellArgs @('cmd', 'role', 'help')
    $RoleSupport = if (-not [string]::IsNullOrWhiteSpace($RoleHelp)) {
        'Supported (read-only probe succeeded)'
    } else {
        'Unknown / Unsupported'
    }
}

$Lines = New-Object System.Collections.Generic.List[string]
$Lines.Add('```markdown')
$Lines.Add('### Neon Launcher Device Diagnostics')
$Lines.Add('')
$Lines.Add("- **Manufacturer / Brand / Model**: $Manufacturer / $Brand / $Model")
$Lines.Add("- **Android Version / API**: Android $AndroidRel (API $AndroidSdk)")
$Lines.Add("- **Primary ABI**: $PrimaryAbi")
$Lines.Add("- **Leanback Feature (``android.software.leanback``)**: $Leanback")
$Lines.Add("- **Low RAM (``ro.config.low_ram``) / RAM / Heap Limit**: $LowRam / $RamMb / $HeapLimit")
$Lines.Add("- **Neon Launcher (``$AppPackage``)**: $NeonStatus")
$Lines.Add('- **Detected HOME Launchers**:')
if ($HomeLaunchers.Count -gt 0) {
    foreach ($comp in $HomeLaunchers) {
        $Lines.Add("  - ``$comp``")
    }
} else {
    $Lines.Add('  - `(none detected)`')
}
$Lines.Add('- **ADB Default-Home Capability Probes (read-only)**:')
$Lines.Add("  - ``cmd package set-home-activity``: $SetHomeSupport")
$Lines.Add("  - ``cmd role (android.app.role.HOME)``: $RoleSupport")
$Lines.Add('```')

$Sanitized = Protect-SensitiveText -Text ($Lines -join "`n")
Write-Output $Sanitized
