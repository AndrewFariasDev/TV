# Shared helper functions for Neon Launcher PowerShell scripts.
# Dot-sourced by scripts\*.ps1; not meant to be executed directly.

Set-StrictMode -Version Latest

function Get-NeonConfig {
    param([string]$ScriptDir)

    $envFile = Join-Path $ScriptDir 'config.env'
    if (-not (Test-Path -LiteralPath $envFile)) {
        throw "Missing configuration file: $envFile"
    }

    $config = @{
        GITHUB_REPO    = 'OWNER/neon-launcher'
        APP_PACKAGE    = 'dev.neonlauncher'
        HOME_ALIAS     = 'dev.neonlauncher/.HomeAlias'
        MAIN_ACTIVITY  = 'dev.neonlauncher/.MainActivity'
        APK_ASSET_NAME = 'neon-launcher.apk'
    }

    foreach ($rawLine in Get-Content -LiteralPath $envFile) {
        $line = $rawLine.Trim()
        if ([string]::IsNullOrWhiteSpace($line) -or $line.StartsWith('#')) {
            continue
        }
        if ($line -match '^([A-Za-z_][A-Za-z0-9_]*)=(.*)$') {
            $key = $Matches[1]
            $val = $Matches[2].Trim()
            if (($val.StartsWith('"') -and $val.EndsWith('"')) -or ($val.StartsWith("'") -and $val.EndsWith("'"))) {
                $val = $val.Substring(1, $val.Length - 2)
            }
            $config[$key] = $val
        }
    }

    return $config
}

function Assert-CommandAvailable {
    param(
        [string]$Name,
        [string]$Hint = ''
    )
    if (-not (Get-Command -Name $Name -ErrorAction SilentlyContinue)) {
        $msg = "Required command '$Name' was not found in PATH."
        if (-not [string]::IsNullOrWhiteSpace($Hint)) {
            $msg += " $Hint"
        }
        throw $msg
    }
}

function Confirm-NeonAction {
    param([string]$Prompt)

    $reply = Read-Host -Prompt "$Prompt [y/N]"
    if ($reply -match '^(y|yes)$') {
        return $true
    }
    Write-Host 'Aborted by user. No changes were made.'
    return $false
}

function Resolve-AdbTarget {
    param(
        [bool]$DryRun,
        [string]$Target,
        [string]$Serial,
        [bool]$Silent = $false
    )

    if (-not [string]::IsNullOrWhiteSpace($Target) -and -not [string]::IsNullOrWhiteSpace($Serial)) {
        throw 'Specify either a TV IP address OR -Serial, not both.'
    }

    if (-not [string]::IsNullOrWhiteSpace($Serial)) {
        return $Serial
    }

    if (-not [string]::IsNullOrWhiteSpace($Target)) {
        $ipPort = if ($Target.Contains(':')) { $Target } else { "${Target}:5555" }
        if (-not $DryRun) {
            if ($Silent) {
                $null = & adb connect $ipPort 2>&1
            } else {
                & adb connect $ipPort
            }
            $state = (& adb -s $ipPort get-state 2>$null | Out-String).Trim()
            if ($state -ne 'device') {
                throw "Target ADB device is not ready (state: '$state'). Check TV debugging and authorization prompt."
            }
        }
        return $ipPort
    }

    if ($DryRun) {
        return '<device>'
    }

    $lines = & adb devices 2>$null
    $connected = @()
    foreach ($line in $lines) {
        $trimmed = $line.Trim()
        if ([string]::IsNullOrWhiteSpace($trimmed) -or $trimmed.StartsWith('List of devices')) {
            continue
        }
        if ($trimmed -match '^(\S+)\s+device\b') {
            $connected += $Matches[1]
        }
    }

    if ($connected.Count -eq 0) {
        throw 'No connected ADB device found. Pass a TV IP address or -Serial <id>.'
    }
    if ($connected.Count -gt 1) {
        throw "Multiple ADB devices connected ($($connected.Count)). Pass a TV IP address or -Serial <id>."
    }

    return $connected[0]
}

function Invoke-AdbShell {
    param(
        [string]$TargetSerial,
        [string[]]$ShellArgs
    )
    if ([string]::IsNullOrWhiteSpace($TargetSerial) -or $TargetSerial -eq '<device>') {
        $out = & adb shell @ShellArgs 2>$null
    } else {
        $out = & adb -s $TargetSerial shell @ShellArgs 2>$null
    }
    if ($null -eq $out) {
        return ''
    }
    return (($out -join "`n") -replace "`r", '').Trim()
}

function Protect-SensitiveText {
    param([string]$Text)
    if ([string]::IsNullOrEmpty($Text)) {
        return $Text
    }
    $clean = [regex]::Replace($Text, '\b\d{1,3}(\.\d{1,3}){3}\b', '[REDACTED_IP]')
    $clean = [regex]::Replace($clean, '\b([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}\b', '[REDACTED_MAC]')
    $clean = [regex]::Replace($clean, '[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}', '[REDACTED_EMAIL]')
    return $clean
}

function ConvertFrom-LauncherOutput {
    param([string]$RawOutput)

    $results = New-Object System.Collections.Generic.List[string]
    if ([string]::IsNullOrWhiteSpace($RawOutput)) {
        return @()
    }

    $pkg = ''
    $cls = ''
    foreach ($rawLine in ($RawOutput -split "`n")) {
        $line = $rawLine.Trim()
        if ($line -match '^[a-zA-Z0-9._]+/[a-zA-Z0-9._$]+$') {
            if (-not $results.Contains($line)) {
                $results.Add($line)
            }
            continue
        }
        if ($line -match '(?:^|\s)packageName=([^\s]+)') {
            $pkg = $Matches[1]
            continue
        }
        if ($line -match '(?:^|\s)name=([^\s]+)' -and $Matches[1].Contains('.')) {
            $cls = $Matches[1]
            continue
        }
        if ($line -match '^(ActivityInfo:|Activity #\d+:)') {
            if ($pkg -and $cls) {
                $comp = "$pkg/$cls"
                if (-not $results.Contains($comp)) {
                    $results.Add($comp)
                }
            }
            $pkg = ''
            $cls = ''
        }
    }

    if ($pkg -and $cls) {
        $comp = "$pkg/$cls"
        if (-not $results.Contains($comp)) {
            $results.Add($comp)
        }
    }

    return $results.ToArray()
}

function Get-HomeLaunchers {
    param([string]$TargetSerial)

    $candidates = @(
        @('cmd', 'package', 'query-activities', '--brief', '-a', 'android.intent.action.MAIN', '-c', 'android.intent.category.HOME'),
        @('cmd', 'package', 'query-activities', '-a', 'android.intent.action.MAIN', '-c', 'android.intent.category.HOME'),
        @('pm', 'resolve-activity', '--brief', '-a', 'android.intent.action.MAIN', '-c', 'android.intent.category.HOME'),
        @('pm', 'resolve-activity', '-a', 'android.intent.action.MAIN', '-c', 'android.intent.category.HOME')
    )

    foreach ($cmdArgs in $candidates) {
        $raw = Invoke-AdbShell -TargetSerial $TargetSerial -ShellArgs $cmdArgs
        $parsed = @(ConvertFrom-LauncherOutput -RawOutput $raw)
        if ($parsed.Count -gt 0) {
            return $parsed
        }
    }

    return @()
}
