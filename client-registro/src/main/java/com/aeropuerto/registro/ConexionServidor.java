package com.aeropuerto.registro;

import com.aeropuerto.common.Mensaje;

import java.io.*;
import java.net.Socket;

/**
 * Maneja la conexión socket con el servidor central.
 *
 * Cada cliente JavaFX tiene una instancia de esta clase.
 * Se conecta al iniciar la app y se desconecta al cerrarla.
 *
 * NOTA: Esta clase se puede copiar igual en client-general,
 * client-prioritaria y client-especial — solo cambia el paquete.
 */
public class ConexionServidor {

    private Socket socket;
    private BufferedReader entrada;
    private PrintWriter salida;

    private final String host;
    private final int puerto;
    private boolean conectado = false;

    public ConexionServidor(String host, int puerto) {
        this.host   = host;
        this.puerto = puerto;
    }

    // ── Conexión ──────────────────────────────────────────────────────────────

    /**
     * Abre la conexión con el servidor.
     * @throws IOException si no puede conectar (servidor apagado, IP incorrecta, etc.)
     */
    public void conectar() throws IOException {
        socket  = new Socket(host, puerto);
        entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        salida  = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        conectado = true;
        System.out.println("[CONEXION] Conectado a " + host + ":" + puerto);
    }

    /**
     * Envía un mensaje al servidor y espera la respuesta.
     * @param mensaje El mensaje a enviar
     * @return La respuesta del servidor como Mensaje
     * @throws IOException si la conexión se perdió
     */
    public Mensaje enviarYRecibir(Mensaje mensaje) throws IOException {
        if (!conectado)
            throw new IllegalStateException("No hay conexión con el servidor");

        // Enviar
        salida.println(mensaje.serializar());

        // Esperar respuesta (readLine bloquea hasta que llega)
        String respuesta = entrada.readLine();
        if (respuesta == null)
            throw new IOException("El servidor cerró la conexión");

        return Mensaje.deserializar(respuesta);
    }

    // ── Desconexión ───────────────────────────────────────────────────────────

    public void desconectar() {
        try {
            conectado = false;
            if (entrada != null) entrada.close();
            if (salida  != null) salida.close();
            if (socket  != null && !socket.isClosed()) socket.close();
            System.out.println("[CONEXION] Desconectado del servidor");
        } catch (IOException e) {
            System.out.println("[CONEXION] Error al desconectar: " + e.getMessage());
        }
    }

    public boolean isConectado() { return conectado; }
}
