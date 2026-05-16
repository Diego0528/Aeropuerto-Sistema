# AeroQueue v1.1 — Sistema de Colas — Aeropuerto Internacional La Aurora

Sistema de gestión de turnos para aeropuerto, implementado con arquitectura cliente-servidor sobre sockets TCP/IP. Desarrollado en Java 21 con interfaz gráfica JavaFX. Distribuible como `.exe` portable sin necesidad de Java instalado.

**Equipo:** Diego Andrino (servidor / integración) · Pablo Acan (estructuras de datos / client-registro)  
**Versión:** 1.1.0

---

## Descripción general

Los pasajeros se registran en un kiosko ingresando su DPI (el sistema consulta el catálogo RENAP ficticio y autocompleta el nombre). Reciben un número de turno según el tipo de cola elegido. Los operadores en ventanilla atienden cada cola de forma ordenada. El personal supervisor dispone de un visor de logs en tiempo real y un monitor de módulos conectados.

### Tres tipos de cola

| Cola | Pasajeros | Datos que registra el operador |
|---|---|---|
| **General** | Público en general | Vuelo + observaciones |
| **Prioritaria** | Adultos mayores, embarazadas, discapacidad | Vuelo + checklist de asistencia |
| **Especial / VIP** | Pasajeros VIP | Vuelo + clase + asiento + beneficios |

---

## Novedades en v1.1

- **Chat interno** — canal de mensajería entre todos los módulos conectados (puerto 5001)
- **Visor de Logs** (`client-logs`) — muestra eventos en tiempo real y permite exportar a JSON
- **Monitor de Módulos** (`client-monitor`) — árbol de conexiones en tiempo real; puede iniciar/detener el servidor
- **Distribución `.exe`** — empaquetado con JRE embebido vía `jpackage`; el usuario solo ejecuta el `.exe`
- **Auto-reconexión** — todos los módulos reintentan la conexión cada 5 s si el servidor cae
- **Fallback a localhost** — los módulos en la misma PC que el servidor se conectan automáticamente vía loopback

---

## Estructura del proyecto

```
Aeropuerto-Sistema/
├── pom.xml                          # POM padre
├── config.txt                       # Plantilla de configuración (ip, puerto, puerto_chat)
├── empaquetar.ps1                   # Script que genera los .exe en dist/
├── common/                          # Código compartido (sin librerías externas)
│   └── .../com/aeropuerto/common/
│       ├── Cola.java                # Cola genérica (lista enlazada)
│       ├── TablaHash.java           # Tabla hash con encadenamiento O(1)
│       ├── Pasajero.java
│       ├── Mensaje.java             # Protocolo de comunicación
│       ├── TipoMensaje.java
│       ├── ClienteInfo.java         # Info de conexión por módulo
│       ├── RegistroConexiones.java  # Registro de todos los módulos activos
│       ├── ConfigServidor.java      # Lee config.txt
│       ├── LogEntry.java            # DTO de evento de log
│       ├── ChatConexion.java        # Conexión al ChatServer (puerto 5001)
│       └── CatalogoRENAP.java       # Personas ficticias para demos
├── server/                          # Servidor central
│   └── .../com/aeropuerto/server/
│       ├── ServerMain.java          # TCP :5000 + lanza ChatServer :5001
│       ├── ClientHandler.java       # Un hilo por cliente
│       ├── GestorColas.java         # Singleton con las tres colas
│       ├── LogDispatcher.java       # Broadcast de logs a clientes LOGS/MONITOR
│       └── ChatServer.java          # Servidor de chat en hilo separado
├── client-registro/                 # Kiosko de pasajeros
├── client-general/                  # Ventanilla cola General
├── client-prioritaria/              # Ventanilla cola Prioritaria
├── client-especial/                 # Ventanilla cola Especial/VIP
├── client-logs/                     # Visor de logs en tiempo real
├── client-monitor/                  # Monitor de módulos + control del servidor
└── docs/                            # Documentación técnica
    ├── arquitectura.md
    ├── protocolo.md
    ├── flujo-trabajo.md
    ├── estructuras-datos.md
    ├── guia-ejecucion.md
    └── Informe Tecnico.md
```

---

## Distribución y uso (`.exe`)

```
1. Ejecutar empaquetar.ps1 — genera dist/ con una carpeta por módulo
2. Copiar cada carpeta a la PC destino
3. Editar config.txt con la IP del servidor y los puertos
4. En la PC servidor: ejecutar AeroQueue-Servidor\AeroQueue-Servidor.exe
5. En las demás PCs: ejecutar el .exe correspondiente
```

No se requiere Java ni Maven instalado en las PCs de producción.

---

## Tecnologías

- **Java 21**, **JavaFX 21.0.2**, **Maven 3** (multi-módulo)
- **TCP/IP sockets** — puerto 5000 (operaciones + push) y 5001 (chat)
- **jpackage** — empaquetado portable con JRE embebido
- Sin librerías externas — `Cola<T>` y `TablaHash<K,V>` implementadas desde cero

---

## Protocolo de mensajes (resumen)

Formato: `TIPO|campo1|campo2\n`

```
# Identificación al conectar
C→S:  IDENTIFICAR|REGISTRO|PC-Kiosko
S→C:  IDENTIFICAR_OK

# Registro de pasajero
C→S:  REGISTRO|1234567890101|Carlos Garcia|GENERAL
S→C:  CONFIRMACION|1234567890101|7

# Operador llama siguiente
C→S:  LLAMAR_SIGUIENTE|GENERAL
S→C:  PASAJERO_LLAMADO|1234567890101|Carlos Garcia|7

# Push de estado al Monitor
S→Monitor:  STATUS_UPDATE|CONECTADO|192.168.1.10|52341|REGISTRO|PC-Kiosko|2026-05-16 09:15:00

# Chat
C→S (5001):  MSG|PC-Monitor|Servidor listo
S→todos:     BROADCAST|PC-Monitor|Servidor listo
```

Ver [`docs/protocolo.md`](docs/protocolo.md) para la especificación completa.

---

## DPIs de prueba (RENAP ficticio)

| DPI | Nombre |
|---|---|
| `1234567890101` | Carlos Garcia Lopez |
| `2345678901201` | Maria Jose Perez Morales |
| `4567890123401` | Ana Lucia Ramirez Solis |
| `1111111111111` | Diego Andrino Gonzalez |
| `2222222222222` | Andrea Soto Castellanos |
| `3333333333333` | Jose Luis Giron Mendez |
| `4444444444444` | Carmen Alicia Lopez (adulta mayor) |

---

## Documentación técnica

| Documento | Contenido |
|---|---|
| [`docs/arquitectura.md`](docs/arquitectura.md) | Diagrama de módulos, capas y thread model |
| [`docs/protocolo.md`](docs/protocolo.md) | Especificación del protocolo TCP/IP (operaciones + push + chat) |
| [`docs/flujo-trabajo.md`](docs/flujo-trabajo.md) | Flujo completo paso a paso con diagramas |
| [`docs/estructuras-datos.md`](docs/estructuras-datos.md) | Cola y TablaHash — API, complejidad, ejemplos |
| [`docs/guia-ejecucion.md`](docs/guia-ejecucion.md) | Compilar, empaquetar y distribuir |
| [`docs/Informe Tecnico.md`](docs/Informe%20Tecnico.md) | Informe técnico completo del proyecto |

---

## Restricción del proyecto

> `Cola<T>` y `TablaHash<K,V>` implementadas desde cero en `common`.  
> No se permite usar `ArrayList`, `LinkedList`, `HashMap` ni colecciones de Java.

---

## Paquetes Java

| Módulo | Paquete base |
|---|---|
| common | `com.aeropuerto.common` |
| server | `com.aeropuerto.server` |
| client-registro | `com.aeropuerto.registro` |
| client-general | `com.aeropuerto.general` |
| client-prioritaria | `com.aeropuerto.prioritaria` |
| client-especial | `com.aeropuerto.especial` |
| client-logs | `com.aeropuerto.logs` |
| client-monitor | `com.aeropuerto.monitor` |

---

## Git workflow

```
main     ← versiones estables (v1.0.0, v1.1.0, ...)
develop  ← integración continua
```