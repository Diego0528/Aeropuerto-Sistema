package com.aeropuerto.monitor;

import com.aeropuerto.common.*;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * Conexión persistente de streaming hacia el servidor (modo MONITOR).
 * Mantiene el socket abierto, procesa mensajes en un hilo de fondo (push model)
 * y reconecta automáticamente cada 5 segundos si se pierde la conexión.
 */
public class ConexionMonitor {

    private static final int REINTENTOS_CADA_MS = 5000;

    private Socket         socket;
    private BufferedReader entrada;
    private PrintWriter    salida;
    private volatile boolean activo              = false;
    private volatile boolean intentandoReconectar = false;

    private String reconectarHost;
    private int    reconectarPuerto;
    private String reconectarPc;

    private Consumer<LogEntry>  onLog;
    private Consumer<StatusMsg> onStatus;
    private Runnable            onDesconexion;
    private Runnable            onConexionRestaurada;

    // ── Callbacks ─────────────────────────────────────────────────────────────

    public void setOnLog(Consumer<LogEntry> cb)              { this.onLog = cb; }
    public void setOnStatus(Consumer<StatusMsg> cb)          { this.onStatus = cb; }
    public void setOnDesconexion(Runnable cb)                { this.onDesconexion = cb; }
    public void setOnConexionRestaurada(Runnable cb)         { this.onConexionRestaurada = cb; }

    // ── Conexión ──────────────────────────────────────────────────────────────

    public void conectar(String host, int puerto) throws IOException {
        this.reconectarHost   = host;
        this.reconectarPuerto = puerto;

        try { reconectarPc = InetAddress.getLocalHost().getHostName(); }
        catch (Exception e) { reconectarPc = "PC-Monitor"; }

        try {
            abrirSocket(host, puerto, reconectarPc);
        } catch (IOException e) {
            // Fallo inicial — intentar localhost como fallback antes de reconectar
            try {
                if (!"localhost".equals(host) && !"127.0.0.1".equals(host)) {
                    abrirSocket("localhost", puerto, reconectarPc);
                    return; // conectado via localhost
                }
            } catch (IOException ignored) { }
            // Ninguno funcionó — iniciar auto-reconexión y propagar error
            iniciarReconexionAutomatica();
            throw e;
        }
    }

    /** Abre el socket, identifica la sesión y arranca el hilo lector. */
    private void abrirSocket(String host, int puerto, String pcName) throws IOException {
        socket  = new Socket(host, puerto);
        entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        salida  = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

        salida.println(Mensaje.identificar("MONITOR", pcName).serializar());
        entrada.readLine(); // IDENTIFICAR_OK

        activo = true;
        Thread lector = new Thread(this::leer, "monitor-reader");
        lector.setDaemon(true);
        lector.start();
    }

    public void desconectar() {
        activo = false;
        intentandoReconectar = false;
        try { if (socket != null) socket.close(); } catch (IOException e) { /* ignore */ }
    }

    public boolean isActivo() { return activo; }

    public void ping() {
        if (activo && salida != null) salida.println(Mensaje.ping().serializar());
    }

    // ── Loop de lectura ───────────────────────────────────────────────────────

    private void leer() {
        try {
            String linea;
            while (activo && (linea = entrada.readLine()) != null) {
                procesarLinea(linea);
            }
        } catch (IOException e) {
            // caída real del socket — distinguir del cierre intencional
        } finally {
            if (activo) {
                activo = false;
                if (onDesconexion != null) try { onDesconexion.run(); } catch (Exception ignored) {}
                iniciarReconexionAutomatica();
            }
        }
    }

    private void iniciarReconexionAutomatica() {
        if (intentandoReconectar || reconectarHost == null) return;
        intentandoReconectar = true;
        Thread hilo = new Thread(() -> {
            while (!activo && intentandoReconectar) {
                try { Thread.sleep(REINTENTOS_CADA_MS); } catch (InterruptedException e) { break; }
                try {
                    abrirSocket(reconectarHost, reconectarPuerto, reconectarPc);
                    System.out.println("[MONITOR] Reconexión exitosa.");
                    intentandoReconectar = false;
                    if (onConexionRestaurada != null) try { onConexionRestaurada.run(); } catch (Exception ignored) {}
                    return;
                } catch (IOException e) {
                    System.out.println("[MONITOR] Reintento fallido: " + e.getMessage());
                }
            }
            intentandoReconectar = false;
        }, "reconexion-monitor");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void procesarLinea(String linea) {
        try {
            Mensaje msg = Mensaje.deserializar(linea);
            switch (msg.getTipo()) {

                case LOG_ENTRY -> {
                    if (onLog == null) break;
                    long id    = Long.parseLong(msg.getCampo(0));
                    String ts  = msg.getCampo(1);
                    LogEntry.Nivel nivel = LogEntry.Nivel.valueOf(msg.getCampo(2));
                    String modulo       = msg.getCampo(3);
                    StringBuilder sb = new StringBuilder(msg.getCampo(4));
                    for (int i = 5; i < msg.getCantidadCampos(); i++) sb.append("|").append(msg.getCampo(i));
                    onLog.accept(LogEntry.desdeRed(id, ts, nivel, modulo, sb.toString()));
                }

                case STATUS_UPDATE -> {
                    if (onStatus == null) break;
                    onStatus.accept(new StatusMsg(
                            msg.getCampo(0),
                            msg.getCampo(1),
                            Integer.parseInt(msg.getCampo(2)),
                            msg.getCampo(3),
                            msg.getCampo(4),
                            msg.getCampo(5)
                    ));
                }

                default -> { }
            }
        } catch (Exception e) { /* ignorar malformados */ }
    }

    // ── DTO ───────────────────────────────────────────────────────────────────

    public static class StatusMsg {
        public final String accion, ip, tipo, nombrePc, timestamp;
        public final int    puerto;

        public StatusMsg(String accion, String ip, int puerto,
                         String tipo, String nombrePc, String timestamp) {
            this.accion    = accion;
            this.ip        = ip;
            this.puerto    = puerto;
            this.tipo      = tipo;
            this.nombrePc  = nombrePc;
            this.timestamp = timestamp;
        }

        public String getId() { return ip + ":" + puerto; }

        public String etiquetaUI() {
            return nombrePc + "  —  " + ip + ":" + puerto + "  —  " + timestamp;
        }
    }
}
