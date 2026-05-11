package com.aeropuerto.server;

import com.aeropuerto.common.LogEntry;
import com.aeropuerto.common.Mensaje;

import java.io.PrintWriter;

/**
 * Gestor de logs del servidor.
 * Singleton — un buffer en memoria + push a monitores conectados.
 *
 * El LogInterceptor llama a registrar() cada vez que el servidor
 * hace System.out.println(). Así capturamos todos los eventos sin
 * modificar GestorColas ni ClientHandler para logging.
 */
public class LogManager {

    private static LogManager instancia;
    private static final int CAPACIDAD_BUFFER = 2000;

    private final BufferCircular<LogEntry>  buffer;
    private final RegistroConexiones        registro;

    private LogManager() {
        this.buffer  = new BufferCircular<>(CAPACIDAD_BUFFER);
        this.registro = RegistroConexiones.getInstance();
    }

    public static synchronized LogManager getInstance() {
        if (instancia == null) instancia = new LogManager();
        return instancia;
    }

    /** Almacena el log en el buffer y lo empuja a todos los monitores. */
    public void registrar(LogEntry entry) {
        buffer.agregar(entry);
        registro.empujar(Mensaje.logEntry(entry).serializar());
    }

    /**
     * Envía todo el historial del buffer a un monitor recién conectado.
     * Se llama DESPUÉS de registrarMonitor() para evitar que el monitor
     * pierda eventos durante la ráfaga inicial.
     */
    public void empujarHistorialA(PrintWriter writer) {
        Object[] logs = buffer.obtenerTodos();
        for (Object obj : logs) {
            LogEntry entry = (LogEntry) obj;
            writer.println(Mensaje.logEntry(entry).serializar());
        }
    }

    public int totalLogs() { return buffer.tamaño(); }
}
