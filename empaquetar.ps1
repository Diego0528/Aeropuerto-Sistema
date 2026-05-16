# ============================================================
# AeroQueue v1.1 - Script de empaquetado portable (.exe)
# Genera una carpeta por modulo con JRE incluido.
# El usuario solo copia la carpeta y ejecuta el .exe.
# ============================================================

$JAVA_HOME  = "C:\Users\ITPORTA\.jdks\openjdk-24.0.1"
$JPACKAGE   = "$JAVA_HOME\bin\jpackage.exe"
$PROYECTO   = $PSScriptRoot
$DIST       = "$PROYECTO\dist"
$CONFIG_SRC = "$PROYECTO\config.txt"

if (!(Test-Path $JPACKAGE)) {
    Write-Host "[ERROR] jpackage no encontrado en $JPACKAGE" -ForegroundColor Red
    exit 1
}

# Limpiar dist anterior
if (Test-Path $DIST) { Remove-Item $DIST -Recurse -Force }
New-Item $DIST -ItemType Directory -Force | Out-Null

Write-Host "============================================" -ForegroundColor Cyan
Write-Host " AeroQueue v1.1 - Empaquetado" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan

# ── Definicion de modulos ─────────────────────────────────
$modulos = @(
    @{
        nombre     = "AeroQueue-Servidor"
        artifact   = "server"
        jar        = "server-1.0-SNAPSHOT.jar"
        mainClass  = "com.aeropuerto.server.ServerMain"
        consola    = $true
        descripcion= "Servidor principal"
    },
    @{
        nombre     = "AeroQueue-Registro"
        artifact   = "client-registro"
        jar        = "client-registro-1.0-SNAPSHOT.jar"
        mainClass  = "com.aeropuerto.registro.Launcher"
        consola    = $false
        descripcion= "Registro de pasajeros"
    },
    @{
        nombre     = "AeroQueue-General"
        artifact   = "client-general"
        jar        = "client-general-1.0-SNAPSHOT.jar"
        mainClass  = "com.aeropuerto.general.Launcher"
        consola    = $false
        descripcion= "Cola general"
    },
    @{
        nombre     = "AeroQueue-Prioritaria"
        artifact   = "client-prioritaria"
        jar        = "client-prioritaria-1.0-SNAPSHOT.jar"
        mainClass  = "com.aeropuerto.prioritaria.Launcher"
        consola    = $false
        descripcion= "Cola prioritaria"
    },
    @{
        nombre     = "AeroQueue-Especial"
        artifact   = "client-especial"
        jar        = "client-especial-1.0-SNAPSHOT.jar"
        mainClass  = "com.aeropuerto.especial.Launcher"
        consola    = $false
        descripcion= "Cola especial"
    },
    @{
        nombre     = "AeroQueue-Logs"
        artifact   = "client-logs"
        jar        = "client-logs-1.0-SNAPSHOT.jar"
        mainClass  = "com.aeropuerto.logs.Launcher"
        consola    = $false
        descripcion= "Visor de logs"
    },
    @{
        nombre     = "AeroQueue-Monitor"
        artifact   = "client-monitor"
        jar        = "client-monitor-1.0-SNAPSHOT.jar"
        mainClass  = "com.aeropuerto.monitor.Launcher"
        consola    = $false
        descripcion= "Monitor de modulos"
    }
)

# ── Empaquetar cada modulo ────────────────────────────────
$ok = 0
$fail = 0

foreach ($m in $modulos) {
    $jarPath = "$PROYECTO\$($m.artifact)\target\$($m.jar)"
    $inputDir = "$PROYECTO\$($m.artifact)\target"

    if (!(Test-Path $jarPath)) {
        Write-Host "[SKIP] $($m.nombre) - JAR no encontrado: $jarPath" -ForegroundColor Yellow
        $fail++
        continue
    }

    Write-Host ""
    Write-Host "[>>] Empaquetando $($m.nombre) ($($m.descripcion))..." -ForegroundColor Green

    $args = @(
        "--type", "app-image",
        "--name", $m.nombre,
        "--input", $inputDir,
        "--main-jar", $m.jar,
        "--main-class", $m.mainClass,
        "--dest", $DIST,
        "--runtime-image", $JAVA_HOME,
        "--java-options", "-Xmx256m",
        "--java-options", "--add-opens=java.base/java.lang=ALL-UNNAMED",
        "--java-options", "--add-opens=java.base/java.util=ALL-UNNAMED",
        "--app-version", "1.1.0",
        "--vendor", "AeroQueue"
    )

    if ($m.consola) {
        $args += "--win-console"
    }

    & $JPACKAGE @args 2>&1 | ForEach-Object { Write-Host "  $_" }

    $exeDir = "$DIST\$($m.nombre)"
    if (Test-Path "$exeDir\$($m.nombre).exe") {
        # Copiar config.txt al lado del .exe
        if (Test-Path $CONFIG_SRC) {
            Copy-Item $CONFIG_SRC "$exeDir\config.txt" -Force
        }
        Write-Host "[OK] $($m.nombre) -> $exeDir" -ForegroundColor Cyan
        $ok++
    } else {
        Write-Host "[FAIL] $($m.nombre) - no se genero el .exe" -ForegroundColor Red
        $fail++
    }
}

# ── Resumen ───────────────────────────────────────────────
Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host " Resultado: $ok exitosos, $fail fallidos" -ForegroundColor Cyan
Write-Host " Carpeta de salida: $DIST" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan

if ($ok -gt 0) {
    Write-Host ""
    Write-Host "Instrucciones de distribucion:" -ForegroundColor White
    Write-Host "  1. Copia cada carpeta de dist\ a la PC destino" -ForegroundColor Gray
    Write-Host "  2. Edita config.txt con la IP y puerto del servidor" -ForegroundColor Gray
    Write-Host "  3. Ejecuta el .exe (no requiere Java instalado)" -ForegroundColor Gray
    Write-Host "  4. En el servidor: ejecuta AeroQueue-Servidor\AeroQueue-Servidor.exe primero" -ForegroundColor Gray
}