<#
.SYNOPSIS
Generate a PKCS12 release signing keystore for Neon Launcher.

.DESCRIPTION
Creates keystore\release.jks (gitignored) using keytool with interactive
password entry (never logging passwords to disk) and prints instructions
for setting KEYSTORE_BASE64 and related GitHub Actions Secrets.
#>
[CmdletBinding()]
param(
    [string]$Output = '',

    [string]$Alias = 'neonlauncher',

    [int]$Validity = 10000,

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
$RepoRoot = (Resolve-Path (Join-Path $ScriptDir '..')).Path
. (Join-Path $ScriptDir '_common.ps1')

$null = Get-NeonConfig -ScriptDir $ScriptDir

if ([string]::IsNullOrWhiteSpace($Output)) {
    $Output = Join-Path $RepoRoot 'keystore\release.jks'
}

if ($Validity -le 0) {
    throw '-Validity must be a positive integer.'
}

function Show-SecretInstructions {
    param(
        [string]$KeystorePath,
        [string]$AliasName
    )
    Write-Host 'Next steps - Configure GitHub Actions Secrets (Settings -> Secrets and variables -> Actions):'
    Write-Host ''
    Write-Host '1. Encode your keystore to single-line Base64 (without writing passwords to disk):'
    Write-Host "   [Convert]::ToBase64String([IO.File]::ReadAllBytes('$KeystorePath'))"
    Write-Host ''
    Write-Host '2. Create these 4 repository secrets:'
    Write-Host '   - KEYSTORE_BASE64   : (paste the single-line Base64 output from step 1)'
    Write-Host '   - KEYSTORE_PASSWORD : (the keystore password you entered interactively)'
    Write-Host "   - KEY_ALIAS         : $AliasName"
    Write-Host '   - KEY_PASSWORD      : (the key password you entered interactively)'
    Write-Host ''
    Write-Host "Keep '$KeystorePath' backed up securely offline and NEVER commit it to git."
}

Write-Host '=== Neon Launcher Keystore Generator ==='
Write-Host "Output path  : $Output"
Write-Host 'Keystore type: PKCS12'
Write-Host "Key alias    : $Alias"
Write-Host "Validity     : $Validity days"
Write-Host ''
Write-Host 'Rollback / removal command if you decide not to keep the new keystore:'
Write-Host "  Remove-Item -LiteralPath '$Output'"
Write-Host ''

if ($DryRun) {
    $ParentDir = Split-Path -Parent $Output
    Write-Host '[DRY RUN] Planned command:'
    Write-Host "  New-Item -ItemType Directory -Force -Path '$ParentDir'"
    Write-Host "  keytool -genkeypair -v -keystore '$Output' -storetype PKCS12 -alias '$Alias' -keyalg RSA -keysize 2048 -validity $Validity"
    Write-Host ''
    Show-SecretInstructions -KeystorePath $Output -AliasName $Alias
    Write-Host '[DRY RUN] Complete. No keystore file was created.'
    exit 0
}

Assert-CommandAvailable -Name 'keytool' -Hint 'Install JDK 17 (or newer) and ensure keytool.exe is in PATH.'

if (Test-Path -LiteralPath $Output) {
    Write-Warning "Keystore file already exists at: $Output"
    Write-Host 'Replacing an existing release keystore breaks seamless updates for previously signed APKs.'
    if (-not (Confirm-NeonAction -Prompt "Overwrite existing keystore at '$Output'?")) {
        exit 1
    }
    Remove-Item -LiteralPath $Output -Force
} else {
    if (-not (Confirm-NeonAction -Prompt "Generate new PKCS12 keystore at '$Output'?")) {
        exit 1
    }
}

$ParentDir = Split-Path -Parent $Output
if (-not [string]::IsNullOrWhiteSpace($ParentDir) -and -not (Test-Path -LiteralPath $ParentDir)) {
    $null = New-Item -ItemType Directory -Path $ParentDir -Force
}

Write-Host ''
Write-Host 'Launching keytool (passwords are entered directly to keytool and are NOT logged to disk)...'
& keytool -genkeypair -v -keystore $Output -storetype PKCS12 -alias $Alias -keyalg RSA -keysize 2048 -validity $Validity
if ($LASTEXITCODE -ne 0) {
    throw "keytool failed with exit code $LASTEXITCODE."
}

Write-Host ''
Write-Host "Keystore created at: $Output"
Write-Host ''
Show-SecretInstructions -KeystorePath $Output -AliasName $Alias
