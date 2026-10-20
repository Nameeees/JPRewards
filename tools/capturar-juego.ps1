# Captura los menus REALES de JPRewards desde un cliente de Minecraft.
#
#   powershell -ExecutionPolicy Bypass -File tools\capturar-juego.ps1
#
# Que hace:
#   1. Instala el plugin de ayuda (testing/capturas) y arranca el servidor de
#      pruebas 1.8.8 con JPRewards (sin el harness, que apaga el servidor).
#   2. Lanza el cliente 1.8.9 instalado en este equipo conectandose solo.
#   3. Ordena al plugin (por archivo) abrir cada menu como el jugador.
#   4. Captura la ventana del cliente y guarda los PNG en docs/assets/capturas.
#
# No usa teclado ni raton: los menus los abre el servidor, asi que no depende
# de que la ventana tenga el foco.

param(
    [string]$Jugador = "Captura",
    [string]$Version = "1.8.9",
    [int]$Puerto = 25580,
    [string]$Salida = "docs\assets\capturas"
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Windows.Forms
Add-Type @'
using System;
using System.Runtime.InteropServices;
public class Cap {
  [StructLayout(LayoutKind.Sequential)] public struct RECT { public int Left, Top, Right, Bottom; }
  [StructLayout(LayoutKind.Sequential)] public struct POINT { public int X, Y; }
  [DllImport("user32.dll")] public static extern bool GetClientRect(IntPtr h, out RECT r);
  [DllImport("user32.dll")] public static extern bool ClientToScreen(IntPtr h, ref POINT p);
  [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr h);
  [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr h, int c);
}
'@

$raiz = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $raiz
$srv = Join-Path $raiz '.test-servers\paper-1.8.8'
$java8 = Join-Path $raiz '.test-servers\tools\jdk8\jdk8u504-b01\bin\java.exe'
$javaw8 = Join-Path $raiz '.test-servers\tools\jdk8\jdk8u504-b01\bin\javaw.exe'
$mc = Join-Path $env:APPDATA '.minecraft'
$gameDir = Join-Path $raiz '.test-servers\cliente-1.8.9'
$destino = Join-Path $raiz $Salida
New-Item -ItemType Directory -Path $destino -Force | Out-Null

function Paso($t) { Write-Host ""; Write-Host "== $t" -ForegroundColor Magenta }
function Bien($t) { Write-Host "   OK  $t" -ForegroundColor Green }

function Parar-Java([string]$patron) {
    Get-Process java, javaw -ErrorAction SilentlyContinue | Where-Object { $_.Path -like $patron } | Stop-Process -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 2
}

function Esperar-Log([string]$archivo, [string]$texto, [int]$segundos) {
    $limite = (Get-Date).AddSeconds($segundos)
    while ((Get-Date) -lt $limite) {
        if (Test-Path $archivo) {
            $contenido = Get-Content $archivo -Raw -ErrorAction SilentlyContinue
            if ($contenido -and $contenido.Contains($texto)) { return $true }
        }
        Start-Sleep -Seconds 2
    }
    return $false
}

function Cliente {
    # la ventana tarda unos segundos en crearse: se reintenta
    for ($intento = 0; $intento -lt 30; $intento++) {
        $proceso = Get-Process javaw, java -ErrorAction SilentlyContinue |
            Where-Object { $_.MainWindowTitle -like '*Minecraft*' -and $_.MainWindowHandle -ne 0 } |
            Select-Object -First 1
        if ($proceso) { return $proceso }
        Start-Sleep -Milliseconds 700
    }
    return $null
}

function Capturar([string]$nombre) {
    $cliente = Cliente
    if (-not $cliente) { throw "no hay ventana de Minecraft (mira si el cliente sigue abierto)" }
    $cliente.Refresh()
    $h = $cliente.MainWindowHandle
    [Cap]::ShowWindow($h, 9) | Out-Null
    [Cap]::SetForegroundWindow($h) | Out-Null
    Start-Sleep -Milliseconds 700
    $rect = New-Object Cap+RECT
    [Cap]::GetClientRect($h, [ref]$rect) | Out-Null
    $punto = New-Object Cap+POINT
    [Cap]::ClientToScreen($h, [ref]$punto) | Out-Null
    $ancho = $rect.Right - $rect.Left
    $alto = $rect.Bottom - $rect.Top
    $bmp = New-Object System.Drawing.Bitmap($ancho, $alto)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.CopyFromScreen($punto.X, $punto.Y, 0, 0, (New-Object System.Drawing.Size($ancho, $alto)))
    $g.Dispose()
    $archivo = Join-Path $destino ($nombre + ".png")
    $bmp.Save($archivo, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Write-Host ("   captura " + $nombre + ".png (" + $ancho + "x" + $alto + ")") -ForegroundColor Gray
}

function Orden([string]$texto) {
    $carpeta = Join-Path $srv 'plugins\Capturas'
    New-Item -ItemType Directory -Path $carpeta -Force | Out-Null
    if (-not (Test-Path (Join-Path $carpeta 'jugador.txt'))) {
        [System.IO.File]::WriteAllText((Join-Path $carpeta 'jugador.txt'), $Jugador)
    }
    [System.IO.File]::WriteAllText((Join-Path $carpeta 'orden.txt'), $texto + [Environment]::NewLine)
    Start-Sleep -Milliseconds 1500
}

Paso "Servidor de pruebas"
Parar-Java '*jdk8*'
Get-ChildItem (Join-Path $srv 'plugins') -Filter '*.off' | ForEach-Object { Move-Item $_.FullName ($_.FullName -replace '\.off$','') -Force }
# el harness no debe estar: apaga el servidor al terminar
$harness = Join-Path $srv 'plugins\Harness.jar'
if (Test-Path $harness) { Move-Item $harness ($harness + '.off') -Force }
Copy-Item (Join-Path $raiz 'testing\capturas\Capturas.jar') (Join-Path $srv 'plugins\Capturas.jar') -Force
$datos = Join-Path $srv 'plugins\JPRewards'
if (Test-Path $datos) { Remove-Item $datos -Recurse -Force }
$capturasPlugin = Join-Path $srv 'plugins\Capturas'
if (Test-Path $capturasPlugin) { Remove-Item $capturasPlugin -Recurse -Force }
$logServidor = Join-Path $srv 'consola-captura.log'
Remove-Item $logServidor -Force -ErrorAction SilentlyContinue
Start-Process -FilePath $java8 -ArgumentList '-Xms512M','-Xmx1G','-jar','paper-1.8.8.jar','nogui' -WorkingDirectory $srv -RedirectStandardOutput $logServidor -RedirectStandardError (Join-Path $srv 'consola-captura.err') -PassThru -WindowStyle Hidden | Out-Null
if (-not (Esperar-Log $logServidor 'Done (' 90)) { throw "el servidor no arranco" }
Bien "servidor listo en el puerto $Puerto"

Paso "Cliente real $Version"
Parar-Java '*javaw*'
$json = Get-Content (Join-Path $mc "versions\$Version\$Version.json") -Raw | ConvertFrom-Json
$cp = New-Object System.Collections.Generic.List[string]
foreach ($lib in $json.libraries) {
    $permitido = $true
    if ($lib.rules) {
        $permitido = $false
        foreach ($r in $lib.rules) {
            $os = if ($r.os) { $r.os.name } else { $null }
            if ($r.action -eq 'allow' -and ($null -eq $os -or $os -eq 'windows')) { $permitido = $true }
            if ($r.action -eq 'disallow' -and $os -eq 'windows') { $permitido = $false }
        }
    }
    if (-not $permitido) { continue }
    if ($lib.downloads -and $lib.downloads.artifact -and $lib.downloads.artifact.path) {
        $cp.Add((Join-Path $mc ('libraries\' + $lib.downloads.artifact.path)))
    }
}
$cp.Add((Join-Path $mc "versions\$Version\$Version.jar"))
New-Item -ItemType Directory -Path $gameDir -Force | Out-Null
$logCliente = Join-Path $gameDir 'logs\latest.log'
Remove-Item $logCliente -Force -ErrorAction SilentlyContinue
$argumentos = @(
    ("-Djava.library.path=" + (Join-Path $mc "versions\$Version\natives")),
    '-cp', ($cp -join ';'), $json.mainClass,
    '--username', $Jugador, '--version', $Version,
    '--gameDir', $gameDir,
    '--assetsDir', (Join-Path $mc 'assets'), '--assetIndex', '1.8',
    '--accessToken', '0', '--userProperties', '{}', '--versionType', 'release',
    '--server', 'localhost', '--port', "$Puerto", '--width', '1280', '--height', '720'
)
Start-Process -FilePath $javaw8 -ArgumentList $argumentos -WorkingDirectory $gameDir | Out-Null
if (-not (Esperar-Log $logCliente 'joined the game' 120)) { throw "el cliente no entro al servidor" }
Bien "cliente dentro del servidor"
Start-Sleep -Seconds 3

Paso "Preparando al jugador"
Orden 'consola:op Captura'
Orden "consola:rewards reset $Jugador all"
Orden "consola:rewards set $Jugador streak 3"
Orden "consola:rewards set $Jugador total 3"
Orden "consola:rewards set $Jugador month-days 6"
Orden "consola:rewards set $Jugador weekly-total 1"
Bien "estado listo (racha 3, dia 4, 6 dias del mes)"

Paso "Capturas"
$menus = @(
    @{ orden = 'cmd:rewards';        archivo = 'menu-hub' },
    @{ orden = 'cmd:rewards daily';   archivo = 'menu-diario' },
    @{ orden = 'cmd:rewards weekly';  archivo = 'menu-semanal' },
    @{ orden = 'cmd:rewards monthly'; archivo = 'menu-mensual' },
    @{ orden = 'cmd:rewards streaks'; archivo = 'menu-rachas' },
    @{ orden = 'cmd:rewards info';    archivo = 'menu-info' }
)
foreach ($menu in $menus) {
    Orden $menu.orden
    Start-Sleep -Milliseconds 1600
    Capturar $menu.archivo
    Orden 'cmd:rewards'
    Start-Sleep -Milliseconds 600
}
Bien "capturas en $Salida"
Get-ChildItem $destino -Filter '*.png' | Select-Object Name, Length | Format-Table -AutoSize | Out-String
