package com.aeropuerto.logs;

import com.aeropuerto.common.LogEntry;
import com.aeropuerto.common.Mensaje;

import java.io.*;
import java.net.*;
import java.util.function.Consumer;

/**
 * Conexion persistente al servidor en modo LOGS (push).
 * El servidor empuja LOG_ENTRY en tiempo real y el historial al conectar.
 * Auto-reconexion cada 5 segundos si se pierde la conexion.
 */
public class ConexionLogs {

    private static final int REINTENTOS_MS = 5000;

    private Socket         socket;
    private BufferedReader entrada;
    private PrintWriter    salida;
    private volatile boolean activo            = false;
    private volatile boolean intentandoReconectar = false;

    private String  reconHost;
    private int     reconPuerto;
    private String  pcName;

    private Consumer<LogEntry> onLog;
    private Runnable           onDesconexion;
    private Runnable           onConexionRestaurada;

    // ── Callbacks ─────────────────────────────────────────────────────────────

    public void setOnLog(Consumer<LogEntry> cb)              { this.onLog = cb; }
    public void setOnDesconexion(Runnable cb)                { this.onDesconexion = cb; }
    public void setOnConexionRestaurada(Runnable cb)         { this.onConexionRestaurada = cb; }

    // ── Conexion ──────────────────────────────────────────────────────────────

    public void conectar(String host, int puerto) throws IOException {
        this.reconHost   = host;
        this.reconPuerto = puerto;
        try { pcName = InetAddress.getLocalHost().getHostName(); }
        catch (Exception e) { pcName = "PC-Logs"; }
        abrirSocket(host, puerto);
    }

    private void abrirSocket(String host, int puerto) throws IOException {
        socket  = new Socket(host, puerto);
        entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        salida  = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

        salida.println(Mensaje.identificar("LOGS", pcName).serializar());
        entrada.readLine(); // IDENTIFICAR_OK

        activo = true;

        Thread lector = new Thread(this::leer, "logs-reader");
        lector.setDaemon(true);
        lector.start();
    }

    public void desconectar() {
        activo               = false;
        intentandoReconectar = false;
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    public boolean isActivo() { return activo; }

    // ── Loop de lectura ───────────────────────────────────────────────────────

    private void leer() {
        try {
            String linea;
            while (activo && (linea = entrada.readLine()) != null) {
                procesarLinea(linea);
            }
        } catch (IOException e) {
            // caida real
        } finally {
            if (activo) {
                activo = false;
                if (onDesconexion != null) try { onDesconexion.run(); } catch (Exception ignored) {}
                iniciarReconexion();
            }
        }
    }

    private void procesarLinea(String linea) {
        try {
            Mensaje msg = Mensaje.deserializar(linea);
            if (msg.getTipo() != com.aeropuerto.common.TipoMensaje.LOG_ENTRY) return;
            if (onLog == null) return;

            long id    = Long.parseLong(msg.getCampo(0));
            String ts  = msg.getCampo(1);
            LogEntry.Nivel nivel = LogEntry.Nivel.valueOf(msg.getCampo(2));
            String modulo       = msg.getCampo(3);
            StringBuilder sb = new StringBuilder(msg.getCampo(4));
            for (int i = 5; i < msg.getCantidadCampos(); i++) sb.append("|").append(msg.getCampo(i));

            onLog.accept(LogEntry.desdeRed(id, ts, nivel, modulo, sb.toString()));
        } catch (Exception ignored) {}
    }

    private void iniciarReconexion() {
        if (intentandoReconectar || reconHost == null) return;
        intentandoReconectar = true;
        Thread hilo = new Thread(() -> {
            while (!activo && intentandoReconectar) {
                try { Thread.sleep(REINTENTOS_MS); } catch (InterruptedException e) { break; }
                try {
                    abrirSocket(reconHost, reconPuerto);
                    System.out.println("[LOGS] Reconexion exitosa.");
                    intentandoReconectar = false;
                    if (onConexionRestaurada != null)
                        try { onConexionRestaurada.run(); } catch (Exception ignored) {}
                    return;
                } catch (IOException e) {
                    System.out.println("[LOGS] Reintento fallido: " + e.getMessage());
                }
            }
            intentandoReconectar = false;
        }, "logs-reconexion");
        hilo.setDaemon(true);
        hilo.start();
    }
}