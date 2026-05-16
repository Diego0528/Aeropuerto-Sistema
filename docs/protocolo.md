# Protocolo de Comunicación TCP/IP

## Formato general

Todos los mensajes son texto plano con el formato:

```
TIPO|campo1|campo2|...\n
```

- El separador entre campos es `|`
- Cada mensaje termina con `\n` (lo agrega `PrintWriter.println()` automáticamente)
- La deserialización usa `split("\\|", -1)` para preservar campos vacíos

---

## Puerto 5000 — Protocolo principal

### Identificación (obligatoria al conectar)

Todo cliente debe identificarse al conectar antes de enviar cualquier otro mensaje:

| Dirección | Tipo | Campos | Descripción |
|---|---|---|---|
| C → S | `IDENTIFICAR` | `tipo`, `nombrePc` | Tipos: `REGISTRO`, `GENERAL`, `PRIORITARIA`, `ESPECIAL`, `LOGS`, `MONITOR` |
| S → C | `IDENTIFICAR_OK` | _(ninguno)_ | Confirmación; el servidor registra la conexión |

### Operaciones (Cliente → Servidor)

| Tipo | Campos | Descripción |
|---|---|---|
| `REGISTRO` | `dpi`, `nombre`, `tipoAtencion` | Registra un pasajero en la cola |
| `LLAMAR_SIGUIENTE` | `tipoAtencion` | El operador solicita al siguiente pasajero |
| `FIN_ATENCION` | `dpi` | El operador finaliza la atención |
| `PING` | _(ninguno)_ | Verificación de conexión |

### Respuestas (Servidor → Cliente)

| Tipo | Campos | Descripción |
|---|---|---|
| `CONFIRMACION` | `dpi`, `numeroCola` | Operación exitosa |
| `ERROR` | `mensaje` | Operación fallida con descripción |
| `PASAJERO_LLAMADO` | `dpi`, `nombre`, `numeroCola` | Datos del siguiente pasajero |
| `COLA_VACIA` | `tipoAtencion` | No hay pasajeros en la cola |
| `PONG` | _(ninguno)_ | Respuesta al PING |

### Push de estado y logs (Servidor → Monitor/Logs)

Estos mensajes se envían sin solicitud previa a los clientes `MONITOR` y `LOGS`:

| Tipo | Campos | Descripción |
|---|---|---|
| `STATUS_UPDATE` | `accion`, `ip`, `puerto`, `tipo`, `nombrePc`, `timestamp` | Notifica conexión/desconexión de un módulo. `accion`: `CONECTADO` o `DESCONECTADO` |
| `LOG_ENTRY` | `id`, `timestamp`, `nivel`, `modulo`, `mensaje...` | Evento de log; el mensaje puede contener `\|` escapados |

Al conectar un cliente `MONITOR`, el servidor le envía inmediatamente el estado actual de todas las conexiones (volcado de `RegistroConexiones`).

---

## Puerto 5001 — Chat interno

### Secuencia de conexión

```
C → S:  JOIN|NombrePC
S → todos:  BROADCAST|NombrePC|NombrePC se unio al chat
```

### Envío de mensaje

```
C → S:  MSG|NombrePC|Texto del mensaje
S → todos:  BROADCAST|NombrePC|Texto del mensaje
```

El servidor hace broadcast a todos los clientes de chat conectados, incluido el remitente.

---

## Ejemplos de intercambio

### Registro exitoso
```
C → S:  REGISTRO|1234567890101|Carlos Garcia Lopez|GENERAL
S → C:  CONFIRMACION|1234567890101|7
```

### Registro duplicado
```
C → S:  REGISTRO|1234567890101|Carlos Garcia Lopez|GENERAL
S → C:  ERROR|El DPI 1234567890101 ya esta registrado en el sistema
```

### Llamar siguiente (hay pasajero)
```
C → S:  LLAMAR_SIGUIENTE|GENERAL
S → C:  PASAJERO_LLAMADO|1234567890101|Carlos Garcia Lopez|7
```

### Llamar siguiente (cola vacía)
```
C → S:  LLAMAR_SIGUIENTE|PRIORITARIA
S → C:  COLA_VACIA|PRIORITARIA
```

### Identificación y push al Monitor
```
C → S:  IDENTIFICAR|MONITOR|PC-Monitor-01
S → C:  IDENTIFICAR_OK
S → C:  STATUS_UPDATE|CONECTADO|192.168.1.10|52341|REGISTRO|PC-Kiosko|2026-05-16 09:15:00
S → C:  STATUS_UPDATE|CONECTADO|192.168.1.11|52342|GENERAL|PC-Ventanilla|2026-05-16 09:16:00
```

---

## Valores del enum `TipoAtencion`

| Valor | Cola | Ventanilla |
|---|---|---|
| `GENERAL` | Cola estándar | client-general |
| `PRIORITARIA` | Adultos mayores, embarazadas, discapacidad | client-prioritaria |
| `ESPECIAL` | Pasajeros VIP | client-especial |

## Implementación en código

**Serializar (para enviar):**
```java
Mensaje m = Mensaje.registro(dpi, nombre, tipo);
printWriter.println(m.serializar()); // agrega \n automaticamente
```

**Deserializar (al recibir):**
```java
String linea = bufferedReader.readLine(); // bloquea hasta recibir \n
Mensaje m = Mensaje.deserializar(linea);
if (m.getTipo() == TipoMensaje.CONFIRMACION) { ... }
```