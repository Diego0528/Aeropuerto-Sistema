package com.aeropuerto.common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Registro individual de log generado por el servidor.
 * Inmutable — una vez creado no se modifica.
 *
 * Serialización para protocolo: id|timestamp|nivel|modulo|mensaje
 * El campo "mensaje" siempre es el último para permitir que contenga el carácter '|'.
 */
public class LogEntry {

    public enum Nivel { INFO, ACTION, WARN, ERROR }

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static long contadorGlobal = 0;

    private final long   id;
    private final String timestamp;
    private final Nivel  nivel;
    private final String modulo;
    private final String mensaje;

    /** Constructor del servidor — asigna ID autoincremental y timestamp actual. */
    public LogEntry(Nivel nivel, String modulo, String mensaje) {
        synchronized (LogEntry.class) { this.id = ++contadorGlobal; }
        this.timestamp = LocalDateTime.now().format(FMT);
        this.nivel     = nivel;
        this.modulo    = modulo;
        this.mensaje   = mensaje;
    }

    /** Constructor privado para deserialización desde la red. */
    private LogEntry(long id, String timestamp, Nivel nivel, String modulo, String mensaje) {
        this.id        = id;
        this.timestamp = timestamp;
        this.nivel     = nivel;
        this.modulo    = modulo;
        this.mensaje   = mensaje;
    }

    /** Crea un LogEntry recibido por red (sin incrementar el contador global). */
    public static LogEntry desdeRed(long id, String timestamp, Nivel nivel, String modulo, String mensaje) {
        return new LogEntry(id, timestamp, nivel, modulo, mensaje);
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public long   getId()        { return id; }
    public String getTimestamp() { return timestamp; }
    public Nivel  getNivel()     { return nivel; }
    public String getModulo()    { return modulo; }
    public String getMensaje()   { return mensaje; }

    @Override
    public String toString() {
        return "[" + modulo + "/" + nivel + "] " + mensaje;
    }
}
