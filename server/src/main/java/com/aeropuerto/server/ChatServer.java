package com.aeropuerto.server;

import java.io.*;
import java.net.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Servidor de chat interno para comunicacion entre modulos.
 * Corre en un hilo separado junto al servidor principal.
 * Puerto configurado en config.txt como puerto_chat (default 5001).
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
public class ChatServer implements Runnable {

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final int                         puerto;
    private final CopyOnWriteArrayList<Sesion> sesiones = new CopyOnWriteArrayList<>();

    public ChatServer(int puerto) {
        this.puerto = puerto;
    }

    @Override
    public void run() {
        try (ServerSocket ss = new ServerSocket(puerto)) {
            ss.setReuseAddress(true);
            System.out.println("[CHAT] Servidor de chat en puerto " + puerto);

            while (true) {
                Socket s = ss.accept();
                Sesion sesion = new Sesion(s);
                sesiones.add(sesion);
                Thread t = new Thread(sesion, "chat-" + s.getPort());
                t.setDaemon(true);
                t.start();
            }
        } catch (IOException e) {
            System.out.println("[CHAT] Error: " + e.getMessage());
        }
    }

    private void broadcast(String mensaje) {
        for (Sesion s : sesiones) s.enviar(mensaje);
    }

    private String listaParticipantes() {
        StringBuilder sb = new StringBuilder();
        for (Sesion s : sesiones) {
            if (!s.nombre.isBlank()) {
                if (sb.length() > 0) sb.append(";");
                sb.append(s.nombre).append(":").append(s.ip);
            }
        }
        return sb.toString();
    }

    // ── Sesion individual ─────────────────────────────────────────────────────

    private class Sesion implements Runnable {

        final Socket socket;
        PrintWriter  out;
        String nombre = "";
        String ip;

        Sesion(Socket s) {
            this.socket = s;
            this.ip     = s.getInetAddress().getHostAddress();
        }

        void enviar(String msg) {
            try { if (out != null) out.println(msg); }
            catch (Exception ignored) {}
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
                BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), "UTF-8"));

                String linea;
                while ((linea = in.readLine()) != null) {
                    String[] p = linea.split("\\|", -1);
                    if (p.length == 0) continue;

                    switch (p[0]) {
                        case "JOIN" -> {
                            nombre = p.length > 1 ? p[1] : "PC";
                            if (p.length > 2) ip = p[2];
                            String lista = listaParticipantes();
                            if (!lista.isBlank()) enviar("PARTICIPANTS|" + lista);
                            broadcast("JOINED|" + nombre + "|" + ip);
                            System.out.println("[CHAT] Unido: " + nombre + " (" + ip + ")");
                        }
                        case "MSG" -> {
                            if (p.length >= 4) {
                                String ts  = LocalTime.now().format(TS_FMT);
                                broadcast("BROADCAST|" + p[1] + "|" + p[2] + "|" + p[3] + "|" + ts);
                            }
                        }
                        case "LEAVE" -> { break; }
                    }
                }
            } catch (IOException e) {
                // cliente desconectado
            } finally {
                sesiones.remove(this);
                if (!nombre.isBlank()) {
                    broadcast("LEFT|" + nombre + "|" + ip);
                    System.out.println("[CHAT] Salio: " + nombre);
                }
                try { socket.close(); } catch (IOException ignored) {}
            }
        }
    }
}
