package com.aeropuerto.common;

/**
 * Tipos de cola disponibles en el aeropuerto.
 * @author Pablo Acan
 */
public enum TipoAtencion {
    GENERAL,      // Cola estándar para cualquier pasajero
    PRIORITARIA,  // Adultos mayores, embarazadas, personas con discapacidad
    ESPECIAL      // VIP u otros criterios
}