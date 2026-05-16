package com.aeropuerto.monitor;

/**
 * Dato que representa un nodo en el TreeView del monitor.
 * clase = "GRUPO"     → cabecera de tipo de módulo
 * clase = "INSTANCIA" → conexión individual de un cliente
 */
public class NodoModulo {

    public final String clase;    // "GRUPO" | "INSTANCIA" | "ROOT"
    public final String tipo;     // "REGISTRO", "GENERAL", etc.
    public final String etiqueta; // texto mostrado en la celda

    // Solo INSTANCIA
    public final String ip;
    public final int    puerto;
    public final String nombrePc;
    public final String timestamp;

    /** Constructor para GRUPO y ROOT. */
    public NodoModulo(String clase, String tipo, String etiqueta) {
        this.clase    = clase;
        this.tipo     = tipo;
        this.etiqueta = etiqueta;
        this.ip        = "";
        this.puerto    = 0;
        this.nombrePc  = "";
        this.timestamp = "";
    }

    /** Constructor para INSTANCIA. */
    public NodoModulo(String clase, String tipo, String etiqueta,
                      String ip, int puerto, String nombrePc, String timestamp) {
        this.clase    = clase;
        this.tipo     = tipo;
        this.etiqueta = etiqueta;
        this.ip        = ip;
        this.puerto    = puerto;
        this.nombrePc  = nombrePc;
        this.timestamp = timestamp;
    }

    public static String nombreTipo(String tipo) {
        return switch (tipo) {
            case "REGISTRO"    -> "Registro de Pasajeros";
            case "GENERAL"     -> "Cola General";
            case "PRIORITARIA" -> "Cola Prioritaria";
            case "ESPECIAL"    -> "Cola Especial / VIP";
            case "LOGS"        -> "Monitor — Visor de Logs";
            case "MONITOR"     -> "Monitor — Módulos";
            default            -> tipo;
        };
    }

    @Override
    public String toString() { return etiqueta; }
}
