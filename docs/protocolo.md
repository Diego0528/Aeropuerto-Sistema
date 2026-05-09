# Protocolo de Comunicación TCP/IP

## Formato general

Todos los mensajes son texto plano con el formato:

```
TIPO|campo1|campo2|...\n
```

- El separador entre campos es `|`
- Cada mensaje termina con `\n` (lo agrega `PrintWriter.println()` automáticamente)
- La deserialización usa `split("\\|", -1)` para preservar campos vacíos

## Tipos de mensaje

### Cliente → Servidor

| Tipo | Campos | Descripción |
|---|---|---|
| `REGISTRO` | `dpi`, `nombre`, `tipoAtencion` | Registra un pasajero en la cola |
| `LLAMAR_SIGUIENTE` | `tipoAtencion` | El operador solicita al siguiente pasajero |
| `FIN_ATENCION` | `dpi` | El operador finaliza la atención |
| `PING` | _(ninguno)_ | Verificación de conexión |

### Servidor → Cliente

| Tipo | Campos | Descripción |
|---|---|---|
| `CONFIRMACION` | `dpi`, `numeroCola` | Operación exitosa |
| `ERROR` | `mensaje` | Operación fallida con descripción |
| `PASAJERO_LLAMADO` | `dpi`, `nombre`, `numeroCola` | Datos del siguiente pasajero |
| `COLA_VACIA` | `tipoAtencion` | No hay pasajeros en la cola |
| `PONG` | _(ninguno)_ | Respuesta al PING |

## Ejemplos de intercambio

### Registro exitoso
```
C → S:  REGISTRO|1234567890101|Carlos Garcia Lopez|GENERAL
S → C:  CONFIRMACION|1234567890101|7
```

### Registro duplicado
```
C → S:  REGISTRO|1234567890101|Carlos Garcia Lopez|GENERAL
S → C:  ERROR|El DPI 1234567890101 ya está registrado en el sistema
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

### Finalizar atención
```
C → S:  FIN_ATENCION|1234567890101
S → C:  CONFIRMACION|1234567890101|7
```

### Ping / Pong
```
C → S:  PING
S → C:  PONG
```

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
printWriter.println(m.serializar()); // agrega \n automáticamente
```

**Deserializar (al recibir):**
```java
String linea = bufferedReader.readLine(); // bloquea hasta recibir \n
Mensaje m = Mensaje.deserializar(linea);
if (m.getTipo() == TipoMensaje.CONFIRMACION) { ... }
```

**Usando métodos de fábrica:**
```java
// En el servidor
return Mensaje.confirmacion(dpi, numeroCola);
return Mensaje.pasajeroLlamado(dpi, nombre, turno);
return Mensaje.error("Mensaje de error");
```
