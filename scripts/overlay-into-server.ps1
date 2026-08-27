# Copy Amilious plugin sources into a 2009Scape Server tree.
# Usage (PowerShell):
#   .\scripts\overlay-into-server.ps1 C:\dev\2009scape
param(
    [Parameter(Mandatory = $true)]
    [string]$ServerRoot
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$Src = Join-Path $Root "src\main\content\amilious"
$Dest = Join-Path $ServerRoot "Server\src\main\content\amilious"
$Probe = Join-Path $ServerRoot "Server\src\main"

if (-not (Test-Path $Probe)) {
    Write-Error "$ServerRoot does not look like a 2009Scape checkout (missing Server\src\main)"
}

New-Item -ItemType Directory -Force -Path $Dest | Out-Null
robocopy $Src $Dest /MIR /NFL /NDL /NJH /NJS /nc /ns /np | Out-Null
if ($LASTEXITCODE -ge 8) {
    Write-Error "robocopy failed with exit $LASTEXITCODE"
}

Write-Host "Overlaid plugins -> $Dest"
Get-ChildItem -Recurse -File $Dest | ForEach-Object { $_.FullName }
