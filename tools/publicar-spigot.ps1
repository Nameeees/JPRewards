# Prepara una actualizacion de JPRewards para SpigotMC.
#
#   powershell -ExecutionPolicy Bypass -File tools\publicar-spigot.ps1 -ResourceId 123456
#
# Hace, en orden:
#   1. Lee la version del pom.xml.
#   2. Compila, valida los recursos y los enlaces de la documentacion.
#   3. Deja el jar listo en dist/ con su sha256.
#   4. Genera dist/TITULO.txt y dist/DESCRIPCION.bbcode desde
#      docs/SPIGOTMC_UPDATE_<version>.md (una sola fuente de verdad).
#   5. Copia la descripcion al portapapeles y abre la pagina del recurso.
#
# Lo unico que queda a mano es pegar el titulo/descripcion y subir el archivo.
# Para eso esta tools/spigot-auto/ (automatizacion completa con el navegador).

param(
    [string]$ResourceId = "",
    [switch]$SkipBuild
)

$ErrorActionPreference = 'Stop'
$raiz = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $raiz

function Paso($texto) { Write-Host ""; Write-Host "== $texto" -ForegroundColor Magenta }
function Bien($texto) { Write-Host "   OK  $texto" -ForegroundColor Green }
function Aviso($texto) { Write-Host "   !!  $texto" -ForegroundColor Yellow }

$fence = [string]::new([char]96, 3)

function LeerBloque($lineas, $desde) {
    $i = $desde
    while ($i -lt $lineas.Count -and -not $lineas[$i].Trim().StartsWith($fence)) { $i++ }
    if ($i -ge $lineas.Count) { return "" }
    $i++
    $bloque = New-Object System.Collections.Generic.List[string]
    while ($i -lt $lineas.Count -and $lineas[$i].Trim() -ne $fence) {
        $bloque.Add($lineas[$i])
        $i++
    }
    return ($bloque -join [Environment]::NewLine).Trim()
}

Paso "Version"
$pom = Get-Content pom.xml -Raw
if ($pom -notmatch '<version>([^<]+)</version>') { throw "No se pudo leer la version del pom.xml" }
$version = $Matches[1]
Bien "version $version"

if (-not $SkipBuild) {
    Paso "Compilando"
    mvn -B -q clean package
    if ($LASTEXITCODE -ne 0) { throw "La compilacion fallo" }
    Bien "jar construido"
}

Paso "Comprobaciones"
python tools/validar-recursos.py | Select-Object -Last 1
if ($LASTEXITCODE -ne 0) { throw "Los recursos no validan" }
python tools/chequear-enlaces.py | Select-Object -Last 1
if ($LASTEXITCODE -ne 0) { throw "Hay enlaces rotos en la documentacion" }
$jarConstruido = Get-ChildItem target\JPRewards-*.jar | Select-Object -First 1
if (-not $jarConstruido) { throw "No hay jar en target/" }
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead($jarConstruido.FullName)
$clase = $zip.Entries | Where-Object { $_.FullName -eq 'me/jp/jprewards/JPRewards.class' }
$zip.Dispose()
if (-not $clase) { throw "El jar no contiene el plugin" }
Bien "recursos, enlaces y jar en orden"

Paso "Paquete para SpigotMC"
$dist = Join-Path $raiz "dist"
if (-not (Test-Path $dist)) { New-Item -ItemType Directory -Path $dist | Out-Null }
$destino = Join-Path $dist "JPRewards-$version.jar"
Copy-Item $jarConstruido.FullName $destino -Force
$hash = (Get-FileHash $destino -Algorithm SHA256).Hash.ToLower()
"$hash  JPRewards-$version.jar" | Set-Content (Join-Path $dist "JPRewards-$version.sha256")
Bien "dist/JPRewards-$version.jar ($((Get-Item $destino).Length) bytes)"
Bien "sha256 $hash"

$notas = Join-Path $raiz "docs/SPIGOTMC_UPDATE_$version.md"
if (-not (Test-Path $notas)) {
    Aviso "No existe docs/SPIGOTMC_UPDATE_$version.md: escribe el titulo y la descripcion de esta version."
    exit 0
}
$lineas = [System.IO.File]::ReadAllLines($notas, [System.Text.Encoding]::UTF8)
$iTitulo = [array]::IndexOf($lineas, '## Title')
$iDesc = [array]::IndexOf($lineas, '## Description (BBCode, paste as-is)')
if ($iTitulo -lt 0 -or $iDesc -lt 0) { throw "No se encontraron '## Title' o '## Description' en $notas" }
$titulo = LeerBloque $lineas $iTitulo
$descripcion = LeerBloque $lineas $iDesc
if (-not $titulo -or -not $descripcion) { throw "El titulo o la descripcion estan vacios en $notas" }
[System.IO.File]::WriteAllText((Join-Path $dist "TITULO.txt"), $titulo, (New-Object System.Text.UTF8Encoding($false)))
[System.IO.File]::WriteAllText((Join-Path $dist "DESCRIPCION.bbcode"), $descripcion, (New-Object System.Text.UTF8Encoding($false)))
Bien "dist/TITULO.txt"
Bien "dist/DESCRIPCION.bbcode"

try {
    Set-Clipboard -Value $descripcion
    Bien "descripcion copiada al portapapeles"
} catch {
    Aviso "no se pudo copiar al portapapeles (abre dist/DESCRIPCION.bbcode)"
}

Paso "Siguiente paso"
if ($ResourceId -and $ResourceId -ne "0") {
    Start-Process "https://www.spigotmc.org/resources/$ResourceId/"
    Bien "abierta la pagina del recurso $ResourceId"
} else {
    Aviso "Pasa -ResourceId <id> para abrir la pagina del recurso automaticamente."
}
Write-Host ""
Write-Host "  1. En SpigotMC: recurso -> 'Post a new update'." -ForegroundColor Gray
Write-Host "  2. Pega el titulo de dist/TITULO.txt y la descripcion (ya esta en el portapapeles)." -ForegroundColor Gray
Write-Host "  3. Sube dist/JPRewards-$version.jar como archivo de la version." -ForegroundColor Gray
Write-Host "  4. Despues: git tag v$version; git push origin v$version" -ForegroundColor Gray
