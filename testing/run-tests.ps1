<#
====================================================================
 run-tests.ps1 - ciclo completo de pruebas de JPRewards
====================================================================
 Hace todo el ciclo sin intervencion manual:

   1. Compila JPRewards            (mvn -q -B clean package)
   2. Compila el harness           (testing\harness, Java 8)
   3. Copia los dos jars a los dos servidores de .test-servers\
   4. Arranca cada servidor en modo consola con un timeout
      (por defecto 180 s) y guarda el log en testing\logs\
   5. Extrae las lineas CHECK/HARNESS y saca un resumen con los fallos

 El harness apaga el servidor el solo (Bukkit.shutdown). Si algo se cuelga,
 el script mata el proceso (y su arbol) al vencer el timeout, asi que se puede
 ejecutar las veces que haga falta sin dejar procesos colgados.

 Ejemplos:
   .\testing\run-tests.ps1
   .\testing\run-tests.ps1 -TimeoutSeconds 240
   .\testing\run-tests.ps1 -Servers paper-1.8.8
   .\testing\run-tests.ps1 -SkipJPRewards -SkipBuild     (solo el harness)
   .\testing\run-tests.ps1 -KeepPluginData               (no borra plugins\JPRewards)

 Nota: el archivo se mantiene en ASCII a proposito (PowerShell 5.1 lee los
 .ps1 sin BOM como ANSI; con acentos se verian mal).
#>
#Requires -Version 5.1
[CmdletBinding()]
param(
    [int]$TimeoutSeconds = 180,
    [switch]$SkipBuild,
    [switch]$SkipJPRewards,
    [switch]$KeepPluginData,
    [string[]]$Servers = @()
)

$ErrorActionPreference = 'Stop'

$testing = $PSScriptRoot
$raiz = Split-Path -Parent $testing
$servidoresDir = Join-Path $raiz '.test-servers'
$logsDir = Join-Path $testing 'logs'
$sello = Get-Date -Format 'yyyyMMdd-HHmmss'

if (-not (Test-Path -LiteralPath $logsDir)) {
    New-Item -ItemType Directory -Force -Path $logsDir | Out-Null
}

$resumen = New-Object System.Collections.Generic.List[string]
$resultadoGlobal = 0

function Escribir {
    param([string]$Texto)
    Write-Host $Texto
    $resumen.Add($Texto)
}

function EscribirLinea {
    Escribir ('-' * 72)
}

function Resolver-Java8 {
    $candidatos = New-Object System.Collections.Generic.List[string]
    if ($env:JPR_JDK8) {
        $candidatos.Add($env:JPR_JDK8)
        $candidatos.Add((Join-Path $env:JPR_JDK8 'bin\java.exe'))
    }
    $candidatos.Add((Join-Path $servidoresDir 'tools\jdk8\jdk8u504-b01\bin\java.exe'))
    $candidatos.Add('C:\Users\Usuario\Downloads\Plugins\JPPlayerVaults\.test-servers\tools\jdk8\jdk8u504-b01\bin\java.exe')
    foreach ($candidato in $candidatos) {
        if (-not $candidato) { continue }
        if ((Test-Path -LiteralPath $candidato -PathType Leaf) -and ($candidato -like '*java.exe')) {
            return (Resolve-Path -LiteralPath $candidato).Path
        }
    }
    return $null
}

function Resolver-JavaModerno {
    $candidatos = New-Object System.Collections.Generic.List[string]
    if ($env:JPR_JAVA21) {
        $candidatos.Add($env:JPR_JAVA21)
        $candidatos.Add((Join-Path $env:JPR_JAVA21 'bin\java.exe'))
    }
    $orden = Get-Command java -ErrorAction SilentlyContinue
    if ($orden) { $candidatos.Add($orden.Source) }
    foreach ($candidato in $candidatos) {
        if (-not $candidato) { continue }
        if ((Test-Path -LiteralPath $candidato -PathType Leaf) -and ($candidato -like '*java.exe')) {
            return (Resolve-Path -LiteralPath $candidato).Path
        }
    }
    return $null
}

function Detener-Proceso {
    param([System.Diagnostics.Process]$Proceso)
    if ($null -eq $Proceso) { return }
    try {
        try {
            $Proceso.Kill($true) | Out-Null
        } catch {
            $Proceso.Kill() | Out-Null
        }
    } catch {
        try { & taskkill.exe /T /F /PID $Proceso.Id 2>&1 | Out-Null } catch { }
    }
    try { $Proceso.WaitForExit(15000) | Out-Null } catch { }
}

function Extraer-LineasHarness {
    param([string]$Texto)
    $limpio = $Texto -replace "\x1B\[[0-9;]*[A-Za-z]", ''
    $patron = '(CHECK (OK|FAILED|SKIPPED):.*|HARNESS (RESULT|PASS|FAIL|INFO|FALLOS|SALTADOS).*)'
    $lineas = New-Object System.Collections.Generic.List[string]
    foreach ($linea in ($limpio -split '\r?\n')) {
        $encontrado = [regex]::Match($linea, $patron)
        if ($encontrado.Success) {
            $lineas.Add($encontrado.Value.TrimEnd())
        }
    }
    return $lineas
}

function Ejecutar-Servidor {
    param(
        [string]$Nombre,
        [string]$Carpeta,
        [string]$Jar,
        [string]$Java,
        [int]$TimeoutSegundos
    )
    $logServidor = Join-Path $logsDir ('{0}-{1}.log' -f $Nombre, $sello)
    $argumentos = @('-Xms512M', '-Xmx1024M', '-Dfile.encoding=UTF-8', '-jar', $Jar, 'nogui') -join ' '
    Escribir ('[{0}] arrancando: {1} {2}' -f $Nombre, $Java, $argumentos)
    Escribir ('[{0}] log: {1}' -f $Nombre, $logServidor)

    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = $Java
    $psi.Arguments = $argumentos
    $psi.WorkingDirectory = $Carpeta
    $psi.UseShellExecute = $false
    $psi.CreateNoWindow = $true
    $psi.RedirectStandardInput = $true
    $psi.RedirectStandardOutput = $true
    $psi.RedirectStandardError = $true
    $psi.StandardOutputEncoding = [System.Text.Encoding]::UTF8
    $psi.StandardErrorEncoding = [System.Text.Encoding]::UTF8

    $proceso = $null
    $timeout = $false
    $texto = ''
    try {
        $proceso = [System.Diagnostics.Process]::Start($psi)
        $salida = $proceso.StandardOutput.ReadToEndAsync()
        $errores = $proceso.StandardError.ReadToEndAsync()
        $terminado = $proceso.WaitForExit($TimeoutSegundos * 1000)
        if (-not $terminado) {
            $timeout = $true
            Escribir ('[{0}] TIMEOUT de {1} s: matando el proceso' -f $Nombre, $TimeoutSegundos)
            Detener-Proceso -Proceso $proceso
        }
        $null = $salida.Wait(15000)
        $null = $errores.Wait(15000)
        if ($salida.IsCompleted) { $texto += $salida.Result }
        if ($errores.IsCompleted) { $texto += [Environment]::NewLine + $errores.Result }
    } finally {
        if ($null -ne $proceso -and -not $proceso.HasExited) {
            Detener-Proceso -Proceso $proceso
        }
    }

    $codigo = 'n/d'
    if ($null -ne $proceso -and $proceso.HasExited) { $codigo = $proceso.ExitCode }
    $texto | Set-Content -LiteralPath $logServidor -Encoding UTF8

    $lineas = Extraer-LineasHarness -Texto $texto
    $checks = @($lineas | Where-Object { $_ -like 'CHECK *' })
    $fallos = @($lineas | Where-Object { $_ -like 'CHECK FAILED:*' })
    $saltados = @($lineas | Where-Object { $_ -like 'CHECK SKIPPED:*' })
    $oks = @($lineas | Where-Object { $_ -like 'CHECK OK:*' })
    $pasa = @($lineas | Where-Object { $_ -eq 'HARNESS PASS' }).Count -gt 0
    $falla = @($lineas | Where-Object { $_ -eq 'HARNESS FAIL' }).Count -gt 0
    $resultadoHarness = @($lineas | Where-Object { $_ -like 'HARNESS RESULT*' })

    Escribir ''
    Escribir ('[{0}] salida del harness ({1} lineas):' -f $Nombre, $lineas.Count)
    foreach ($linea in $lineas) { Escribir ('  ' + $linea) }
    Escribir ''
    Escribir ('[{0}] resumen: exit={1} timeout={2} checks={3} ok={4} failed={5} skipped={6}' -f $Nombre, $codigo, $timeout, $checks.Count, $oks.Count, $fallos.Count, $saltados.Count)
    if ($fallos.Count -gt 0) {
        Escribir ('[{0}] FALLOS:' -f $Nombre)
        foreach ($fallo in $fallos) { Escribir ('  ' + $fallo) }
    }
    if ($resultadoHarness.Count -eq 0) {
        Escribir ('[{0}] ERROR: el harness no imprimio HARNESS RESULT (revisa {1})' -f $Nombre, $logServidor)
    }

    $veredicto = 'FALLO'
    if ($pasa -and -not $falla -and -not $timeout) { $veredicto = 'PASA' }
    Escribir ('[{0}] veredicto: {1}' -f $Nombre, $veredicto)
    return [pscustomobject]@{
        Nombre    = $Nombre
        Veredicto = $veredicto
        Log       = $logServidor
        Check     = $checks.Count
        Ok        = $oks.Count
        Fallos    = $fallos.Count
        Saltados  = $saltados.Count
        Timeout   = $timeout
        Exit      = $codigo
        Lineas    = $lineas
    }
}

# --------------------------------------------------------------------
# Cabecera
# --------------------------------------------------------------------
EscribirLinea
Escribir ' JPRewards - ciclo de pruebas'
EscribirLinea
Escribir ('fecha        : ' + (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'))
Escribir ('raiz         : ' + $raiz)
Escribir ('servidores   : ' + $servidoresDir)
Escribir ('logs         : ' + $logsDir)
Escribir ('timeout      : ' + $TimeoutSeconds + ' s por servidor')
Escribir ''

$java8 = Resolver-Java8
$javaModerno = Resolver-JavaModerno
if ($java8) { Escribir ('java 8       : ' + $java8) } else { Escribir 'java 8       : NO ENCONTRADO' }
if ($javaModerno) { Escribir ('java moderno : ' + $javaModerno) } else { Escribir 'java moderno : NO ENCONTRADO' }
Escribir ''

$definiciones = @(
    [pscustomobject]@{
        Nombre   = 'paper-1.8.8'
        Carpeta  = (Join-Path $servidoresDir 'paper-1.8.8')
        Jar      = 'paper-1.8.8.jar'
        Java     = $java8
        Etiqueta = 'Paper 1.8.8 (JDK 8)'
    },
    [pscustomobject]@{
        Nombre   = 'spigot-1.21.1'
        Carpeta  = (Join-Path $servidoresDir 'spigot-1.21.1')
        Jar      = 'spigot-1.21.1.jar'
        Java     = $javaModerno
        Etiqueta = 'Spigot 1.21.1 (Java 21)'
    }
)
if ($Servers.Count -gt 0) {
    $definiciones = @($definiciones | Where-Object { $Servers -contains $_.Nombre })
    if ($definiciones.Count -eq 0) {
        Escribir 'ERROR: -Servers no coincide con ningun servidor conocido (paper-1.8.8, spigot-1.21.1)'
        exit 2
    }
}

# --------------------------------------------------------------------
# 1. Compilar JPRewards
# --------------------------------------------------------------------
$jarJPRewards = $null
if ($SkipJPRewards) {
    Escribir 'JPRewards    : no se compila ni se copia (-SkipJPRewards)'
} elseif ($SkipBuild) {
    Escribir 'JPRewards    : no se compila (-SkipBuild)'
    $candidatos = @(Get-ChildItem -LiteralPath (Join-Path $raiz 'target') -Filter 'JPRewards-*.jar' -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notmatch '(sources|javadoc|original)' })
    if ($candidatos.Count -gt 0) {
        $jarJPRewards = ($candidatos | Sort-Object LastWriteTime -Descending | Select-Object -First 1).FullName
    }
} else {
    $mvn = (Get-Command mvn -ErrorAction SilentlyContinue)
    if (-not $mvn) {
        Escribir 'ERROR: no se encontro mvn en el PATH.'
        exit 2
    }
    $logMaven = Join-Path $logsDir ('build-jprewards-' + $sello + '.log')
    Escribir ('JPRewards    : mvn -q -B clean package  (log: ' + $logMaven + ')')
    $salidaMaven = & $mvn.Source -q -B -f (Join-Path $raiz 'pom.xml') clean package 2>&1
    $codigoMaven = $LASTEXITCODE
    ($salidaMaven | Out-String) | Set-Content -LiteralPath $logMaven -Encoding UTF8
    if ($codigoMaven -ne 0) {
        Escribir ''
        Escribir ('JPRewards NO COMPILA (mvn clean package devolvio ' + $codigoMaven + '). Ultimas lineas:')
        $salidaMaven | Select-Object -Last 40 | ForEach-Object { Escribir ('  ' + $_) }
        Escribir ''
        Escribir ('log completo: ' + $logMaven)
        Escribir 'No se arrancan los servidores: sin jar no hay nada que probar.'
        ($resumen -join [Environment]::NewLine) | Set-Content -LiteralPath (Join-Path $logsDir ('resumen-' + $sello + '.txt')) -Encoding UTF8
        exit 1
    }
    $candidatos = @(Get-ChildItem -LiteralPath (Join-Path $raiz 'target') -Filter 'JPRewards-*.jar' -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notmatch '(sources|javadoc|original)' })
    if ($candidatos.Count -eq 0) {
        Escribir 'ERROR: la compilacion termino bien pero no hay target\JPRewards-*.jar'
        exit 1
    }
    $jarJPRewards = ($candidatos | Sort-Object LastWriteTime -Descending | Select-Object -First 1).FullName
    Escribir ('JPRewards    : OK -> ' + $jarJPRewards)
}

# --------------------------------------------------------------------
# 2. Compilar el harness
# --------------------------------------------------------------------
$jarHarness = Join-Path $testing 'harness\target\Harness.jar'
if ($SkipBuild) {
    Escribir 'harness      : no se compila (-SkipBuild)'
} else {
    $mvn = (Get-Command mvn -ErrorAction SilentlyContinue)
    if (-not $mvn) {
        Escribir 'ERROR: no se encontro mvn en el PATH.'
        exit 2
    }
    $logMavenHarness = Join-Path $logsDir ('build-harness-' + $sello + '.log')
    Escribir ('harness      : mvn -q -B clean package  (log: ' + $logMavenHarness + ')')
    $salidaHarness = & $mvn.Source -q -B -f (Join-Path $testing 'harness\pom.xml') clean package 2>&1
    $codigoHarness = $LASTEXITCODE
    ($salidaHarness | Out-String) | Set-Content -LiteralPath $logMavenHarness -Encoding UTF8
    if ($codigoHarness -ne 0) {
        Escribir 'ERROR: el harness no compila.'
        $salidaHarness | Select-Object -Last 40 | ForEach-Object { Escribir ('  ' + $_) }
        exit 1
    }
}
if (-not (Test-Path -LiteralPath $jarHarness)) {
    Escribir ('ERROR: no existe ' + $jarHarness)
    exit 1
}
Escribir ('harness      : OK -> ' + $jarHarness)
Escribir ''

# --------------------------------------------------------------------
# 3, 4 y 5. Copiar y arrancar cada servidor
# --------------------------------------------------------------------
$resultados = New-Object System.Collections.Generic.List[object]
foreach ($servidor in $definiciones) {
    EscribirLinea
    Escribir (' SERVIDOR: ' + $servidor.Etiqueta + '  (' + $servidor.Nombre + ')')
    EscribirLinea
    if (-not (Test-Path -LiteralPath $servidor.Carpeta)) {
        Escribir ('ERROR: no existe la carpeta ' + $servidor.Carpeta)
        $resultadoGlobal = 1
        continue
    }
    if (-not (Test-Path -LiteralPath (Join-Path $servidor.Carpeta $servidor.Jar))) {
        Escribir ('ERROR: no existe ' + (Join-Path $servidor.Carpeta $servidor.Jar))
        $resultadoGlobal = 1
        continue
    }
    if (-not $servidor.Java) {
        Escribir 'ERROR: no se encontro el java necesario para este servidor.'
        $resultadoGlobal = 1
        continue
    }
    $carpetaPlugins = Join-Path $servidor.Carpeta 'plugins'
    if (-not (Test-Path -LiteralPath $carpetaPlugins)) {
        New-Item -ItemType Directory -Force -Path $carpetaPlugins | Out-Null
    }
    Get-ChildItem -LiteralPath $carpetaPlugins -Filter '*.jar' -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -like 'JPRewards*' -or $_.Name -eq 'Harness.jar' } |
        ForEach-Object { Remove-Item -LiteralPath $_.FullName -Force }
    if (-not $KeepPluginData) {
        $datos = Join-Path $carpetaPlugins 'JPRewards'
        if (Test-Path -LiteralPath $datos) {
            Remove-Item -LiteralPath $datos -Recurse -Force -ErrorAction SilentlyContinue
            Escribir ('[{0}] plugins\JPRewards borrado (los archivos por defecto se prueban de cero)' -f $servidor.Nombre)
        }
    }
    Copy-Item -LiteralPath $jarHarness -Destination (Join-Path $carpetaPlugins 'Harness.jar') -Force
    if ($jarJPRewards) {
        Copy-Item -LiteralPath $jarJPRewards -Destination (Join-Path $carpetaPlugins (Split-Path -Leaf $jarJPRewards)) -Force
        $nombreJPR = Split-Path -Leaf $jarJPRewards
        Escribir ('[{0}] copiados {1} y Harness.jar en plugins\' -f $servidor.Nombre, $nombreJPR)
    } else {
        Escribir ('[{0}] copiado Harness.jar en plugins\ (sin JPRewards)' -f $servidor.Nombre)
    }
    $resultado = Ejecutar-Servidor -Nombre $servidor.Nombre -Carpeta $servidor.Carpeta -Jar $servidor.Jar -Java $servidor.Java -TimeoutSegundos $TimeoutSeconds
    $resultados.Add($resultado)
    if ($resultado.Veredicto -ne 'PASA') { $resultadoGlobal = 1 }
    Escribir ''
}

# --------------------------------------------------------------------
# Resumen final
# --------------------------------------------------------------------
EscribirLinea
Escribir ' RESUMEN FINAL'
EscribirLinea
foreach ($resultado in $resultados) {
    $lineaResumen = '{0,-16} {1,-6} checks={2,-3} ok={3,-3} failed={4,-3} skipped={5,-3} exit={6}' -f $resultado.Nombre, $resultado.Veredicto, $resultado.Check, $resultado.Ok, $resultado.Fallos, $resultado.Saltados, $resultado.Exit
    Escribir $lineaResumen
    Escribir ('                 log: ' + $resultado.Log)
}
$totalFallos = 0
foreach ($resultado in $resultados) { $totalFallos += $resultado.Fallos }
Escribir ''
Escribir ('fallos totales: ' + $totalFallos)
if ($resultadoGlobal -eq 0) {
    Escribir 'RESULTADO: TODO PASA'
} else {
    Escribir 'RESULTADO: HAY FALLOS (revisa las lineas CHECK FAILED y los logs)'
}
$archivoResumen = Join-Path $logsDir ('resumen-' + $sello + '.txt')
($resumen -join [Environment]::NewLine) | Set-Content -LiteralPath $archivoResumen -Encoding UTF8
Escribir ('resumen guardado en: ' + $archivoResumen)
exit $resultadoGlobal
