package com.aeropuerto.registro;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Maneja la conexión TCP entre RegistroApp y el servidor central.
 * Abre el socket, envía mensajes con el protocolo definido y retorna la respuesta.
 *
 * Protocolo de mensajes (definido en el informe):
 *   Envío:    "REGISTRO|dpi|nombre|tipoAtencion\n"
 *   Respuesta: "CONFIRMACION|dpi|numeroCola\n"
 *            o "ERROR|mensajeError\n"
 *
 * Ruta: client-registro/src/main/java/com/aeropuerto/registro/ClienteSocket.java
 *
 * @author Pablo Acan
 */
public class ClienteSocket {

    private final String ip;
    private final int    puerto;

    private Socket       socket;
    private PrintWriter  salida;
    private BufferedReader entrada;

    private boolean conectado = false;

    // ── Constructor ──────────────────────────────────────────────────────────
    public ClienteSocket(String ip, int puerto) {
        this.ip     = ip;
        this.puerto = puerto;
    }

    // ── Conectar al servidor ─────────────────────────────────────────────────

    /**
     * Abre la conexión TCP con el servidor.
     * @throws IOException si no se puede conectar
     */
    public void conectar() throws IOException {
        socket  = new Socket(ip, puerto);
        salida  = new PrintWriter(socket.getOutputStream(), true);  // autoFlush = true
        entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        conectado = true;
    }

    // ── Enviar mensaje de registro ───────────────────────────────────────────

    /**
     * Envía un mensaje REGISTRO al servidor y retorna la respuesta.
     *
     * Formato enviado: "REGISTRO|dpi|nombre|tipoAtencion\n"
     *
     * @param dpi          DPI del pasajero (String)
     * @param nombre       Nombre completo
     * @param tipoAtencion "GENERAL", "PRIORITARIA" o "ESPECIAL"
     * @return Respuesta del servidor como String (ej: "CONFIRMACION|...|5")
     * @throws IOException si hay error de comunicación
     */
    public String enviarRegistro(String dpi, String nombre, String tipoAtencion) throws IOException {
        if (!conectado) {
            throw new IOException("No hay conexión con el servidor.");
        }

        // Construir mensaje según el protocolo
        String mensaje = "REGISTRO|" + dpi + "|" + nombre + "|" + tipoAtencion;
        salida.println(mensaje);  // println agrega \n automáticamente

        // Esperar respuesta del servidor (bloqueante)
        String respuesta = entrada.readLine();
        return respuesta;
    }

    // ── Cerrar conexión ───────────────────────────────────────────────────────

    public void desconectar() {
        try {
            conectado = false;
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Error al cerrar socket: " + e.getMessage());
        }
    }

    // ── Getters de estado ────────────────────────────────────────────────────

    public boolean isConectado() { return conectado; }
    public String  getIp()       { return ip; }
    public int     getPuerto()   { return puerto; }
}