package com.aeropuerto.common;

/**
 * Tipos de cola de atención disponibles en el aeropuerto.
 * Este enum viaja dentro de Mensaje como texto (name()) para no depender
 * de serialización de Java — compatibilidad total entre módulos.
 */
public enum TipoAtencion {
    GENERAL,      // Cola estándar
    PRIORITARIA,  // Adultos mayores, embarazadas, personas con discapacidad
    ESPECIAL      // VIP u otros criterios definidos por el aeropuerto
}
