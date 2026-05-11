package com.aeropuerto.server;

import com.aeropuerto.common.ClienteInfo;
import com.aeropuerto.common.Mensaje;
import com.aeropuerto.common.TablaHash;

import java.io.PrintWriter;

/**
 * Registro central de todas las conexiones activas.
 * Singleton — un estado global para el servidor.
 *
 * Responsabilidades:
 *  - Trackear clientes operativos (REGISTRO, GENERAL, PRIORITARIA, ESPECIAL)
 *  - Trackear monitores (LOGS, MONITOR) con sus PrintWriters para push
 *  - Empujar STATUS_UPDATE a todos los monitores al conectar/desconectar
 *
 * Thread-safety: todos los métodos son synchronized.
 */
public class RegistroConexiones {

    private static RegistroConexiones instancia;

    // Clientes operativos: ip:puerto → ClienteInfo
    private final TablaHash<String, ClienteInfo> clientes;

    // Monitores: ip:puerto → PrintWriter (para empujar mensajes)
    private final TablaHash<String, PrintWriter> monitores;

    private RegistroConexiones() {
        clientes  = new TablaHash<>();
        monitores = new TablaHash<>();
    }

    public static synchronized RegistroConexiones getInstance() {
        if (instancia == null) instancia = new RegistroConexiones();
        return instancia;
    }

    // ── Clientes operativos ───────────────────────────────────────────────────

    /** Registra un cliente operativo y notifica a todos los monitores. */
    public synchronized void registrarCliente(ClienteInfo info) {
        clientes.insertar(info.getId(), info);
        empujar(Mensaje.statusUpdate("CONECTADO", info).serializar());
        System.out.println("[CONEXION] Registrado: " + info);
    }

    /** Elimina un cliente operativo y notifica a todos los monitores. */
    public synchronized void eliminarCliente(String id) {
        ClienteInfo info = clientes.buscar(id);
        if (info != null) {
            clientes.eliminar(id);
            empujar(Mensaje.statusUpdate("DESCONECTADO", info).serializar());
            System.out.println("[CONEXION] Eliminado: " + info);
        }
    }

    // ── Monitores ─────────────────────────────────────────────────────────────

    /**
     * Registra un monitor y le envía inmediatamente el estado actual
     * (todos los clientes conectados como STATUS_UPDATE CONECTADO).
     */
    public synchronized void registrarMonitor(String id, ClienteInfo info, PrintWriter writer) {
        monitores.insertar(id, writer);

        // Volcar estado actual al nuevo monitor
        Object[] actuales = clientes.obtenerValores();
        for (Object obj : actuales) {
            ClienteInfo c = (ClienteInfo) obj;
            writer.println(Mensaje.statusUpdate("CONECTADO", c).serializar());
        }

        System.out.println("[CONEXION] Monitor registrado: " + info);
    }

    /** Elimina un monitor (se desconectó). */
    public synchronized void eliminarMonitor(String id) {
        monitores.eliminar(id);
        System.out.println("[CONEXION] Monitor eliminado: " + id);
    }

    // ── Push a monitores ──────────────────────────────────────────────────────

    /** Envía un mensaje a todos los monitores conectados. */
    public synchronized void empujar(String mensaje) {
        Object[] escritores = monitores.obtenerValores();
        for (Object obj : escritores) {
            PrintWriter w = (PrintWriter) obj;
            try {
                w.println(mensaje);
            } catch (Exception e) {
                // Si el monitor falló, se eliminará cuando ClientHandler detecte la desconexión
            }
        }
    }
}
