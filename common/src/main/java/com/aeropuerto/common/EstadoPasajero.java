package com.aeropuerto.common;

/**
 * Estado actual de un pasajero dentro del sistema.
 *
 * Ciclo de vida:
 *   EN_ESPERA → EN_ATENCION → ATENDIDO
 */
public enum EstadoPasajero {
    EN_ESPERA,    // Registrado y en la cola, aún no llamado
    EN_ATENCION,  // Ya fue llamado al mostrador, siendo atendido
    ATENDIDO      // Proceso finalizado
}
