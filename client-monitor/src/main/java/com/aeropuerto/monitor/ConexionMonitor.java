package com.aeropuerto.monitor;

import com.aeropuerto.common.*;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * Conexión persistente de streaming hacia el servidor (modo MONITOR).
 * Igual que el ConexionMonitor de client-logs pero se identifica como MONITOR.
 */
public class ConexionMonitor {

    private Socket         socket;
    private BufferedReader entrada;
    private PrintWriter    salida;
    private volatile boolean activo = false;

    private Consumer<LogEntry>  onLog;
    private Consumer<StatusMsg> onStatus;
    private Runnable            onDesconexion;

    // ── Callbacks ─────────────────────────────────────────────────────────────

    public void setOnLog(Consumer<LogEntry> cb)        { this.onLog = cb; }
    public void setOnStatus(Consumer<StatusMsg> cb)    { this.onStatus = cb; }
    public void setOnDesconexion(Runnable cb)          { this.onDesconexion = cb; }

    // ── Conexión ──────────────────────────────────────────────────────────────

    public void conectar(String host, int puerto) throws IOException {
        socket  = new Socket(host, puerto);
        entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        salida  = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

        String pcName;
        try { pcName = InetAddress.getLocalHost().getHostName(); }
        catch (Exception e) { pcName = "PC-Monitor"; }

        salida.println(Mensaje.identificar("MONITOR", pcName).serializar());
        entrada.readLine(); // IDENTIFICAR_OK

        activo = true;
        Thread lector = new Thread(this::leer, "monitor-reader");
        lector.setDaemon(true);
        lector.start();
    }

    public void desconectar() {
        activo = false;
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
            if (activo && onDesconexion != null) onDesconexion.run();
        } finally {
            activo = false;
        }
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
