package com.aeropuerto.common;

/**
 * Ciclo de vida de un pasajero dentro del sistema.
 * @author Pablo Acan
 */
public enum EstadoPasajero {
    EN_ESPERA,    // Registrado y en cola, aún no llamado
    EN_ATENCION,  // Fue llamado por el operador
    ATENDIDO      // Proceso completado
}