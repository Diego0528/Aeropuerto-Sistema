# Arquitectura del Sistema

## Visión general

El sistema sigue una arquitectura **cliente-servidor** distribuida con comunicación por sockets TCP/IP. Un servidor central gestiona el estado de las tres colas, y múltiples clientes JavaFX se conectan a él para registrar pasajeros u operar ventanillas. Un segundo servidor de chat escucha en el puerto 5001 para el canal interno entre módulos.

```
┌────────────────────────────────────────────────────────────────────┐
│                          SERVIDOR CENTRAL                          │
│          ServerMain :5000              ChatServer :5001            │
│                                                                    │
│  ┌───────────────────────────────────┐  ┌─────────────────────┐    │
│  │       GestorColas (Singleton)     │  │   ChatServer        │    │
│  │                                   │  │   (hilo dedicado)   │    │
│  │  Cola<Pasajero>  TablaHash<K,V>   │  └─────────────────────┘    │
│  │  colaGeneral     indicePorDpi     │                             │
│  │  colaPrioritaria                  │  ┌─────────────────────┐    │
│  │  colaEspecial                     │  │  RegistroConexiones │    │
│  └───────────────────────────────────┘  │  TablaHash<id,info> │    │
│                                         └─────────────────────┘    │
│   ClientHandler(hilo x cliente)                                    │
└──────┬──────────────┬──────────────┬──────────────┬────────────────┘
       │ TCP :5000    │ TCP :5000    │ TCP :5000    │ TCP :5000
┌──────┴──────┐ ┌─────┴──────┐ ┌────┴──────┐ ┌────┴──────────┐
│   Registro  │ │  General   │ │ Priorit.  │ │   Especial    │
│  (kiosko)   │ │            │ │           │ │   (VIP)       │
└─────────────┘ └────────────┘ └───────────┘ └───────────────┘
       │ TCP :5000 (push)           │ TCP :5001 (chat)
┌──────┴──────┐                    │
│    Logs     │  ◄─────────────────┘
│   (visor)   │
└─────────────┘
┌─────────────┐
│   Monitor   │  ← push :5000 + lanza servidor desde la UI
│  (árbol UI) │
└─────────────┘
```

## Módulos Maven

El proyecto es un **multi-módulo Maven** con un POM padre y 7 módulos hijos:

```
Aeropuerto-Sistema/
├── pom.xml                  ← POM padre (gestión de versiones)
├── common/                  ← Código compartido entre todos
├── server/                  ← Servidor central (puertos 5000 y 5001)
├── client-registro/         ← Kiosko de registro (pasajeros)
├── client-general/          ← Ventanilla cola General
├── client-prioritaria/      ← Ventanilla cola Prioritaria
├── client-especial/         ← Ventanilla cola Especial/VIP
├── client-logs/             ← Visor de logs en tiempo real + exportación JSON
└── client-monitor/          ← Monitor de módulos (árbol de conexiones)
```

### Diagrama de dependencias

```
common  ←── server
        ←── client-registro
        ←── client-general
        ←── client-prioritaria
        ←── client-especial
        ←── client-logs
        ←── client-monitor
```

El módulo `common` no tiene dependencias externas (restricción del proyecto).

## Capa de transporte

- **Protocolo:** TCP/IP con texto plano (líneas terminadas en `\n`)
- **Puerto principal:** 5000 — solicitudes request/response + push de logs y status
- **Puerto de chat:** 5001 — mensajes broadcast entre módulos conectados
- **Modelo de hilos:** Un `ClientHandler` por cliente en hilo daemon; monitor/logs usan hilo lector continuo (push model)
- **Thread safety:** `GestorColas` y `RegistroConexiones` usan `synchronized`
- **Auto-reconexión:** todos los módulos reintentan cada 5 s si pierden conexión

## Estructuras de datos propias

| Estructura | Uso | Complejidad |
|---|---|---|
| `Cola<T>` (lista enlazada) | Una por tipo de atención | encolar/desencolar O(1) |
| `TablaHash<K,V>` (encadenamiento) | Índice DPI → Pasajero; registro de conexiones | buscar/insertar O(1) promedio |

Ambas implementadas desde cero, sin usar colecciones de Java.

## Distribución (.exe)

Cada módulo se empaqueta como carpeta portable (`dist/AeroQueue-<nombre>/`) con JRE embebido usando `jpackage --type app-image`. El usuario copia la carpeta y ejecuta el `.exe` sin necesitar Java instalado. La IP y puerto del servidor se configuran editando `config.txt` en la carpeta del ejecutable.
