package com.aeropuerto.common;

/**
 * Tipos de mensaje del protocolo de comunicación socket.
 *
 * Formato de cada mensaje en el cable:
 *   TIPO|campo1|campo2|...\n
 *
 * La barra vertical (|) es el separador. Cada mensaje termina con \n.
 * Se lee con BufferedReader.readLine() — simple y sin ambigüedades.
 *
 * ┌─────────────────────┬──────────────┬─────────────────────────────────────┐
 * │ TipoMensaje         │ Dirección    │ Campos                              │
 * ├─────────────────────┼──────────────┼─────────────────────────────────────┤
 * │ REGISTRO            │ C → S        │ dpi|nombre|tipoAtencion             │
 * │ CONFIRMACION        │ S → C        │ dpi|numeroCola                      │
 * │ ERROR               │ S → C        │ mensajeError                        │
 * │ LLAMAR_SIGUIENTE    │ C → S        │ tipoAtencion                        │
 * │ PASAJERO_LLAMADO    │ S → C        │ dpi|nombre|numeroCola               │
 * │ COLA_VACIA          │ S → C        │ tipoAtencion                        │
 * │ ESTADO_COLA         │ S → C        │ tipoAtencion|total|dpi1:nombre1:num1│
 * │ FIN_ATENCION        │ C → S        │ dpi                                 │
 * │ PING                │ C → S        │ (sin campos)                        │
 * │ PONG                │ S → C        │ (sin campos)                        │
 * └─────────────────────┴──────────────┴─────────────────────────────────────┘
 */
public enum TipoMensaje {
    // Cliente → Servidor
    REGISTRO,          // Registrar nuevo pasajero en una cola
    LLAMAR_SIGUIENTE,  // Operador pide el siguiente pasajero de su cola
    FIN_ATENCION,      // Operador marca pasajero como atendido
    PING,              // Verificar que el servidor sigue vivo

    // Servidor → Cliente
    CONFIRMACION,      // Registro exitoso
    ERROR,             // Algo salió mal (DPI duplicado, cola vacía al registrar, etc.)
    PASAJERO_LLAMADO,  // Respuesta a LLAMAR_SIGUIENTE con datos del pasajero
    COLA_VACIA,        // Respuesta a LLAMAR_SIGUIENTE cuando no hay nadie
    ESTADO_COLA,       // Actualización periódica del estado de una cola
    PONG               // Respuesta al PING
}
