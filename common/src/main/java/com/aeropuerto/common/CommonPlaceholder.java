package com.aeropuerto.common;

/**
 * Placeholder temporal para que Maven compile el módulo común.
 *
 * REEMPLAZAR con las clases reales:
 *   - Pasajero.java          (DTO principal)
 *   - TipoAtencion.java      (enum: GENERAL, PRIORITARIA, ESPECIAL)
 *   - EstadoPasajero.java    (enum: EN_ESPERA, EN_ATENCION, ATENDIDO)
 *   - Mensaje.java           (protocolo de sockets)
 *   - TipoMensaje.java       (enum: REGISTRO, LLAMAR, ESTADO, etc.)
 *   - Cola.java              (lista enlazada genérica — Pablo)
 *   - TablaHash.java         (hash con encadenamiento por DPI — Pablo)
 *
 * Responsable de borrar esto: Pablo (cuando suba Cola.java y TablaHash.java)
 */
public class CommonPlaceholder {
    public static String version() {
        return "Airport Queue System v1.0-SNAPSHOT";
    }
}
