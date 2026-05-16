package com.aeropuerto.common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Metadatos de un cliente conectado al servidor.
 * Creado cuando el cliente envía IDENTIFICAR — inmutable después.
 */
public class ClienteInfo {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public enum TipoCliente {
        REGISTRO, GENERAL, PRIORITARIA, ESPECIAL,
        LOGS, MONITOR,
        DESCONOCIDO
    }

    private final String      id;        // ip:puerto — clave única
    private final String      ip;
    private final int         puerto;
    private final TipoCliente tipo;
    private final String      nombrePc;
    private final String      timestamp; // hora de conexión formateada

    public ClienteInfo(String ip, int puerto, TipoCliente tipo, String nombrePc) {
        this.id        = ip + ":" + puerto;
        this.ip        = ip;
        this.puerto    = puerto;
        this.tipo      = tipo;
        this.nombrePc  = nombrePc;
        this.timestamp = LocalDateTime.now().format(FMT);
    }

    /** Constructor de deserialización (timestamp ya viene formateado). */
    private ClienteInfo(String ip, int puerto, TipoCliente tipo, String nombrePc, String timestamp) {
        this.id        = ip + ":" + puerto;
        this.ip        = ip;
        this.puerto    = puerto;
        this.tipo      = tipo;
        this.nombrePc  = nombrePc;
        this.timestamp = timestamp;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String      getId()        { return id; }
    public String      getIp()        { return ip; }
    public int         getPuerto()    { return puerto; }
    public TipoCliente getTipo()      { return tipo; }
    public String      getNombrePc()  { return nombrePc; }
    public String      getTimestamp() { return timestamp; }

    /** True si este cliente es un visor de logs o monitor de módulos. */
    public boolean esMonitor() {
        return tipo == TipoCliente.LOGS || tipo == TipoCliente.MONITOR;
    }

    /** Nombre visible para la UI del task manager. */
    public String etiquetaUI() {
        return nombrePc + "  —  " + ip + ":" + puerto + "  —  " + timestamp;
    }

    /** Nombre legible del tipo para cabeceras de grupo. */
    public static String nombreGrupo(TipoCliente tipo) {
        return switch (tipo) {
            case REGISTRO    -> "Registro";
            case GENERAL     -> "Cola General";
            case PRIORITARIA -> "Cola Prioritaria";
            case ESPECIAL    -> "Cola Especial";
            case LOGS        -> "Monitor — Logs";
            case MONITOR     -> "Monitor — Módulos";
            case DESCONOCIDO -> "Desconocido";
        };
    }

    // ── Serialización flat para el protocolo (sin anidamiento de '|') ─────────

    /**
     * Serializa como: ip|puerto|tipo|nombrePc|timestamp
     * Nota: nombrePc no debería contener '|'.
     */
    public String serializar() {
        return ip + "|" + puerto + "|" + tipo.name() + "|" + nombrePc + "|" + timestamp;
    }

    public static ClienteInfo deserializar(String datos) {
        String[] p = datos.split("\\|", 5);
        TipoCliente tipo;
        try { tipo = TipoCliente.valueOf(p[2]); }
        catch (Exception e) { tipo = TipoCliente.DESCONOCIDO; }
        return new ClienteInfo(p[0], Integer.parseInt(p[1]), tipo, p[3], p[4]);
    }

    @Override
    public String toString() {
        return "ClienteInfo{" + tipo + " " + id + " " + nombrePc + "}";
    }
}
