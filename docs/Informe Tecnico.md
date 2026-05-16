# INFORME TÉCNICO COMPLETO — SISTEMA AEROQUEUE
## Aeropuerto Internacional — Sistema de Gestión de Colas

**Versión del sistema:** 1.1  
**Fecha del informe:** 12 de mayo de 2026  
**Autor del proyecto:** Diego  
**Tecnología principal:** Java 21 + JavaFX 21.0.2  
**Modelo de arquitectura:** Cliente/Servidor TCP con protocolo de texto plano

---

## TABLA DE CONTENIDOS

1. [Descripción General del Proyecto](#1-descripción-general-del-proyecto)
2. [Arquitectura del Sistema](#2-arquitectura-del-sistema)
3. [Estructura de Módulos Maven](#3-estructura-de-módulos-maven)
4. [Módulo Common — Biblioteca Compartida](#4-módulo-common--biblioteca-compartida)
5. [Módulo Server — Servidor Central](#5-módulo-server--servidor-central)
6. [Módulo client-registro — Kiosco de Registro](#6-módulo-client-registro--kiosco-de-registro)
7. [Módulo client-general — Ventanilla General](#7-módulo-client-general--ventanilla-general)
8. [Módulo client-prioritaria — Ventanilla Prioritaria](#8-módulo-client-prioritaria--ventanilla-prioritaria)
9. [Módulo client-especial — Ventanilla Especial/VIP](#9-módulo-client-especial--ventanilla-especialvip)
10. [Módulo client-logs — Visor de Logs y Base de Datos](#10-módulo-client-logs--visor-de-logs-y-base-de-datos)
11. [Módulo client-monitor — Monitor de Módulos](#11-módulo-client-monitor--monitor-de-módulos)
12. [Protocolo de Comunicación TCP](#12-protocolo-de-comunicación-tcp)
13. [Base de Datos Persistente](#13-base-de-datos-persistente)
14. [Sistema de Logs en Tiempo Real (Push-Based)](#14-sistema-de-logs-en-tiempo-real-push-based)
15. [Auto-Reconexión Automática](#15-auto-reconexión-automática)
16. [Estructuras de Datos Implementadas](#16-estructuras-de-datos-implementadas)
17. [Catálogo RENAP (Simulación)](#17-catálogo-renap-simulación)
18. [Diseño de Interfaz de Usuario](#18-diseño-de-interfaz-de-usuario)
19. [Ciclo de Vida Completo de un Pasajero](#19-ciclo-de-vida-completo-de-un-pasajero)
20. [Distribución y Despliegue](#20-distribución-y-despliegue)
21. [Configuración de Red Local](#21-configuración-de-red-local)
22. [Guía de Operación del Sistema](#22-guía-de-operación-del-sistema)
23. [DPIs de Prueba Disponibles](#23-dpis-de-prueba-disponibles)
24. [Decisiones de Diseño y Justificación](#24-decisiones-de-diseño-y-justificación)

---

## 1. Descripción General del Proyecto

### ¿Qué es AeroQueue?

AeroQueue es un sistema de gestión de colas de atención al pasajero diseñado para el Aeropuerto Internacional La Aurora de Guatemala. El sistema permite organizar y controlar el flujo de pasajeros en tres tipos de ventanilla de atención, registrando su identidad, asignándoles un turno numerado, llamándolos a la ventanilla correspondiente y registrando los resultados de la atención en una base de datos histórica.

El sistema es completamente funcional en red local (LAN), donde una PC central ejecuta el servidor y las demás PCs ejecutan los clientes de operador o supervisión. Cada módulo se distribuye como un ejecutable independiente que no requiere Java instalado en la PC destino.

### Objetivo principal

Digitalizar el proceso de turnos en las ventanillas del aeropuerto, eliminando los sistemas de ticket en papel, proporcionando supervisión en tiempo real del estado del sistema y registrando métricas de atención (duración, vuelo, observaciones) para análisis posterior.

### Tipos de cola del sistema

| Cola | Código | Color identidad | Destinatarios |
|------|--------|-----------------|---------------|
| General | GENERAL | Verde Apple `#30D158` | Pasajeros sin condición especial |
| Prioritaria | PRIORITARIA | Naranja Apple `#FF9500` | Adulto mayor (≥60 años), embarazadas, personas con discapacidad |
| Especial / VIP | ESPECIAL | Azul Apple `#007AFF` | Titulares de tarjeta VIP, diplomáticos, servicios especiales |

### Módulos del sistema

El sistema consta de **8 módulos Maven** que se compilan y distribuyen como unidades independientes:

| Módulo | Tipo | Función |
|--------|------|---------|
| `common` | Biblioteca | Clases compartidas entre servidor y clientes |
| `server` | Servidor | Núcleo central del sistema |
| `client-registro` | Cliente JavaFX | Kiosco de registro de pasajeros |
| `client-general` | Cliente JavaFX | Ventanilla de atención General |
| `client-prioritaria` | Cliente JavaFX | Ventanilla de atención Prioritaria |
| `client-especial` | Cliente JavaFX | Ventanilla de atención Especial/VIP |
| `client-logs` | Cliente JavaFX | Visor de logs + gestor de base de datos |
| `client-monitor` | Cliente JavaFX | Monitor de módulos estilo task manager |

---

## 2. Arquitectura del Sistema

### Modelo general

```
┌─────────────────────────────────────────────────────────────────────┐
│                          RED LOCAL (LAN)                            │
│                                                                     │
│  PC SERVIDOR                                                        │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │                  ServerMain (puerto 5000)                    │   │
│  │  ┌────────────┐  ┌──────────────┐  ┌────────────────────┐    │   │
│  │  │ GestorColas│  │  BaseDatos   │  │ RegistroConexiones │    │   │
│  │  │ 3 colas    │  │ .db en disco │  │ TablaHash monitores│    │   │
│  │  └────────────┘  └──────────────┘  └────────────────────┘    │   │
│  │  ┌────────────┐  ┌──────────────┐                            │   │
│  │  │ LogManager │  │LogInterceptor│  (captura System.out)      │   │
│  │  └────────────┘  └──────────────┘                            │   │
│  └──────────────────────────────────────────────────────────────┘   │
│         ▲ TCP request/response        ▲ TCP push (LOG/STATUS)       │
│         │                             │                             │
│  ┌──────┴──────┐              ┌───────┴──────┐                      │
│  │  CLIENTES   │              │  MONITORES   │                      │
│  │ OPERATIVOS  │              │              │                      │
│  │  Registro   │              │   Logs App   │                      │
│  │  General    │              │ Monitor App  │                      │
│  │ Prioritaria │              └──────────────┘                      │
│  │  Especial   │                                                    │
│  └─────────────┘                                                    │
└─────────────────────────────────────────────────────────────────────┘
```

### Dos modos de comunicación

**Modo request/response** (clientes operativos):
- El cliente envía un mensaje al servidor
- El servidor responde con exactamente un mensaje
- Síncrono, bloqueante por operación

**Modo push** (monitores):
- El cliente se conecta y se identifica como `LOGS` o `MONITOR`
- El servidor le envía inmediatamente todo el historial de logs (buffer de 2000 entradas)
- A partir de ahí, el servidor empuja cada nuevo evento en tiempo real
- El cliente solo envía `PING` de keepalive

### Comunicación de red

- **Protocolo de transporte:** TCP/IP
- **Puerto:** 5000
- **Formato de mensaje:** texto plano, campos separados por `|`, mensaje terminado en `\n`
- **Codificación:** UTF-8
- **Lectura:** `BufferedReader.readLine()` — simple, sin ambigüedades de delimitación

---

## 3. Estructura de Módulos Maven

### POM padre (raíz del proyecto)

```xml
groupId:    com.aeropuerto
artifactId: airport-queue
version:    1.0-SNAPSHOT
packaging:  pom
```

**Propiedades globales:**
- `maven.compiler.source = 21` — Java 21
- `maven.compiler.target = 21`
- `javafx.version = 21.0.2`
- `maven.shade.plugin.version = 3.5.2` — para fat JARs

**Módulos declarados:**
```
common → server → client-registro → client-general → 
client-prioritaria → client-especial → client-logs → client-monitor
```

El módulo `common` es compilado primero y se instala en el repositorio local Maven. Los demás módulos lo declaran como dependencia. Los clientes también declaran `javafx-controls` y `javafx-fxml`.

### Estructura de carpetas

```
Aeropuerto-Sistema/
├── pom.xml                          ← POM padre
├── common/
│   └── src/main/java/com/aeropuerto/common/
│       ├── Mensaje.java
│       ├── TipoMensaje.java
│       ├── Pasajero.java
│       ├── RegistroVisita.java
│       ├── Cola.java
│       ├── TablaHash.java
│       ├── CatalogoRENAP.java
│       ├── TipoAtencion.java
│       ├── EstadoPasajero.java
│       ├── LogEntry.java
│       └── ClienteInfo.java
├── server/
│   └── src/main/java/com/aeropuerto/server/
│       ├── ServerMain.java
│       ├── ClientHandler.java
│       ├── GestorColas.java
│       ├── BaseDatos.java
│       ├── LogManager.java
│       ├── LogInterceptor.java
│       ├── RegistroConexiones.java
│       └── BufferCircular.java
├── client-registro/
│   └── src/main/java/com/aeropuerto/registro/
│       ├── RegistroApp.java
│       ├── ConexionServidor.java
│       ├── ClienteSocket.java
│       ├── TicketStage.java
│       └── Launcher.java
├── client-general/
│   └── src/main/java/com/aeropuerto/general/
│       ├── GeneralApp.java
│       ├── ConexionServidor.java
│       └── Launcher.java
├── client-prioritaria/
│   └── src/main/java/com/aeropuerto/prioritaria/
│       ├── PrioritariaApp.java
│       ├── ConexionServidor.java
│       └── Launcher.java
├── client-especial/
│   └── src/main/java/com/aeropuerto/especial/
│       ├── EspecialApp.java
│       ├── ConexionServidor.java
│       └── Launcher.java
├── client-logs/
│   └── src/main/java/com/aeropuerto/logs/
│       ├── LogsApp.java
│       ├── ConexionMonitor.java
│       └── Launcher.java
├── client-monitor/
│   └── src/main/java/com/aeropuerto/monitor/
│       ├── MonitorApp.java
│       ├── ConexionMonitor.java
│       ├── NodoModulo.java
│       └── Launcher.java
├── package-beta.ps1                 ← Script de empaquetado jpackage
└── dist-beta/                       ← Generado por el script
    ├── Servidor/   Servidor.exe  (~152 MB)
    ├── Registro/   Registro.exe  (~161 MB)
    ├── General/    General.exe   (~161 MB)
    ├── Prioritaria/              (~161 MB)
    ├── Especial/                 (~161 MB)
    ├── Logs/       Logs.exe      (~161 MB)
    └── Monitor/    Monitor.exe   (~161 MB)
```

---

## 4. Módulo Common — Biblioteca Compartida

Este módulo no tiene clase `main`. Es una biblioteca JAR que empaquetan todos los demás módulos dentro de su fat JAR. Contiene las clases que el servidor y los clientes necesitan conocer en común.

### 4.1 TipoMensaje.java — Enum del protocolo

Define todos los tipos de mensaje que pueden circular por el protocolo TCP.

```
REGISTRO          C→S   Registrar pasajero en cola: dpi|nombre|tipoAtencion
CONFIRMACION      S→C   Registro o fin exitoso: dpi|numeroCola
ERROR             S→C   Algo falló: mensajeError
LLAMAR_SIGUIENTE  C→S   Operador pide siguiente: tipoAtencion
PASAJERO_LLAMADO  S→C   Datos del siguiente: dpi|nombre|numeroCola
COLA_VACIA        S→C   No hay nadie en esa cola: tipoAtencion
ESTADO_COLA       S→C   Actualización periódica de estado
FIN_ATENCION      C→S   Operador termina: dpi|vuelo|observaciones|duracionSeg
PING              C→S   Keepalive (sin campos)
PONG              S→C   Respuesta a PING (sin campos)
IDENTIFICAR       C→S   Primer mensaje: tipoCliente|nombrePc
IDENTIFICAR_OK    S→C   Confirmación de identificación (sin campos)
LOG_ENTRY         S→Monitor   id|timestamp|nivel|modulo|mensaje
STATUS_UPDATE     S→Monitor   accion|ip|puerto|tipo|nombrePc|timestamp
```

### 4.2 Mensaje.java — Serialización del protocolo

Clase central de comunicación. Encapsula tipo + campos variables.

**Constructores:**
```java
new Mensaje(TipoMensaje tipo, String... campos)
```

**Serialización:**
```java
m.serializar()  // "REGISTRO|1234567890101|Carlos García|GENERAL"
Mensaje.deserializar(linea)  // desde texto recibido por socket
```

**Métodos factory (facilitan creación sin recordar orden de campos):**
```java
Mensaje.registro(dpi, nombre, tipo)
Mensaje.confirmacion(dpi, numeroCola)
Mensaje.error(mensajeError)
Mensaje.llamarSiguiente(tipo)
Mensaje.pasajeroLlamado(dpi, nombre, numeroCola)
Mensaje.colaVacia(tipo)
Mensaje.finAtencion(dpi)                                  // versión simple
Mensaje.finAtencion(dpi, vuelo, observaciones, durSeg)    // versión completa
Mensaje.identificar(tipoCliente, nombrePc)
Mensaje.identificarOk()
Mensaje.logEntry(entry)
Mensaje.statusUpdate(accion, info)
Mensaje.ping()
Mensaje.pong()
```

**Notas de seguridad del protocolo:**
- `finAtencion()` limpia automáticamente el carácter `|` de vuelo y observaciones para no romper el protocolo pipe-delimited
- El campo `mensaje` de `LOG_ENTRY` siempre va al final para que pueda contener `|` sin ambigüedades

### 4.3 Pasajero.java — Entidad activa en cola

Representa a un pasajero **activo** en el sistema (en espera o en atención). Es inmutable en DPI y nombre.

**Campos:**
```
String dpi         — Identificador único, nunca cambia
String nombre      — Nombre completo
TipoAtencion tipo  — Cola a la que pertenece
EstadoPasajero estado  — EN_ESPERA → EN_ATENCION → ATENDIDO
int numeroCola     — Número de turno asignado (1, 2, 3...)
```

**Igualdad:** Dos pasajeros son iguales si tienen el mismo DPI (`equals()` y `hashCode()` implementados sobre `dpi`).

### 4.4 RegistroVisita.java — Historial persistente

Registro completo de una visita al sistema. Contiene el ciclo de vida completo del pasajero desde que entra hasta que es atendido.

**Ciclo de estados:**
```
Constructor → estado: EN_ESPERA,    fechaLlamada="",   fechaFin=""
marcarLlamada() → EN_ATENCION,     fechaLlamada=ahora()
marcarFin() → ATENDIDO,            fechaFin=ahora(), vuelo, obs, durSeg
```

**Formato de archivo `aeropuerto_datos.db`:**
```
dpi;;nombre;;tipo;;numCola;;fechaRegistro;;fechaLlamada;;fechaFin;;durSeg;;vuelo;;obs;;estado
```

El separador `;;` fue elegido deliberadamente para diferenciarse del `|` del protocolo (que puede aparecer en nombres de vuelos como "AM 123 — Ciudad de Mexico").

**11 campos por registro:**

| Campo | Ejemplo |
|-------|---------|
| dpi | `1234567890101` |
| nombre | `Carlos García López` |
| tipo | `GENERAL` |
| numCola | `5` |
| fechaRegistro | `2026-05-12 10:23:45` |
| fechaLlamada | `2026-05-12 10:35:12` |
| fechaFin | `2026-05-12 10:38:44` |
| durSeg | `212` |
| vuelo | `AM 123 — Ciudad de Mexico` |
| obs | `Asiento 14B, equipaje adicional` |
| estado | `ATENDIDO` |

### 4.5 TipoAtencion.java — Enum de colas

```java
public enum TipoAtencion { GENERAL, PRIORITARIA, ESPECIAL }
```

### 4.6 EstadoPasajero.java — Enum de estados

```java
public enum EstadoPasajero { EN_ESPERA, EN_ATENCION, ATENDIDO }
```

### 4.7 LogEntry.java — Registro de evento del servidor

Inmutable. Asigna ID autoincremental global y timestamp en el momento de creación.

**Niveles de log:**
```
INFO     — Mensajes informativos generales
ACTION   — Eventos de acción (registrado, llamado, atendido, conectado)
WARN     — Advertencias (reconexión, sin conexión)
ERROR    — Errores del sistema
```

**Inferencia automática del nivel** (en `LogInterceptor`):
- Contiene "error", "exception", "fallo" → ERROR
- Contiene "advertencia", "warning", "sin conexi" → WARN
- Contiene "registrado", "llamado", "atendido", "conectado", "desconectado", "monitor" → ACTION
- Resto → INFO

### 4.8 ClienteInfo.java — Metadatos de conexión

Creado cuando el cliente envía `IDENTIFICAR`. Captura: IP, puerto, tipo de cliente, nombre de PC, hora de conexión.

**Tipos de cliente:**
```
REGISTRO, GENERAL, PRIORITARIA, ESPECIAL → clientes operativos
LOGS, MONITOR → monitores (modo push)
DESCONOCIDO → cliente que no envió IDENTIFICAR o tipo inválido
```

`esMonitor()` retorna `true` para LOGS y MONITOR — el servidor los desvía al modo push.

### 4.9 Cola.java — Estructura de datos propia

Cola genérica implementada con **lista enlazada simple propia** (sin ArrayList ni LinkedList de Java).

**Complejidades:**
```
encolar(T)   → O(1) — agrega al fin
desencolar() → O(1) — retira del frente, retorna null si vacía
peek()       → O(1) — consulta frente sin retirar
isEmpty()    → O(1)
size()       → O(1)
```

**Implementación:** Nodos `Nodo<T>` con `dato` y `siguiente`. Mantiene referencias a `frente` y `fin` para O(1) en ambos extremos.

### 4.10 TablaHash.java — Estructura de datos propia

Tabla hash genérica con **encadenamiento** para resolución de colisiones.

**Capacidad por defecto:** 16 buckets  
**Hash:** `Math.abs(clave.hashCode() % tabla.length)`

**Operaciones:**
```
insertar(K, V) → O(1) promedio — actualiza si ya existe la clave
buscar(K)      → O(1) promedio — retorna null si no existe
eliminar(K)    → O(1) promedio — retorna false si no existía
contiene(K)    → O(1) promedio
obtenerValores() → O(n) — retorna todos los valores como Object[]
```

**Usos en el sistema:**
- `TablaHash<String, Pasajero>` — índice DPI → pasajero activo en GestorColas
- `TablaHash<String, ArrayList<RegistroVisita>>` — historial por DPI en BaseDatos
- `TablaHash<String, ClienteInfo>` — clientes activos en RegistroConexiones
- `TablaHash<String, PrintWriter>` — monitores activos en RegistroConexiones

### 4.11 CatalogoRENAP.java — Simulación del registro civil

Simula la consulta al Registro Nacional de Personas de Guatemala. Contiene 15 registros codificados en un `HashMap` estático.

**Estructura de datos:**
```java
class DatosPersona {
    String nombre;
    String fechaNacimiento;  // formato "yyyy-MM-dd"
    String genero;           // "M" o "F"
}
```

**Uso:**
```java
DatosPersona d = CatalogoRENAP.consultar("1234567890101");
// d.nombre = "Carlos García López"
// d.fechaNacimiento = "1985-03-15"
// d.genero = "M"
```

**Integración con RegistroApp:** Cuando se ingresa un DPI válido, se consulta RENAP. Si el pasajero tiene 60 años o más, el sistema automáticamente le asigna la Cola Prioritaria y muestra un badge "Auto" junto al tipo seleccionado.

---

## 5. Módulo Server — Servidor Central

### 5.1 ServerMain.java — Punto de entrada

Clase principal del servidor. Responsabilidades:
1. Instalar el `LogInterceptor` sobre `System.out` (captura todos los `println`)
2. Pre-inicializar `GestorColas` (que inicializa `BaseDatos`)
3. Abrir `ServerSocket` en el puerto configurado (default: 5000)
4. Loop infinito: `accept()` → nuevo `ClientHandler` en hilo daemon

**Configuración:**
```java
private static final int PUERTO_DEFAULT = 5000;
```

El puerto también puede pasarse como argumento de línea de comandos:
```
Servidor.exe 6000
```

**Salida al iniciar:**
```
╔══════════════════════════════════════════╗
║  AEROPUERTO GUATEMALA — SERVIDOR CENTRAL ║
╚══════════════════════════════════════════╝
[SERVER] Iniciando en puerto 5000...
[SERVER] GestorColas inicializado OK
[SERVER] Escuchando en puerto 5000
[SERVER] IP local: 192.168.1.74
[SERVER] Esperando clientes... (Ctrl+C para detener)
```

**Nota:** `SO_REUSEADDR = true` permite reiniciar el servidor inmediatamente sin esperar que el OS libere el puerto (útil en demos y pruebas).

### 5.2 ClientHandler.java — Manejo de conexiones individuales

Se instancia y ejecuta en un hilo daemon por cada cliente que se conecta. El servidor vuelve a `accept()` inmediatamente.

**Ciclo de vida del hilo:**
```
1. Leer primera línea
2. intentarIdentificar(primeraLinea)
   ├── Si IDENTIFICAR y tipo es MONITOR/LOGS → manejarMonitor()
   ├── Si IDENTIFICAR y tipo operativo → loopNormal(null)
   └── Si no es IDENTIFICAR → loopNormal(primeraLinea)  [compatibilidad]
3. finally: eliminarCliente() + cerrarConexion()
```

**Procesamiento de mensajes (`procesar()`):**

Usa un `switch` de expresión (Java 14+):
```
REGISTRO        → GestorColas.registrarPasajero()
LLAMAR_SIGUIENTE → GestorColas.llamarSiguiente()
FIN_ATENCION    → GestorColas.finalizarAtencion() con campos opcionales
PING            → Mensaje.pong()
IDENTIFICAR     → ya procesado, retorna null
default         → ERROR con mensaje descriptivo
```

**`FIN_ATENCION` con campos opcionales:** El handler acepta mensajes con 1, 2, 3 o 4 campos (el mínimo es solo `dpi`). Los campos opcionales tienen valores por defecto si se omiten, garantizando compatibilidad hacia atrás.

**Modo monitor (`manejarMonitor()`):**
1. Registrar en `RegistroConexiones` con su `PrintWriter`
2. `LogManager.empujarHistorialA()` — enviar los últimos 2000 logs inmediatamente
3. Loop de lectura: solo responde a `PING` con `PONG`
4. `finally`: eliminar de `RegistroConexiones`

### 5.3 GestorColas.java — Núcleo de la lógica de negocios

Singleton thread-safe. Mantiene el estado activo del sistema en memoria.

**Estado en memoria:**
```java
Cola<Pasajero>              colaGeneral
Cola<Pasajero>              colaPrioritaria
Cola<Pasajero>              colaEspecial
TablaHash<String, Pasajero> indicePorDpi      // DPI → pasajero activo
int                         contadorGeneral    // turno actual GENERAL
int                         contadorPrioritaria
int                         contadorEspecial
```

**Método `registrarPasajero(dpi, nombre, tipo)`:**
1. Verificar que `indicePorDpi` no contenga ya ese DPI → si sí, retornar ERROR con estado actual
2. Asignar número de turno (`++contadorXxx`)
3. Crear `Pasajero` con `estado = EN_ESPERA`
4. `colaXxx.encolar(pasajero)` + `indicePorDpi.insertar(dpi, pasajero)`
5. Crear `RegistroVisita` y persistir en `BaseDatos`
6. Retornar `CONFIRMACION|dpi|numeroCola`

**Método `llamarSiguiente(tipo)`:**
1. Verificar que `colaXxx` no esté vacía → si sí, retornar `COLA_VACIA|tipo`
2. `cola.desencolar()` → el pasajero sale de la cola
3. `pasajero.setEstado(EN_ATENCION)` — sigue en `indicePorDpi`
4. `BaseDatos.marcarLlamada(dpi)`
5. Retornar `PASAJERO_LLAMADO|dpi|nombre|numeroCola`

**Método `finalizarAtencion(dpi, vuelo, obs, durSeg)`:**
1. Buscar en `indicePorDpi` → si no existe, retornar ERROR
2. Verificar que el estado sea `EN_ATENCION` → si no, retornar ERROR
3. `pasajero.setEstado(ATENDIDO)` + `indicePorDpi.eliminar(dpi)` (ya puede registrarse de nuevo)
4. `BaseDatos.marcarFin(dpi, vuelo, obs, durSeg)`
5. Retornar `CONFIRMACION|dpi|numeroCola`

**Thread-safety:** Todos los métodos son `synchronized`. Múltiples `ClientHandler` (uno por ventanilla) acceden simultáneamente.

**Importante:** El estado en memoria se pierde si el servidor se reinicia. Los datos históricos persisten en `aeropuerto_datos.db`.

### 5.4 BaseDatos.java — Persistencia en disco

Singleton thread-safe. Archivo de texto plano en el directorio de trabajo del servidor.

**Archivo:** `aeropuerto_datos.db`  
**Ubicación:** Directorio desde donde se ejecuta `Servidor.exe` (misma carpeta)

**Estructura en memoria:**
```java
TablaHash<String, ArrayList<RegistroVisita>> historialPorDpi   // búsqueda O(1)
ArrayList<RegistroVisita>                     todosLosRegistros // orden de inserción
```

**Al iniciar el servidor:** Carga todo el archivo desde disco a memoria. Las líneas malformadas se ignoran (se registra cuántas hubo).

**Al modificar:** Reescribe el archivo completo. Justificación: los registros se actualizan (marcarLlamada, marcarFin), no son solo inserciones. Reescribir es la forma más simple de garantizar consistencia. Para el volumen esperado (cientos por día), es perfectamente aceptable.

**Cabecera del archivo:**
```
# AeroQueue — Base de datos historica
# Formato: dpi;;nombre;;tipo;;numCola;;fechaRegistro;;fechaLlamada;;fechaFin;;duracionSeg;;vuelo;;obs;;estado
# NO EDITAR MANUALMENTE — es mantenido automaticamente por el servidor
```

**Consultas disponibles:**
```java
buscarPorDpi(dpi)     → List<RegistroVisita>  // historial completo de un DPI
buscarUltimaVisita(dpi) → RegistroVisita      // la más reciente (activa o última)
totalRegistros()      → int                    // cantidad total histórica
```

### 5.5 LogManager.java — Buffer de logs

Singleton. Almacena los últimos logs del servidor en un `BufferCircular` y los empuja a los monitores conectados.

**Buffer:** `BufferCircular<LogEntry>` con capacidad de **2000 entradas**. Cuando se llena, los logs más antiguos se sobreescriben automáticamente (ring buffer).

**Flujo de un log:**
```
System.out.println("[GESTOR] Registrado: ...")
    ↓  (interceptado por LogInterceptor)
LogManager.registrar(entry)
    ↓
BufferCircular.agregar(entry)
    +
RegistroConexiones.empujar(logEntry serializado)
    ↓
Todos los monitores conectados reciben el LOG_ENTRY por socket
```

**Al conectarse un monitor:** `empujarHistorialA(writer)` envía todos los logs del buffer en orden cronológico. El monitor recibe el historial completo inmediatamente.

### 5.6 LogInterceptor.java — Captura de System.out

Extiende `PrintStream`. Se instala en `ServerMain`:
```java
System.setOut(new LogInterceptor(System.out, logManager));
```

Intercepta `println(String)` y `println(Object)`. Para cada línea:
1. Escribe en la consola original (`original.println(x)`)
2. Llama a `logManager.registrar(parsear(x))`

`parsear()` infiere:
- **Módulo** por prefijo: `[SERVER]`, `[HANDLER]`, `[GESTOR]`, `[CONEXION]`, `[DB]`, `[ERROR]`, etc.
- **Nivel** por palabras clave en el texto

`ThreadLocal<Boolean> enProceso` previene recursión infinita al hacer logging del proceso de logging.

### 5.7 RegistroConexiones.java — Registro de conexiones activas

Singleton thread-safe. Dos estructuras independientes:

```java
TablaHash<String, ClienteInfo> clientes    // operativos: ip:puerto → ClienteInfo
TablaHash<String, PrintWriter> monitores   // monitores:  ip:puerto → PrintWriter
```

**Al conectar un cliente operativo:**
- `registrarCliente(info)` → insertar en `clientes` + empujar `STATUS_UPDATE CONECTADO` a todos los monitores

**Al desconectar:**
- `eliminarCliente(id)` → eliminar de `clientes` + empujar `STATUS_UPDATE DESCONECTADO`

**Al conectar un monitor:**
- `registrarMonitor(id, info, writer)` → insertar en `monitores` + volcar estado actual (todos los clientes conectados como `STATUS_UPDATE CONECTADO`)
- Esto permite que el monitor arranque con el estado real del sistema, no solo eventos futuros

**`empujar(mensaje)`:** Envía a todos los `PrintWriter` en `monitores`. Si alguno falla, el error se ignora silenciosamente (se limpiará cuando `ClientHandler` detecte la desconexión).

### 5.8 BufferCircular.java — Ring buffer de logs

Buffer circular genérico de capacidad fija. Cuando se llena, el elemento más antiguo se sobreescribe.

**Todas las operaciones son O(1).**

```java
agregar(T)     → O(1) — inserta en posición calculada por módulo aritmético
obtenerTodos() → O(n) — retorna arreglo en orden cronológico (más antiguo primero)
tamaño()       → O(1)
```

---

## 6. Módulo client-registro — Kiosco de Registro

### Propósito

Permite a un operador de kiosco registrar nuevos pasajeros en el sistema. Consulta el catálogo RENAP, autocompleta los datos, asigna tipo de cola automáticamente y genera un ticket imprimible.

### Configuración de conexión

```java
private static final String HOST   = "192.168.1.74";
private static final int    PUERTO = 5000;
```

### Clases del módulo

**`RegistroApp.java`** — Aplicación principal JavaFX. Extiende `Application`.

**`ConexionServidor.java`** — Manejo de socket con auto-reconexión (ver sección 15).

**`TicketStage.java`** — Ventana modal del ticket generado. Glassmorphism con cabecera de color según tipo de cola. Botones: "Imprimir" (usa `javafx.print.PrinterJob`) y "Listo" (cierra y llama al callback `onListo` para limpiar el formulario).

**`ClienteSocket.java`** — Utilidad de socket de bajo nivel (envuelto por `ConexionServidor`).

**`Launcher.java`** — Clase main para compatibilidad con fat JAR (JavaFX requiere launcher separado en módulos no-modulares).

### Flujo de uso

```
1. Operador ingresa DPI (13 dígitos)
2. Sistema consulta CatalogoRENAP en tiempo real
   ├── Encontrado: autocompleta nombre, fecha, género
   │              edad ≥ 60 → selecciona Prioritaria automáticamente
   │              muestra tiempo de búsqueda en badge verde
   └── No encontrado: muestra tiempo y solicita nombre manual
3. Operador puede cambiar el tipo de cola manualmente
4. Operador puede agregar Necesidades especiales y Observaciones
5. Tick previa del ticket se actualiza en tiempo real en la columna derecha
6. "Generar ticket →" → envía REGISTRO al servidor
   ├── CONFIRMACION: muestra TicketStage con número de turno real
   └── ERROR: muestra banner rojo con la causa exacta
7. TicketStage: operador puede imprimir y/o hacer clic en "Listo"
8. Formulario se limpia para el siguiente pasajero
```

### Validaciones

- DPI debe ser exactamente 13 dígitos numéricos (regex `\d{13}`)
- Nombre no puede estar vacío
- Si la validación falla, el campo afectado "agita" lateralmente (animación de error)

### Interfaz gráfica

- **Tamaño:** 1080 × 660 px, redimensionable
- **Dos columnas:** izquierda (tipo de cola, necesidades, observaciones) | derecha (vista previa del ticket)
- **Zona DPI:** fija en la parte superior, siempre visible
- **Banner RENAP:** aparece con animación fade al consultar un DPI
- **Strip del pasajero:** muestra avatar con iniciales y datos resumidos

### Identificación con el servidor

```java
conexion.conectarEIdentificar("REGISTRO", nombrePc);
```

Donde `nombrePc` es el hostname de la PC obtenido con `InetAddress.getLocalHost().getHostName()`.

---

## 7. Módulo client-general — Ventanilla General

### Propósito

Módulo para el operador de la ventanilla de Cola General. Permite llamar al siguiente pasajero, ingresar vuelo y observaciones, y finalizar la atención con tiempo medido automáticamente.

### Características principales

**Tiempo de atención:** Se mide desde que el operador presiona "Llamar siguiente" (`tiempoInicioAtencion = System.currentTimeMillis()`) hasta que presiona "Finalizar atención". El tiempo en segundos se envía al servidor en `FIN_ATENCION`.

**Bloqueo de llamada:** El botón "Llamar siguiente" se deshabilita mientras hay un pasajero en atención activa. Si el operador intenta llamar con uno activo, el sistema muestra un aviso "Finaliza la atención actual primero".

**Auto-reconexión:** Si el servidor se cae, el badge de conexión cambia a rojo y el sistema intenta reconectar cada 5 segundos automáticamente.

**Vuelos predefinidos:** El operador selecciona el vuelo de un `ComboBox` con 10 vuelos predefinidos:
```
AM 123 — Ciudad de Mexico
AA 456 — Miami
UA 789 — Houston
CM 101 — Ciudad de Panama
IB 202 — Madrid
LA 303 — Bogota
AV 404 — Medellin
NK 505 — Fort Lauderdale
VB 606 — Cancun
TB 707 — San Jose, CR
```

### Identificación

```java
conexion.conectarEIdentificar("GENERAL", nombrePc);
```

### Flujo de operación

```
1. Pantalla inicial: pasajero vacío, botón "Llamar siguiente" activo
2. Operador presiona "Llamar siguiente"
   → Envía LLAMAR_SIGUIENTE|GENERAL
   ├── PASAJERO_LLAMADO: muestra DPI, nombre, turno, inicia cronómetro
   │                     deshabilita "Llamar siguiente"
   └── COLA_VACIA: muestra aviso, botón permanece activo
3. Operador atiende al pasajero
4. Operador selecciona vuelo + escribe observaciones
5. "Finalizar atención"
   → Calcula duración = (now - tiempoInicioAtencion) / 1000
   → Envía FIN_ATENCION|dpi|vuelo|observaciones|duracionSeg
   ├── CONFIRMACION: limpia pantalla, reactiva "Llamar siguiente"
   └── ERROR: muestra causa, botón "Finalizar" permanece activo
```

---

## 8. Módulo client-prioritaria — Ventanilla Prioritaria

Funcionalmente idéntico a `client-general` pero conectado a la cola `PRIORITARIA`. La identidad visual usa naranja Apple como color de acento en lugar de verde. Se identifica con `"PRIORITARIA"` al servidor.

La interfaz muestra el mismo conjunto de vuelos predefinidos y tiene los mismos controles (bloqueo de atención activa, tiempo de atención, auto-reconexión).

---

## 9. Módulo client-especial — Ventanilla Especial/VIP

### Propósito

Módulo para la ventanilla VIP/Especial. Tiene funcionalidades adicionales respecto a General y Prioritaria porque las atenciones especiales requieren más datos.

### Características adicionales

**Selector de clase de asiento:**
```
Primera Clase   / Business Class / Premium Economy / Economy
```

**Checkboxes de condición VIP:**
```
☐ Socio VIP
☐ Ejecutivo
☐ Diplomático
☐ Acompañante autorizado
☐ Embarque preferencial
```

**Observaciones generadas automáticamente:** Al finalizar, el campo `observaciones` incluye la clase de asiento y los checkboxes marcados, formando una cadena como:
```
Business Class | Socio VIP | Diplomático
```

**Identificación:** `"ESPECIAL"`

El resto de la funcionalidad (bloqueo, tiempo de atención, auto-reconexión) es idéntico a General y Prioritaria.

---

## 10. Módulo client-logs — Visor de Logs y Base de Datos

### Propósito

Módulo de supervisión con dos funciones: ver los eventos del servidor en tiempo real y explorar la base de datos histórica de pasajeros.

### Modo de conexión

Se identifica como `"LOGS"`. El servidor lo detecta como monitor y lo desvía al modo push:
1. Recibe inmediatamente el historial del buffer (hasta 2000 logs)
2. Recibe cada nuevo evento en tiempo real
3. No envía mensajes de operación (solo PING de keepalive)

### Tab 1 — Logs en Tiempo Real

**TableView con columnas:**
```
ID | Hora | Nivel | Módulo | Mensaje
```

**Colores por nivel:**
```
INFO   → Azul claro  #64D2FF
ACTION → Verde       #32D74B
WARN   → Naranja     #FF9F0A
ERROR  → Rojo        #FF453A
```

**Filtros disponibles:**
- Campo de búsqueda libre (por texto del mensaje)
- `ComboBox` de módulo (SERVER, HANDLER, GESTOR, DB, CONEXION, etc.)
- `ComboBox` de nivel (INFO, ACTION, WARN, ERROR)
- Checkbox "Auto-scroll" (desplaza la tabla al último log automáticamente)

**Contador:** Muestra cuántos logs están en pantalla y el total recibido.

**Auto-reconexión:** Si el servidor se cae, el badge cambia a rojo y el sistema intenta reconectar cada 5 segundos. Al reconectar, el servidor vuelve a enviar el historial completo.

### Tab 2 — Base de Datos

Permite cargar y explorar el archivo `aeropuerto_datos.db` directamente.

**Barra superior:**
- `TextField` con la ruta al archivo (editable, apunta al archivo del servidor)
- Botón "Cargar / Actualizar" — carga en hilo de fondo para no bloquear la UI
- Contador de registros cargados

**Filtros:**
- Búsqueda por DPI (coincidencia parcial, case-insensitive)
- Filtro por tipo de cola (GENERAL, PRIORITARIA, ESPECIAL)
- Filtro por estado (EN_ESPERA, EN_ATENCION, ATENDIDO)
- Botón "Resetear filtros"

**TableView con 11 columnas:**
```
DPI | Nombre | Cola | Turno | Registro | Llamada | Fin | Duración | Vuelo | Estado | Observaciones
```

**Colores de fila según estado:**
```
ATENDIDO   → verde tenue
EN_ATENCION → ámbar tenue
EN_ESPERA  → azul tenue
```

**Carga:** `Files.readAllLines()` en hilo background → `RegistroVisita.deserializar()` por línea → `Platform.runLater()` para actualizar la tabla.

---

## 11. Módulo client-monitor — Monitor de Módulos

### Propósito

Vista "task manager" del sistema en tiempo real. Muestra todas las PCs conectadas organizadas por tipo de módulo, con un panel adicional para controlar el servidor.

### Modo de conexión

Se identifica como `"MONITOR"`. Recibe `STATUS_UPDATE` en tiempo real (conexiones y desconexiones de clientes), más `LOG_ENTRY` con todos los eventos del servidor.

### Árbol de conexiones

`TreeView<NodoModulo>` con estructura:
```
📂 Registro           (x instancias)
    └── 📄 PC-VENTAS-01 — 192.168.1.82:51234 — 10:23:45
    └── 📄 PC-VENTAS-02 — 192.168.1.83:51890 — 10:24:12
📂 Cola General       (x instancias)
    └── 📄 ...
📂 Cola Prioritaria
📂 Cola Especial
📂 Monitor — Logs
📂 Monitor — Módulos
```

Los grupos vacíos se muestran sin instancias (no se ocultan). Al hacer click en una instancia, el panel derecho muestra los detalles: IP, puerto, tipo, PC, hora de conexión.

**Colores por tipo:**
```
REGISTRO    → Naranja  #FF9F0A
GENERAL     → Verde    #32D74B
PRIORITARIA → Naranja  #FF9F0A
ESPECIAL    → Azul     #64AAFF
LOGS/MONITOR → Púrpura #BF5AF2
```

**Contador de logs:** Muestra cuántos `LOG_ENTRY` se han recibido en la sesión.

**Auto-reconexión:** Al reconectar, `limpiarArbol()` reinicia el árbol y el servidor empuja el estado actual de todas las conexiones.

### Panel de control del servidor

Sección en la parte inferior de la ventana con fondo oscuro y fuente de consola verde (estilo terminal).

**Campos:**
- `TextField` con la ruta al JAR o EXE del servidor (editable)
- Botón "▶ Iniciar Servidor"
- Botón "■ Detener"
- `TextArea` de consola — muestra la salida del proceso servidor

**Iniciar servidor:**
```java
ProcessBuilder pb = new ProcessBuilder("java", "-jar", rutaJar.getText());
pb.redirectErrorStream(true);  // stderr + stdout en el mismo stream
Process proceso = pb.start();
// Hilo daemon lee línea por línea → Platform.runLater(() -> appendConsola(linea))
```

**Detener servidor:**
```java
procesosServidor.destroy();
```

La consola muestra en verde (Consolas 12px) la salida del servidor. Los mensajes de estado del proceso se muestran en ámbar.

---

## 12. Protocolo de Comunicación TCP

### Especificación completa

**Formato de mensaje:**
```
TIPO|campo1|campo2|...|campoN\n
```
- Separador de campos: `|` (barra vertical / pipe)
- Terminador: `\n` (newline — lo agrega `PrintWriter.println()`)
- Codificación: UTF-8
- Lectura: `BufferedReader.readLine()` → retorna la línea sin el `\n`

### Tabla completa de mensajes

| Tipo | Dirección | Campos | Ejemplo |
|------|-----------|--------|---------|
| `REGISTRO` | C→S | dpi\|nombre\|tipo | `REGISTRO\|1234567890101\|Carlos García\|GENERAL` |
| `CONFIRMACION` | S→C | dpi\|numeroCola | `CONFIRMACION\|1234567890101\|5` |
| `ERROR` | S→C | mensajeError | `ERROR\|DPI ya registrado en el sistema` |
| `LLAMAR_SIGUIENTE` | C→S | tipo | `LLAMAR_SIGUIENTE\|GENERAL` |
| `PASAJERO_LLAMADO` | S→C | dpi\|nombre\|numeroCola | `PASAJERO_LLAMADO\|1234567890101\|Carlos García\|5` |
| `COLA_VACIA` | S→C | tipo | `COLA_VACIA\|GENERAL` |
| `FIN_ATENCION` | C→S | dpi\|vuelo\|obs\|durSeg | `FIN_ATENCION\|1234567890101\|AM 123 — Ciudad de Mexico\|Asiento 14B\|212` |
| `PING` | C→S | (ninguno) | `PING` |
| `PONG` | S→C | (ninguno) | `PONG` |
| `IDENTIFICAR` | C→S | tipoCliente\|nombrePc | `IDENTIFICAR\|GENERAL\|PC-VENTAS-01` |
| `IDENTIFICAR_OK` | S→C | (ninguno) | `IDENTIFICAR_OK` |
| `LOG_ENTRY` | S→Monitor | id\|ts\|nivel\|modulo\|msg | `LOG_ENTRY\|42\|10:23:45.123\|ACTION\|GESTOR\|Registrado: ...` |
| `STATUS_UPDATE` | S→Monitor | accion\|ip\|puerto\|tipo\|pc\|ts | `STATUS_UPDATE\|CONECTADO\|192.168.1.82\|51234\|GENERAL\|PC-VENTAS-01\|10:23:45` |

### Secuencia de una sesión típica (cliente operativo)

```
Cliente                           Servidor
   │                                 │
   │── IDENTIFICAR|GENERAL|PC-01 ──►│
   │◄── IDENTIFICAR_OK ──────────────│  (cliente registrado en RegistroConexiones)
   │                                 │  (monitores reciben STATUS_UPDATE CONECTADO)
   │── LLAMAR_SIGUIENTE|GENERAL ───►│
   │◄── PASAJERO_LLAMADO|DPI|Nombre|5│
   │                                 │  (BaseDatos.marcarLlamada())
   │       [operador atiende]        │
   │── FIN_ATENCION|DPI|Vuelo|Obs|212►│
   │◄── CONFIRMACION|DPI|5 ──────────│  (BaseDatos.marcarFin())
   │                                 │
   │── PING ──────────────────────────│
   │◄── PONG ────────────────────────│
   │                                 │
   │   [cierre de ventana]           │
   │── (cierre TCP) ────────────────►│  (monitores reciben STATUS_UPDATE DESCONECTADO)
```

### Secuencia de conexión de un monitor

```
Monitor                           Servidor
   │                                 │
   │── IDENTIFICAR|LOGS|PC-SUP-01 ──►│
   │◄── IDENTIFICAR_OK ──────────────│
   │◄── LOG_ENTRY|1|...|INFO|... ────│  \
   │◄── LOG_ENTRY|2|...|ACTION|... ──│   | historial completo del buffer
   │◄── LOG_ENTRY|3|... ─────────────│   | (hasta 2000 entradas)
   │◄── ... ─────────────────────────│  /
   │◄── STATUS_UPDATE|CONECTADO|... ─│  estado actual de clientes
   │                                 │
   │   [en tiempo real, push]        │
   │◄── LOG_ENTRY|43|... ────────────│  eventos nuevos del servidor
   │◄── STATUS_UPDATE|DESCONECTADO...│  cuando un cliente se desconecta
```

---

## 13. Base de Datos Persistente

### Archivo `aeropuerto_datos.db`

**Ubicación:** Directorio de trabajo del servidor (donde se ejecuta `Servidor.exe`)

**Formato:** Una línea por visita, campos separados por `;;`

**Ejemplo de contenido:**
```
# AeroQueue — Base de datos historica
# Formato: dpi;;nombre;;tipo;;numCola;;fechaRegistro;;fechaLlamada;;fechaFin;;duracionSeg;;vuelo;;obs;;estado
# NO EDITAR MANUALMENTE — es mantenido automaticamente por el servidor
1234567890101;;Carlos García López;;GENERAL;;1;;2026-05-12 10:23:45;;2026-05-12 10:35:12;;2026-05-12 10:38:44;;212;;AM 123 — Ciudad de Mexico;;Asiento 14B;;ATENDIDO
5678901234501;;Pedro Antonio Santos Cruz;;PRIORITARIA;;1;;2026-05-12 10:25:00;;2026-05-12 10:31:00;;2026-05-12 10:34:30;;210;;;;Adulto mayor;;ATENDIDO
9012345678901;;Miguel Ángel Torres Lima;;ESPECIAL;;1;;2026-05-12 10:26:15;;;;;;;EN_ESPERA
```

### Ciclo de actualizaciones del archivo

```
Registro →  escribe línea nueva (estado: EN_ESPERA)
Llamada  →  actualiza fechaLlamada + estado: EN_ATENCION
Fin      →  actualiza fechaFin + durSeg + vuelo + obs + estado: ATENDIDO
```

En cada actualización, el archivo se reescribe completamente desde cero.

### Acceso desde LogsApp

LogsApp puede cargar el archivo si tiene acceso a la ruta donde el servidor lo genera. En una red local, si el servidor está en `\\192.168.1.74\C$\...\Servidor\` y se tienen permisos, se puede abrir desde la red. Para mayor simplicidad en demos, se copia el archivo a una carpeta compartida o se usa la ruta local si LogsApp corre en la misma PC que el servidor.

---

## 14. Sistema de Logs en Tiempo Real (Push-Based)

### Diseño del pipeline de logging

```
Cualquier System.out.println() en el servidor
          │
          ▼ (interceptado por LogInterceptor)
LogInterceptor.println(String x)
          │
          ├──► original.println(x)     [muestra en la consola del servidor]
          │
          └──► LogManager.registrar(parsear(x))
                    │
                    ├──► BufferCircular.agregar(entry)
                    │
                    └──► RegistroConexiones.empujar(
                              Mensaje.logEntry(entry).serializar()
                          )
                              │
                              └──► Para cada monitor.PrintWriter:
                                       writer.println(mensajeSerializado)
```

### Ventajas de este diseño

1. **No invasivo:** GestorColas, ClientHandler, BaseDatos no necesitan importar ni conocer el sistema de logs.
2. **Cero configuración:** Todos los `System.out.println()` existentes se capturan automáticamente.
3. **Buffer de historial:** Los monitores que se conectan tarde reciben el historial completo.
4. **Push real:** Los monitores no hacen polling; el servidor empuja los eventos.

### Módulos que aparecen en logs

| Prefijo | Módulo | Eventos típicos |
|---------|--------|-----------------|
| `[SERVER]` | SERVER | Inicio, IP, error de bind |
| `[HANDLER]` | HANDLER | Conexiones y desconexiones individuales |
| `[GESTOR]` | GESTOR | Registros, llamadas, fines de atención |
| `[CONEXION]` | CONEXION | Alta/baja de clientes y monitores |
| `[DB]` | DB | Lecturas y escrituras a disco |
| `[ERROR]` | ERROR | Errores del sistema |

---

## 15. Auto-Reconexión Automática

### Implementación en clientes operativos (ConexionServidor.java)

Todos los módulos (Registro, General, Prioritaria, Especial) tienen su propia copia de `ConexionServidor.java` con el mismo patrón de auto-reconexión.

**Campos de estado:**
```java
private volatile boolean conectado = false;
private volatile boolean intentandoReconectar = false;
private String tipoCliente = "";
private String nombrePc    = "";
private Runnable onConexionPerdida;
private Runnable onConexionRestaurada;
```

**Flujo de reconexión:**
```
enviarYRecibir() lanza IOException
        │
        ▼
conectado = false
iniciarReconexionAutomatica()
        │
        ├── Si ya hay hilo activo → ignorar
        │
        └── Crear hilo daemon "reconexion-GENERAL"
                    │
                    ├── onConexionPerdida.run()  [actualiza badge en UI]
                    │
                    └── Loop cada 5000ms:
                            │
                            ├── conectar()  [nuevo Socket]
                            ├── enviarYRecibir(identificar())  [re-identificarse]
                            │
                            ├── Si éxito:
                            │       intentandoReconectar = false
                            │       onConexionRestaurada.run()
                            │       return
                            │
                            └── Si fallo: log + sleep + reintentar
```

**Registro de callbacks (en cada cliente):**
```java
conexion.setOnConexionPerdida(() -> Platform.runLater(() -> marcarDesconectado()));
conexion.setOnConexionRestaurada(() -> Platform.runLater(() -> marcarConectado()));
```

Los callbacks usan `Platform.runLater()` porque el hilo de reconexión no es el hilo de JavaFX.

### Implementación en monitores (ConexionMonitor.java)

LogsApp y MonitorApp tienen `ConexionMonitor.java`. El patrón es similar pero el hilo de lectura (`leer()`) es el que detecta la desconexión y dispara la reconexión:

```java
// En el finally del hilo de lectura:
if (activo) {
    activo = false;
    if (onDesconexion != null) onDesconexion.run();
    iniciarReconexionAutomatica();
}
```

Al reconectar exitosamente:
- **MonitorApp:** `limpiarArbol()` + `marcarConectado()` — el árbol se reinicia y el servidor envía el estado actual
- **LogsApp:** `marcarConectado()` — los logs nuevos se siguen recibiendo

---

## 16. Estructuras de Datos Implementadas

El proyecto implementa todas sus estructuras de datos desde cero, sin usar las colecciones de Java (`ArrayList`, `LinkedList`, `HashMap`, etc.), como requisito académico.

### Cola (lista enlazada)

```
Nodos: [Nodo1: dato, →Nodo2] → [Nodo2: dato, →Nodo3] → [Nodo3: dato, null]
        ↑ frente                                          ↑ fin
```

- `encolar()` → O(1): crea nodo, lo agrega al fin
- `desencolar()` → O(1): retira el frente, avanza el puntero
- `size` se mantiene como contador actualizado

### TablaHash (tabla hash con encadenamiento)

```
índice 0: Nodo{clave="1234", valor=Pasajero} → null
índice 3: Nodo{clave="5678", valor=Pasajero} → Nodo{clave="9012", valor=Pasajero} → null
...
```

- Hash: `Math.abs(clave.hashCode() % 16)`
- Colisiones: lista enlazada por bucket
- Actualización: si la clave ya existe, sobreescribe el valor

### BufferCircular (ring buffer)

```
Capacidad 5, inicio=2, tamaño=4:
índice:  0    1    2    3    4
datos: [e4]  [e5]  [e1]  [e2]  [e3]
                    ↑ inicio
```

- `agregar()` → calcula `destino = (inicio + tamaño) % capacidad`; si lleno, avanza `inicio`
- `obtenerTodos()` → itera desde `inicio` con módulo aritmético

---

## 17. Catálogo RENAP (Simulación)

### DPIs registrados en el sistema de prueba

| DPI | Nombre | Nacimiento | Género | Edad (2026) | Cola auto-asignada |
|-----|--------|------------|--------|-------------|-------------------|
| `1234567890101` | Carlos García López | 1985-03-15 | M | 41 | General |
| `2345678901201` | María José Pérez Morales | 1990-07-22 | F | 35 | General |
| `3456789012301` | Juan Pablo Méndez Castillo | 1975-11-08 | M | 50 | General |
| `4567890123401` | Ana Lucía Ramírez Solís | 1998-02-28 | F | 28 | General |
| `5678901234501` | Pedro Antonio Santos Cruz | 1962-09-14 | M | **63** | **Prioritaria** |
| `6789012345601` | Sofía Isabel Morales Paz | 2000-05-30 | F | 25 | General |
| `7890123456701` | Roberto Carlos Fuentes | 1980-12-03 | M | 45 | General |
| `8901234567801` | Laura Beatriz González | 1955-08-19 | F | **70** | **Prioritaria** |
| `9012345678901` | Miguel Ángel Torres Lima | 1993-04-11 | M | 33 | General |
| `0123456789001` | Elena Cristina Vargas | 1970-06-25 | F | 55 | General |
| `1111111111111` | Diego Andrino González | 1995-01-20 | M | 31 | General |
| `2222222222222` | Andrea Soto Castellanos | 1988-06-15 | F | 37 | General |
| `3333333333333` | José Luis Girón Méndez | 1950-09-30 | M | **75** | **Prioritaria** |
| `4444444444444` | Carmen Alicia López | 1948-03-12 | F | **78** | **Prioritaria** |
| `5555555555555` | Francisco Ajú Caal | 2005-11-25 | M | 20 | General |

**Regla de asignación automática:** `edad >= 60 → PRIORITARIA`, resto → `GENERAL`

---

## 18. Diseño de Interfaz de Usuario

### Estilo visual: Glassmorphism Dark Navy

Todos los módulos siguen el mismo lenguaje visual:

**Fondo base:** Gradiente lineal `#1a2744 → #0f1629` (navy oscuro)

**Cards de contenido:** `rgba(255,255,255,0.10)` sobre el fondo — efecto vidrio translúcido

**Bordes:** `rgba(255,255,255,0.22)` — 1px, redondeados 14-18px

**Inputs:** `rgba(255,255,255,0.12)` — sin borde, solo fondo tenue

**Botón principal:** `#007AFF` (azul Apple) con sombra `rgba(0,122,255,0.50)`

**Textos:**
```
Título:     rgba(255,255,255,0.95)
Subtítulo:  rgba(255,255,255,0.70)
Dim:        rgba(255,255,255,0.45)
Muy tenue:  rgba(255,255,255,0.28)
```

**Acentos por módulo:**
```
General     → Verde Apple  #30D158 / #4CD964
Prioritaria → Naranja      #FF9500 / #FF9F0A
Especial    → Azul         #007AFF / #64AAFF
Logs        → Azul claro   #5AC8FA
Monitor     → Ámbar        #FF9F0A
```

**Paleta de niveles de log:**
```
INFO   → #64D2FF (azul)
ACTION → #32D74B (verde)
WARN   → #FF9F0A (ámbar)
ERROR  → #FF453A (rojo)
```

### Componentes reutilizados

- **Badge de conexión:** Círculo pulsante verde (animación `ScaleTransition` 1.2s) + texto "Conectado/Sin conexión"
- **Navbar:** Fija, muestra marca AeroQueue, fecha, reloj en vivo, badge de conexión
- **Reloj en vivo:** `Timeline` con `KeyFrame` cada 1 segundo
- **Animación de entrada:** `ParallelTransition` de fade (0→1, 320ms) + slide (−8→0 Y, 320ms, EASE_OUT)
- **Agitación de error:** `TranslateTransition` de ±5px, 45ms, 6 ciclos

---

## 19. Ciclo de Vida Completo de un Pasajero

```
┌─────────────────────────────────────────────────────────────────────────┐
│                     CICLO DE VIDA DE UN PASAJERO                       │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  1. LLEGADA AL KIOSCO                                                   │
│     Operador ingresa DPI → RENAP autocompleta datos                     │
│     Sistema asigna tipo según edad (o manual)                           │
│     "Generar ticket →"                                                  │
│          │                                                              │
│          ▼                                                              │
│  2. REGISTRO EN SERVIDOR                                                │
│     REGISTRO|dpi|nombre|GENERAL                                         │
│     ├── Servidor: crear Pasajero(EN_ESPERA), encolar, asignar turno     │
│     ├── BaseDatos: nueva línea en .db (estado: EN_ESPERA)               │
│     └── Respuesta: CONFIRMACION|dpi|5                                   │
│          │                                                              │
│          ▼                                                              │
│  3. TICKET IMPRESO                                                      │
│     TicketStage muestra: G-5, nombre, DPI, hora, tipo                  │
│     Operador puede imprimir el ticket físico                            │
│     Pasajero espera en sala → estado: EN_ESPERA en base de datos        │
│          │                                                              │
│          ▼                                                              │
│  4. LLAMADA A VENTANILLA                                                │
│     Operador en ventanilla General: "Llamar siguiente"                  │
│     LLAMAR_SIGUIENTE|GENERAL                                            │
│     ├── Servidor: desencolar → EN_ATENCION                              │
│     ├── BaseDatos: fechaLlamada=ahora(), estado: EN_ATENCION            │
│     ├── Cliente: inicia cronómetro (tiempoInicioAtencion)              │
│     └── Respuesta: PASAJERO_LLAMADO|dpi|Carlos García|5                │
│          │                                                              │
│          ▼                                                              │
│  5. ATENCIÓN                                                            │
│     Operador atiende, selecciona vuelo, escribe observaciones           │
│     Cronómetro corriendo desde que fue llamado                          │
│          │                                                              │
│          ▼                                                              │
│  6. FINALIZACIÓN                                                        │
│     "Finalizar atención"                                                │
│     FIN_ATENCION|dpi|AM 123 — Ciudad de Mexico|Asiento 14B|212          │
│     ├── Servidor: estado→ATENDIDO, eliminar de índice DPI activo        │
│     ├── BaseDatos: fechaFin, vuelo, obs, durSeg, estado: ATENDIDO       │
│     └── Respuesta: CONFIRMACION|dpi|5                                   │
│          │                                                              │
│          ▼                                                              │
│  7. REGISTRO COMPLETO EN BASE DE DATOS                                  │
│     1234567890101;;Carlos García López;;GENERAL;;5;;2026-05-12 10:23:45 │
│     ;;2026-05-12 10:35:12;;2026-05-12 10:38:44;;212;;                   │
│     AM 123 — Ciudad de Mexico;;Asiento 14B;;ATENDIDO                    │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 20. Distribución y Despliegue

### Script de empaquetado: `package-beta.ps1`

Genera ejecutables `.exe` autocontenidos usando `jpackage --type app-image`. Cada ejecutable incluye su propio JRE — la PC destino no necesita Java instalado.

**Herramientas requeridas en la PC de desarrollo:**
```
Java 24.0.1     → C:\Users\dandr\.jdks\openjdk-24.0.1
Maven 3.9.5     → C:\Program Files\JetBrains\IntelliJ IDEA 2025.1\plugins\maven\...
jpackage        → incluido en el JDK 24
```

**Uso del script:**
```powershell
# Compilar + empaquetar (primera vez o tras cambios)
.\package-beta.ps1

# Solo empaquetar (JARs ya compilados, más rápido)
.\package-beta.ps1 -SkipBuild
```

**Resultado:**
```
dist-beta/
├── Servidor/    Servidor.exe    152 MB  (tiene ventana de consola con logs)
├── Registro/    Registro.exe    161 MB
├── General/     General.exe     161 MB
├── Prioritaria/ Prioritaria.exe 161 MB
├── Especial/    Especial.exe    161 MB
├── Logs/        Logs.exe        161 MB
└── Monitor/     Monitor.exe     161 MB
                                 ────────
                         TOTAL   ~1.1 GB
```

**¿Por qué tan grandes?** Cada carpeta incluye un JRE completo (Java Runtime Environment) de ~140 MB. El JAR propio es ~9 MB. Es el costo de ser autocontenido (sin depender de Java instalado).

**Opción `--win-console` para el servidor:** El servidor incluye esta opción para que al ejecutarse aparezca una ventana de consola negra donde se ven los logs en tiempo real. Los clientes no la incluyen — se abren como aplicaciones de ventana normal.

**Opción Java:** `--enable-native-access=ALL-UNNAMED` — necesaria para Java 24 para suprimir advertencias de acceso nativo.

### Distribución

Para distribuir a los ingenieros del equipo:
1. Copiar la carpeta `dist-beta/` completa (o comprimida en ZIP)
2. El servidor va a una PC, cada cliente a otra
3. No se necesita instalar nada — solo ejecutar el `.exe`

---

## 21. Configuración de Red Local

### Configuración actual

**IP del servidor:** `192.168.1.74`  
**Puerto:** `5000`

Esta IP está hardcodeada en los 6 clientes:
```java
private static final String HOST   = "192.168.1.74";
private static final int    PUERTO = 5000;
```

### Pasos para configurar una red nueva con switch

1. **Conectar todas las PCs al switch** con cables ethernet o WiFi al mismo router/switch

2. **Identificar la IP del servidor** en la PC que ejecutará `Servidor.exe`:
   ```cmd
   ipconfig
   ```
   Buscar la dirección IPv4 del adaptador de red activo (ej: `192.168.0.15`)

3. **Verificar conectividad** desde una PC cliente:
   ```cmd
   ping 192.168.0.15
   ```

4. **Si la IP cambió**, actualizar en los 6 archivos de clientes y recompilar:
   - `client-registro/RegistroApp.java` → `HOST = "192.168.0.15"`
   - `client-general/GeneralApp.java`
   - `client-prioritaria/PrioritariaApp.java`
   - `client-especial/EspecialApp.java`
   - `client-monitor/MonitorApp.java`
   - `client-logs/LogsApp.java`

   Luego reempacar:
   ```powershell
   .\package-beta.ps1
   ```
   (sin `-SkipBuild` para que compile con la IP nueva)

5. **Abrir firewall en la PC del servidor** (una sola vez):
   ```cmd
   netsh advfirewall firewall add rule name="AeroQueue Server" dir=in action=allow protocol=TCP localport=5000
   ```

6. **Orden de inicio:**
   1. Primero iniciar `Servidor.exe` en la PC servidor
   2. Luego iniciar los clientes en las demás PCs
   3. Los clientes con auto-reconexión se conectarán automáticamente

### ¿Qué pasa si el servidor se cae?

- Los clientes muestran badge rojo "Sin conexión — reconectando..."
- Cada 5 segundos intentan reconectar automáticamente
- Al volver el servidor, se reconectan sin intervención manual
- El servidor carga el historial desde `aeropuerto_datos.db` al reiniciar
- Los pasajeros activos en memoria (EN_ESPERA o EN_ATENCION) se pierden — solo persisten los ATENDIDOS

---

## 22. Guía de Operación del Sistema

### Inicio del sistema

**1. Iniciar el servidor:**
- Ejecutar `dist-beta\Servidor\Servidor.exe`
- Aparece ventana negra de consola con los logs
- Esperar hasta ver: `[SERVER] Escuchando en puerto 5000`
- Verificar la IP mostrada: `[SERVER] IP local: 192.168.1.74`

**2. Iniciar los clientes** (en cualquier orden, en las PCs correspondientes):
- `Registro.exe` → PC del kiosco de registro
- `General.exe` → PC de la ventanilla General
- `Prioritaria.exe` → PC de la ventanilla Prioritaria
- `Especial.exe` → PC de la ventanilla Especial/VIP
- `Logs.exe` → PC de supervisión de logs
- `Monitor.exe` → PC del supervisor de módulos

### Operación del kiosco de registro

1. Ingresar DPI del pasajero (13 dígitos)
2. Presionar Enter o "Buscar" (o esperar al quitar el foco del campo)
3. Verificar que el nombre y tipo sean correctos
4. Agregar necesidades especiales si aplica
5. Presionar "Generar ticket →"
6. En el TicketStage: presionar "Imprimir" si hay impresora, luego "Listo"
7. El formulario se limpia automáticamente para el siguiente pasajero

### Operación de ventanillas

1. Presionar "Llamar siguiente" cuando la ventanilla esté libre
2. Si hay pasajero: se muestra su DPI, nombre y número de turno
3. Atender al pasajero
4. Seleccionar el vuelo del ComboBox
5. Agregar observaciones si aplica (asiento, situación especial, etc.)
6. Presionar "Finalizar atención"
7. El sistema registra el tiempo automáticamente
8. La pantalla se limpia y puede llamarse al siguiente

### Supervisión en tiempo real

**LogsApp (Tab "Logs en Tiempo Real"):**
- Los eventos llegan automáticamente al conectarse
- Usar filtros de módulo y nivel para encontrar eventos específicos
- Activar "Auto-scroll" para seguir el flujo en tiempo real

**LogsApp (Tab "Base de Datos"):**
- Ingresar la ruta al archivo `aeropuerto_datos.db`
- Presionar "Cargar / Actualizar"
- Filtrar por DPI, tipo de cola o estado

**MonitorApp:**
- El árbol se actualiza en tiempo real con las conexiones activas
- Hacer clic en una instancia para ver sus detalles
- El panel inferior permite iniciar/detener el servidor si es necesario

### Manejo de errores comunes

| Error | Causa | Solución |
|-------|-------|----------|
| "DPI ya tiene un ticket activo" | El pasajero ya está registrado | Verificar en LogsApp si ya fue llamado; puede haberse registrado dos veces |
| "No hay pasajeros en espera" | Cola vacía | Normal — esperar a que registren más pasajeros |
| "Sin conexión — reconectando..." | Servidor caído o red inestable | Verificar que `Servidor.exe` esté corriendo; el cliente reconectará solo |
| Badge rojo en el servidor | Puerto 5000 ya en uso | Cerrar la instancia anterior del servidor o usar otro puerto |
| TicketStage con "Error al registrar" | Problema de red al enviar | Verificar conexión y reintentar |

---

## 23. DPIs de Prueba Disponibles

Para realizar pruebas sin datos reales, usar los siguientes DPIs del catálogo RENAP simulado:

### Para la Cola General (edad < 60 años)
```
1234567890101  →  Carlos García López (41 años)
2345678901201  →  María José Pérez Morales (35 años)
3456789012301  →  Juan Pablo Méndez Castillo (50 años)
4567890123401  →  Ana Lucía Ramírez Solís (28 años)
6789012345601  →  Sofía Isabel Morales Paz (25 años)
7890123456701  →  Roberto Carlos Fuentes (45 años)
9012345678901  →  Miguel Ángel Torres Lima (33 años)
0123456789001  →  Elena Cristina Vargas (55 años)
1111111111111  →  Diego Andrino González (31 años)
2222222222222  →  Andrea Soto Castellanos (37 años)
5555555555555  →  Francisco Ajú Caal (20 años)
```

### Para la Cola Prioritaria (edad ≥ 60 — se asigna automáticamente)
```
5678901234501  →  Pedro Antonio Santos Cruz (63 años) ← PRIORITARIA AUTO
8901234567801  →  Laura Beatriz González (70 años)    ← PRIORITARIA AUTO
3333333333333  →  José Luis Girón Méndez (75 años)    ← PRIORITARIA AUTO
4444444444444  →  Carmen Alicia López (78 años)        ← PRIORITARIA AUTO
```

### DPIs no registrados (para probar el flujo manual)
Cualquier número de 13 dígitos que no esté en la lista anterior.  
Ejemplo: `9999999999999` → el operador deberá ingresar el nombre manualmente.

---

## 24. Decisiones de Diseño y Justificación

### ¿Por qué TCP en lugar de HTTP/REST?

TCP directo fue elegido por:
- **Simplicidad:** No requiere servidor HTTP, frameworks ni serialización JSON
- **Push nativo:** Para los monitores, el servidor puede empujar eventos sin polling
- **Control total:** El equipo implementa el protocolo, lo que facilita entenderlo y depurarlo
- **Apropiado para LAN:** En red local, la latencia extra de HTTP no compensa la complejidad añadida

### ¿Por qué protocolo de texto plano en lugar de binario?

- **Depurable:** Con `telnet` o `nc` se puede probar el servidor manualmente
- **Sin versioning:** No se necesita preocupar por serialización de objetos Java entre versiones
- **Legible en logs:** Los mensajes que aparecen en la consola son comprensibles directamente

### ¿Por qué implementar Cola y TablaHash desde cero?

Requisito académico del proyecto — demostrar comprensión de las estructuras de datos fundamentales en lugar de usar las implementaciones de la biblioteca estándar de Java.

### ¿Por qué `;;` como separador en la base de datos?

El protocolo de red usa `|` (pipe). Los nombres de vuelos pueden contener pipe implícitamente (como separadores en textos). El doble punto y coma `;;` es poco común en datos de aeropuerto y evita el conflicto.

### ¿Por qué reescribir el archivo completo en cada cambio?

Los registros se actualizan (marcarLlamada, marcarFin), no son solo inserciones. Implementar actualización parcial requeriría offset calculado, tamaño fijo por registro, o base de datos real (SQLite). Para el volumen del sistema (cientos de registros por día), reescribir es confiable y simple.

### ¿Por qué jpackage `app-image` en lugar de instalador `msi`?

- `msi` requiere WiX Toolset instalado y derechos de administrador
- `app-image` genera una carpeta autocontenida: copiar + ejecutar
- Para presentaciones y demos, la carpeta es más flexible que un instalador

### ¿Por qué LogInterceptor en lugar de logging framework (SLF4J, Log4j)?

- El equipo es sub-junior — un logging framework agrega configuración extra
- La captura de `System.out.println()` permite que todo el código existente genere logs sin modificación
- Mantiene el código de lógica de negocios limpio de referencias al sistema de logging

### ¿Por qué el estado activo (colas en memoria) se pierde al reiniciar el servidor?

Decisión deliberada de simplicidad. La alternativa sería:
- Al iniciar: cargar de `aeropuerto_datos.db` los registros EN_ESPERA y EN_ATENCION y reconstruir las colas
- Problema: los pasajeros EN_ATENCION habrán sido atendidos o estarán esperando nuevamente — el servidor no puede saberlo sin confirmación del operador

Para el caso de uso actual (demos y uso en el aeropuerto con supervisión directa), reiniciar el servidor implica que los operadores vuelven a llamar manualmente a los pasajeros que quedaron en espera. Los históricos completos (ATENDIDOS) siempre persisten.

---

## APÉNDICE A — Árbol de archivos completo

```
Aeropuerto-Sistema/                                    [Proyecto raíz]
├── pom.xml                                            [POM padre Maven]
├── package-beta.ps1                                   [Script jpackage]
├── dist-beta/                                         [Ejecutables generados]
│   ├── Servidor/Servidor.exe                          [152 MB]
│   ├── Registro/Registro.exe                          [161 MB]
│   ├── General/General.exe                            [161 MB]
│   ├── Prioritaria/Prioritaria.exe                    [161 MB]
│   ├── Especial/Especial.exe                          [161 MB]
│   ├── Logs/Logs.exe                                  [161 MB]
│   └── Monitor/Monitor.exe                            [161 MB]
├── common/src/main/java/com/aeropuerto/common/
│   ├── Mensaje.java          [Protocolo: serialización/deserialización]
│   ├── TipoMensaje.java      [Enum: 14 tipos de mensaje]
│   ├── Pasajero.java         [Entidad activa en cola]
│   ├── RegistroVisita.java   [Historial persistente, sep ;;]
│   ├── Cola.java             [Lista enlazada propia, O(1)]
│   ├── TablaHash.java        [Hash con encadenamiento, O(1)]
│   ├── BufferCircular.java   [Ring buffer, cap 2000 logs]
│   ├── CatalogoRENAP.java    [Simulación RENAP, 15 registros]
│   ├── TipoAtencion.java     [GENERAL | PRIORITARIA | ESPECIAL]
│   ├── EstadoPasajero.java   [EN_ESPERA | EN_ATENCION | ATENDIDO]
│   ├── LogEntry.java         [Evento de log inmutable]
│   └── ClienteInfo.java      [Metadatos de conexión]
├── server/src/main/java/com/aeropuerto/server/
│   ├── ServerMain.java       [Entrada, acepta conexiones]
│   ├── ClientHandler.java    [Un hilo por conexión]
│   ├── GestorColas.java      [Lógica: 3 colas + índice DPI]
│   ├── BaseDatos.java        [Persistencia aeropuerto_datos.db]
│   ├── LogManager.java       [Buffer + push a monitores]
│   ├── LogInterceptor.java   [Captura System.out.println()]
│   └── RegistroConexiones.java [Registro de clientes y monitores]
├── client-registro/src/main/java/com/aeropuerto/registro/
│   ├── RegistroApp.java      [JavaFX: kiosco de registro]
│   ├── ConexionServidor.java [Socket + auto-reconexión]
│   ├── TicketStage.java      [Modal del ticket generado]
│   └── Launcher.java         [Main para fat JAR]
├── client-general/
│   ├── GeneralApp.java       [JavaFX: ventanilla General]
│   └── ConexionServidor.java
├── client-prioritaria/
│   ├── PrioritariaApp.java
│   └── ConexionServidor.java
├── client-especial/
│   ├── EspecialApp.java      [+ clase, asiento, checkboxes VIP]
│   └── ConexionServidor.java
├── client-logs/
│   ├── LogsApp.java          [Tab logs + tab base de datos]
│   └── ConexionMonitor.java  [Socket push + auto-reconexión]
└── client-monitor/
    ├── MonitorApp.java       [Árbol de módulos + panel servidor]
    ├── ConexionMonitor.java
    └── NodoModulo.java       [Dato del TreeItem<NodoModulo>]
```

---

## APÉNDICE B — Comandos útiles de mantenimiento

### Compilar el proyecto completo
```powershell
$env:JAVA_HOME = "C:\Users\dandr\.jdks\openjdk-24.0.1"
$env:PATH = "$env:JAVA_HOME\bin;" + $env:PATH
& "C:\Program Files\JetBrains\IntelliJ IDEA 2025.1\plugins\maven\lib\maven3\bin\mvn.cmd" `
    clean package -DskipTests --no-transfer-progress
```

### Generar ejecutables (compilando desde cero)
```powershell
.\package-beta.ps1
```

### Generar ejecutables (solo empaquetar, JARs ya compilados)
```powershell
.\package-beta.ps1 -SkipBuild
```

### Probar el servidor manualmente (verificar protocolo)
```cmd
telnet 192.168.1.74 5000
```
Luego escribir (terminar con Enter):
```
IDENTIFICAR|REGISTRO|TEST-PC
```
El servidor responde: `IDENTIFICAR_OK`

### Abrir firewall para el servidor (una vez)
```cmd
netsh advfirewall firewall add rule name="AeroQueue Server" dir=in action=allow protocol=TCP localport=5000
```

### Ver la IP de la PC del servidor
```cmd
ipconfig | findstr IPv4
```

---

*Fin del informe técnico — AeroQueue v1.1*  
*Sistema de Gestión de Colas — Aeropuerto Internacional La Aurora, Guatemala*  
*Generado: 12 de mayo de 2026*
