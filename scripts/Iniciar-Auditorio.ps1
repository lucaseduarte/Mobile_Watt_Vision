param(
    [ValidateRange(1, 65535)]
    [int]$PortaMySql = 3306
)

$ErrorActionPreference = 'Stop'
$raizMobile = Split-Path -Parent $PSScriptRoot
$pastaProjetos = Split-Path -Parent $raizMobile
$backend = Join-Path $pastaProjetos 'back-end_wattVision'
$web = Join-Path $pastaProjetos 'Front_WV'
$node = (Get-Command node -ErrorAction Stop).Source

if (-not (Test-Path (Join-Path $backend 'node_modules\mysql2')) -or
    -not (Test-Path (Join-Path $web 'node_modules\vite\bin\vite.js'))) {
    throw 'Execute npm.cmd ci nas pastas do Backend e do Web antes de iniciar.'
}

foreach ($porta in @(3000, 5173)) {
    $testePorta = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, $porta)
    try {
        $testePorta.Start()
    } catch {
        throw "A porta $porta esta indisponivel. Encerre o servico que a ocupa antes de iniciar."
    } finally {
        $testePorta.Stop()
    }
}

$usuario = Read-Host 'Conta MySQL local do Backend'
$senha = Read-Host 'Senha MySQL da conta do Backend' -AsSecureString
if ([string]::IsNullOrWhiteSpace($usuario) -or $senha.Length -eq 0) {
    throw 'Informe a conta e a senha do MySQL.'
}

$variaveis = @('VITE_API_URL', 'DB_HOST', 'DB_PORT', 'DB_NAME', 'DB_USER', 'DB_PASSWORD', 'JWT_SECRET', 'DEMO_MODE', 'TZ')
$anteriores = @{}
foreach ($nome in $variaveis) {
    $anteriores[$nome] = [Environment]::GetEnvironmentVariable($nome, 'Process')
}

$vite = $null
try {
    $env:VITE_API_URL = 'http://127.0.0.1:3000/api'
    $vite = Start-Process -FilePath $node -WorkingDirectory $web -PassThru -NoNewWindow -ArgumentList @(
        'node_modules\vite\bin\vite.js', '--host', '127.0.0.1', '--port', '5173', '--strictPort'
    )
    $env:DB_HOST = '127.0.0.1'
    $env:DB_PORT = $PortaMySql.ToString()
    $env:DB_NAME = 'db_watt_vision'
    $env:DB_USER = $usuario
    $env:DB_PASSWORD = [pscredential]::new($usuario, $senha).GetNetworkCredential().Password
    $env:JWT_SECRET = & $node -p "require('node:crypto').randomBytes(32).toString('hex')"
    $env:DEMO_MODE = 'false'
    $env:TZ = 'America/Sao_Paulo'

    Push-Location $backend
    try {
        & $node (Join-Path $PSScriptRoot 'auditorio-backend.mjs')
        if ($LASTEXITCODE -ne 0) { throw 'O Backend não iniciou. Confira a mensagem acima.' }
    } finally {
        Pop-Location
    }
} finally {
    if ($null -ne $vite -and -not $vite.HasExited) {
        Stop-Process -Id $vite.Id -ErrorAction SilentlyContinue
    }
    foreach ($nome in $variaveis) {
        [Environment]::SetEnvironmentVariable($nome, $anteriores[$nome], 'Process')
    }
}
