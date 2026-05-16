# Arquitectura del Sistema

## Visión general

El sistema sigue una arquitectura **cliente-servidor** distribuida con comunicación por sockets TCP/IP. Un servidor central gestiona el estado de las tres colas, y múltiples clientes JavaFX se conectan a él para registrar pasajeros u operar ventanillas.

```
┌─────────────────────────────────────────────────────────────────┐
│                        SERVIDOR CENTRAL                         │
│                      (ServerMain :5000)                         │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                   GestorColas (Singleton)               │   │
│  │                                                         │   │
│  │   Cola<Pasajero>  ←→  TablaHash<String, Pasajero>      │   │
│  │   colaGeneral          indicePorDpi                     │   │
│  │   colaPrioritaria                                       │   │
│  │   colaEspecial                                          │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│   ClientHandler(hilo)  ClientHandler(hilo)  ClientHandler(hilo) │
└──────────┬──────────────────┬───────────────────┬──────────────┘
           │ TCP :5000        │ TCP :5000         │ TCP :5000
    ┌──────┴──────┐    ┌──────┴──────┐    ┌──────┴──────┐
    │   Kiosko    │    │  Op. General│    │ Op. Priorit.│  ...
    │  (Registro) │    │             │    │             │
    └─────────────┘    └─────────────┘    └─────────────┘
```

## Módulos Maven

El proyecto es un **multi-módulo Maven** con un POM padre y 6 módulos hijos:

```
Aeropuerto-Sistema/
├── pom.xml                  ← POM padre (gestión de versiones)
├── common/                  ← Código compartido entre todos
├── server/                  ← Servidor central
├── client-registro/         ← Kiosko de registro (pasajeros)
├── client-general/          ← Ventanilla cola General
├── client-prioritaria/      ← Ventanilla cola Prioritaria
└── client-especial/         ← Ventanilla cola Especial/VIP
```

### Diagrama de dependencias

```
common  ←── server
        ←── client-registro
        ←── client-general
        ←── client-prioritaria
        ←── client-especial
```

El módulo `common` no tiene dependencias externas por diseño (restricción del proyecto).

## Capa de transporte

- **Protocolo:** TCP/IP con texto plano (líneas terminadas en `\n`)
- **Puerto por defecto:** 5000
- **Modelo de hilos:** Un `ClientHandler` por cliente, ejecutado en hilo daemon
- **Thread safety:** `GestorColas` usa `synchronized` en todos sus métodos

## Estructuras de datos propias

| Estructura | Uso | Complejidad |
|---|---|---|
| `Cola<T>` (lista enlazada) | Una por tipo de atención | encolar/desencolar O(1) |
| `TablaHash<K,V>` (encadenamiento) | Índice DPI → Pasajero | buscar/insertar O(1) promedio |

Ambas implementadas desde cero, sin usar colecciones de Java.
