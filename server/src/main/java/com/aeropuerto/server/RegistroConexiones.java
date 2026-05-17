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
 *  - Empujar STATUS_UPDATE a todos los monitores al conectar/desconectar,
 *    incluyendo cuando los propios módulos LOGS/MONITOR se conectan o desconectan.
 *
 * Thread-safety: todos los métodos son synchronized.
 */
public class RegistroConexiones {

    private static RegistroConexiones instancia;

    // Clientes operativos: ip:puerto → ClienteInfo
    private final TablaHash<String, ClienteInfo> clientes;

    // Monitores: ip:puerto → PrintWriter (para empujar mensajes)
    private final TablaHash<String, PrintWriter> monitores;

    // Info de cada monitor: ip:puerto → ClienteInfo (para STATUS_UPDATE al conectar/desconectar)
    private final TablaHash<String, ClienteInfo> monitoresInfo;

    private RegistroConexiones() {
        clientes      = new TablaHash<>();
        monitores     = new TablaHash<>();
        monitoresInfo = new TablaHash<>();
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
     * Registra un monitor (LOGS o MONITOR) y:
     *  1. Notifica a todos los monitores YA conectados que este nuevo módulo llegó.
     *  2. Le envía al nuevo monitor el estado actual completo:
     *     - Todos los clientes operativos conectados.
     *     - Todos los otros monitores conectados.
     */
    public synchronized void registrarMonitor(String id, ClienteInfo info, PrintWriter writer) {
        // Notificar a monitores existentes que este nuevo módulo se conectó
        empujar(Mensaje.statusUpdate("CONECTADO", info).serializar());

        // Volcar estado actual al nuevo monitor: clientes operativos
        Object[] actualesClientes = clientes.obtenerValores();
        for (Object obj : actualesClientes) {
            ClienteInfo c = (ClienteInfo) obj;
            writer.println(Mensaje.statusUpdate("CONECTADO", c).serializar());
        }

        // Volcar estado actual al nuevo monitor: otros monitores (antes de insertar el nuevo)
        Object[] actualesMonitores = monitoresInfo.obtenerValores();
        for (Object obj : actualesMonitores) {
            ClienteInfo m = (ClienteInfo) obj;
            writer.println(Mensaje.statusUpdate("CONECTADO", m).serializar());
        }

        // Registrar el nuevo monitor después del volcado para no incluirse a sí mismo
        monitores.insertar(id, writer);
        monitoresInfo.insertar(id, info);

        System.out.println("[CONEXION] Monitor registrado: " + info);
    }

    /** Elimina un monitor y notifica al resto que se desconectó. */
    public synchronized void eliminarMonitor(String id) {
        ClienteInfo info = monitoresInfo.buscar(id);
        monitores.eliminar(id);
        monitoresInfo.eliminar(id);
        if (info != null) {
            empujar(Mensaje.statusUpdate("DESCONECTADO", info).serializar());
        }
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