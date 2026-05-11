package com.aeropuerto.server;

import com.aeropuerto.common.LogEntry;
import com.aeropuerto.common.LogEntry.Nivel;

import java.io.PrintStream;

/**
 * Interceptor de System.out para capturar todos los logs del servidor.
 *
 * Se instala en ServerMain con: System.setOut(new LogInterceptor(System.out))
 * Después de eso, cada System.out.println() del servidor (GestorColas,
 * ClientHandler, etc.) pasa por aquí y se persiste en LogManager.
 *
 * Sin modificar ninguna otra clase de lógica.
 */
public class LogInterceptor extends PrintStream {

    private final PrintStream original;
    private final LogManager  logManager;

    /** true durante la inicialización para evitar recursión al registrar logs de registro. */
    private final ThreadLocal<Boolean> enProceso = ThreadLocal.withInitial(() -> false);

    public LogInterceptor(PrintStream original, LogManager logManager) {
        super(original, true);  // autoFlush = true
        this.original   = original;
        this.logManager = logManager;
    }

    // Interceptamos println(String) — el más usado con el prefijo [XXX]
    @Override
    public void println(String x) {
        original.println(x);
        if (x != null && !x.isBlank() && !enProceso.get()) {
            enProceso.set(true);
            try {
                logManager.registrar(parsear(x));
            } finally {
                enProceso.set(false);
            }
        }
    }

    @Override
    public void println(Object x) {
        println(String.valueOf(x));
    }

    // ── Parsing ───────────────────────────────────────────────────────────────

    private LogEntry parsear(String linea) {
        Nivel  nivel  = inferirNivel(linea);
        String modulo = inferirModulo(linea);
        String msg    = limpiarPrefijo(linea);
        return new LogEntry(nivel, modulo, msg);
    }

    private String inferirModulo(String linea) {
        if (linea.startsWith("[SERVER]"))      return "SERVER";
        if (linea.startsWith("[HANDLER]"))     return "HANDLER";
        if (linea.startsWith("[GESTOR]"))      return "GESTOR";
        if (linea.startsWith("[CONEXION]"))    return "CONEXION";
        if (linea.startsWith("[REGISTRO]"))    return "REGISTRO";
        if (linea.startsWith("[GENERAL]"))     return "GENERAL";
        if (linea.startsWith("[PRIORITARIA]")) return "PRIORITARIA";
        if (linea.startsWith("[ESPECIAL]"))    return "ESPECIAL";
        if (linea.startsWith("[ERROR]"))       return "ERROR";
        return "SISTEMA";
    }

    private String limpiarPrefijo(String linea) {
        int cierre = linea.indexOf(']');
        if (cierre >= 0 && cierre + 2 <= linea.length()) {
            return linea.substring(cierre + 2);
        }
        return linea;
    }

    private Nivel inferirNivel(String linea) {
        String l = linea.toLowerCase();
        if (l.contains("error") || l.contains("exception") || l.contains("fallo"))
            return Nivel.ERROR;
        if (l.contains("advertencia") || l.contains("warning") || l.contains("sin conexi"))
            return Nivel.WARN;
        if (l.contains("registrado") || l.contains("llamado") || l.contains("atendido")
                || l.contains("conectado") || l.contains("desconectado") || l.contains("monitor"))
            return Nivel.ACTION;
        return Nivel.INFO;
    }
}
