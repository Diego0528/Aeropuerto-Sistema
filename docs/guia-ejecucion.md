# Guía de Ejecución — AeroQueue v1.1

## Distribución (.exe) — Uso en producción

A partir de v1.1 cada módulo se empaqueta como carpeta portable con JRE embebido. **No se necesita Java instalado** en las PCs destino.

### Estructura generada por `empaquetar.ps1`

```
dist/
├── AeroQueue-Servidor/
│   ├── AeroQueue-Servidor.exe   ← ejecutar primero
│   └── config.txt
├── AeroQueue-Registro/
│   ├── AeroQueue-Registro.exe
│   └── config.txt
├── AeroQueue-General/
├── AeroQueue-Prioritaria/
├── AeroQueue-Especial/
├── AeroQueue-Logs/
└── AeroQueue-Monitor/
```

### `config.txt` — configuración de red

Cada carpeta contiene su propio `config.txt`. Editarlo **antes de distribuir** a cada PC:

```
ip=192.168.1.100       # IP de la PC donde corre el servidor
puerto=5000
puerto_chat=5001
```

En la PC del servidor la IP puede ser `localhost` o `127.0.0.1`.

### Orden de inicio

1. **Servidor** — ejecutar `AeroQueue-Servidor\AeroQueue-Servidor.exe` primero
2. **Módulos cliente** — en cualquier orden, en cualquier PC de la red
3. **Monitor** — puede iniciarse antes del servidor; reconecta automáticamente cada 5 s
4. **Logs** — ídem

Los módulos que corren en la misma PC que el servidor se conectan automáticamente vía `localhost` si la IP configurada no responde.

### Generar los ejecutables

```powershell
# Desde la raíz del proyecto en IntelliJ / PowerShell
.\empaquetar.ps1
```

Requiere haber compilado antes (ver sección de compilación). Genera todo en `dist/`.

---

## Desarrollo — Compilar y ejecutar desde fuente

### Requisitos

- Java 21 (JDK de IntelliJ IDEA — no necesita estar en PATH)
- Maven (bundled de IntelliJ IDEA — no necesita estar en PATH)
- IntelliJ IDEA 2024.x (recomendado)

### Compilar todo

```powershell
$env:JAVA_HOME = "C:\Users\ITPORTA\.jdks\openjdk-24.0.1"
$mvn = "C:\Program Files\JetBrains\IntelliJ IDEA 2024.3.5\plugins\maven\lib\maven3\bin\mvn.cmd"
& $mvn clean package
```

### Ejecutar módulos individuales (modo desarrollo)

```powershell
# Servidor
& $mvn exec:java -pl server

# Clientes
& $mvn javafx:run -pl client-registro
& $mvn javafx:run -pl client-general
& $mvn javafx:run -pl client-prioritaria
& $mvn javafx:run -pl client-especial
& $mvn javafx:run -pl client-logs
& $mvn javafx:run -pl client-monitor
```

---

## Módulos del sistema

| Módulo | Tipo | Puerto | Descripción |
|---|---|---|---|
| `server` | Servidor | 5000 + 5001 | Servidor central y servidor de chat |
| `client-registro` | Cliente | 5000 | Kiosko de registro de pasajeros |
| `client-general` | Cliente | 5000 | Ventanilla cola General |
| `client-prioritaria` | Cliente | 5000 | Ventanilla cola Prioritaria |
| `client-especial` | Cliente | 5000 | Ventanilla cola Especial/VIP |
| `client-logs` | Monitor | 5000 (push) | Visor de logs en tiempo real + exportación JSON |
| `client-monitor` | Monitor | 5000 (push) | Árbol de conexiones en tiempo real; lanza servidor |

---

## Demo rápida (flujo completo)

1. Inicia el servidor (`.exe` o desde IntelliJ)
2. Abre el kiosko de registro — aparecerá "Conectado"
3. Ingresa un DPI de prueba (ej: `1234567890101`) y presiona Enter
4. El nombre se autocompleta desde RENAP: "Carlos García López"
5. Selecciona "General" y presiona "Registrar en Cola"
6. El sistema confirma: "Registrado. Turno #1 — Cola GENERAL"
7. Abre la ventanilla General
8. Presiona "Llamar Siguiente" — aparece el turno y el nombre del pasajero
9. Presiona "Finalizar Atención" — el turno se cierra
10. En el Monitor verás todos los módulos conectados en el árbol lateral
