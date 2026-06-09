param(
    [string]$PrivateKeyPath = "keystore/locationmax-license-private.pem",
    [int]$Days = 10,
    [string]$Note = ""
)

if ($Days -lt 1 -or $Days -gt 10) {
    throw "Days must be between 1 and 10."
}

if (-not [System.IO.Path]::IsPathRooted($PrivateKeyPath)) {
    $candidates = @(
        (Join-Path (Get-Location) $PrivateKeyPath),
        (Join-Path $PSScriptRoot "..\keystore\locationmax-license-private.pem"),
        (Join-Path $PSScriptRoot "..\work\XposedFakeLocation\keystore\locationmax-license-private.pem")
    )
    $resolved = $candidates | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
    if ($resolved) {
        $PrivateKeyPath = $resolved
    }
}

$java = Get-Command java -ErrorAction SilentlyContinue
if ($null -eq $java) {
    throw "Java was not found in PATH. Install/use the same JDK used to build LocationMax."
}

$toolPath = Join-Path $PSScriptRoot "LocationMaxCodeGenerator.java"
if (-not (Test-Path -LiteralPath $toolPath)) {
    throw "Generator source not found: $toolPath"
}

& $java.Source $toolPath $PrivateKeyPath $Days $Note
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}
