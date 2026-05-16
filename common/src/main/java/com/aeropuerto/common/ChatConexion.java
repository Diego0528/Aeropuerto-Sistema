package com.aeropuerto.common;

import java.io.*;
import java.net.*;

/**
 * Cliente del servidor de chat interno.
 * Conexion TCP persistente en un hilo de fondo — reconexion automatica.
 *
 * Protocolo (texto plano, una linea por mensaje):
 *   Cliente → Servidor:  JOIN|nombre|ip
 *                        MSG|nombre|ip|texto
 *                        LEAVE|nombre|ip
 *   Servidor → Clientes: JOINED|nombre|ip
 *                        LEFT|nombre|ip
 *                        BROADCAST|nombre|ip|texto|timestamp
 *                        PARTICIPANTS|nombre1:ip1;nombre2:ip2;...
 */
public class ChatConexion {

    private static final int REINTENTOS_MS = 5000;

    // ── DTOs ─────────────────────────────────────────────────────────────────

    public record Participante(String nombre, String ip) {}

    public record MensajeChat(String nombre, String ip, String texto, String timestamp) {}

    public interface Listener {
        void onMensaje(MensajeChat m);
        void onParticipante(Participante p, boolean unido);
        void onDesconexion();
        void onConexionRestaurada();
    }

    // ── Estado ────────────────────────────────────────────────────────────────

    private Socket         socket;
    private PrintWriter    salida;
    private BufferedReader entrada;
    private volatile boolean activo          = false;
    private volatile boolean reconectando    = false;

    private String   host;
    private int      puerto;
    private String   miNombre = "PC-Local";
    private String   miIp     = "0.0.0.0";
    private Listener listener;

    // ── API pública ───────────────────────────────────────────────────────────

    public void setListener(Listener l) { this.listener = l; }

    /** Conecta al servidor de chat y arranca el hilo lector. */
    public void conectar(String host, int puerto) throws IOException {
        this.host   = host;
        this.puerto = puerto;
        resolverIdentidad();
        abrirConexion();
    }

    /** Envia un mensaje de chat a todos los conectados. */
    public void enviarMensaje(String texto) {
        if (activo && salida != null)
            salida.println("MSG|" + miNombre + "|" + miIp + "|" + texto.replace("|", " "));
    }

    /** Cierra la conexion limpiamente. */
    public void desconectar() {
        activo      = false;
        reconectando = false;
        if (salida != null)
            try { salida.println("LEAVE|" + miNombre + "|" + miIp); }
            catch (Exception ignored) {}
        cerrar();
    }

    public boolean isActivo()    { return activo; }
    public String  getMiNombre() { return miNombre; }
    public String  getMiIp()     { return miIp; }

    // ── Internos ──────────────────────────────────────────────────────────────

    private void resolverIdentidad() {
        try {
            InetAddress addr = InetAddress.getLocalHost();
            miNombre = addr.getHostName();
            miIp     = addr.getHostAddress();
        } catch (Exception e) { /* usar defaults */ }
    }

    private void abrirConexion() throws IOException {
        socket  = new Socket(host, puerto);
        entrada = new BufferedReader(new InputStreamReader(socket.getInputStream(),  "UTF-8"));
        salida  = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
        activo  = true;

        salida.println("JOIN|" + miNombre + "|" + miIp);

        Thread lector = new Thread(this::leer, "chat-reader");
        lector.setDaemon(true);
        lector.start();
    }

    private void leer() {
        try {
            String linea;
            while (activo && (linea = entrada.readLine()) != null) {
                procesarLinea(linea);
            }
        } catch (IOException e) {
            if (activo) iniciarReconexion();
        }
    }

    private void procesarLinea(String linea) {
        if (listener == null) return;
        String[] p = linea.split("\\|", -1);
        if (p.length == 0) return;

        switch (p[0]) {
            case "BROADCAST" -> {
                if (p.length >= 5)
                    listener.onMensaje(new MensajeChat(p[1], p[2], p[3], p[4]));
            }
            case "JOINED" -> {
                if (p.length >= 3)
                    listener.onParticipante(new Participante(p[1], p[2]), true);
            }
            case "LEFT" -> {
                if (p.length >= 3)
                    listener.onParticipante(new Participante(p[1], p[2]), false);
            }
            case "PARTICIPANTS" -> {
                if (p.length >= 2 && !p[1].isBlank()) {
                    for (String entry : p[1].split(";")) {
                        String[] kv = entry.split(":", 2);
                        if (kv.length == 2)
                            listener.onParticipante(new Participante(kv[0], kv[1]), true);
                    }
                }
            }
        }
    }

    private void iniciarReconexion() {
        if (reconectando) return;
        reconectando = true;
        activo       = false;
        if (listener != null) listener.onDesconexion();
        cerrar();

        Thread hilo = new Thread(() -> {
            while (reconectando) {
                try { Thread.sleep(REINTENTOS_MS); } catch (InterruptedException e) { return; }
                try {
                    abrirConexion();
                    reconectando = false;
                    if (listener != null) listener.onConexionRestaurada();
                    return;
                } catch (IOException ignored) {}
            }
        }, "chat-reconexion");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void cerrar() {
        try { if (socket != null && !socket.isClosed()) socket.close(); }
        catch (IOException ignored) {}
    }
}
