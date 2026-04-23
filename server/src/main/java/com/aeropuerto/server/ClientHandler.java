package com.aeropuerto.server;

import com.aeropuerto.common.*;

import java.io.*;
import java.net.Socket;

/**
 * Maneja la comunicación con UN cliente conectado.
 * Se ejecuta en su propio hilo — el servidor crea uno por cada conexión.
 *
 * Ciclo de vida:
 *   1. Cliente se conecta → ServerMain crea new ClientHandler(socket) y lo inicia
 *   2. ClientHandler lee mensajes en loop hasta que el cliente se desconecta
 *   3. Cada mensaje se procesa y se responde
 *   4. Cuando el socket cierra → el hilo termina solo
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final GestorColas gestor;
    private final String clienteId; // Para logs — IP:puerto del cliente

    private BufferedReader entrada;
    private PrintWriter    salida;

    // ── Constructor ──────────────────────────────────────────────────────────
    public ClientHandler(Socket socket) {
        this.socket    = socket;
        this.gestor    = GestorColas.getInstance();
        this.clienteId = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
    }

    // ── Hilo principal ────────────────────────────────────────────────────────
    @Override
    public void run() {
        System.out.println("[HANDLER] Cliente conectado: " + clienteId);

        try {
            // Configurar streams de texto sobre el socket
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            salida  = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            // true = autoFlush: cada println() se envía inmediatamente, sin buffer

            String lineaRecibida;

            // Loop principal — leer hasta que el cliente cierre la conexión
            while ((lineaRecibida = entrada.readLine()) != null) {
                System.out.println("[HANDLER] Recibido de " + clienteId + ": " + lineaRecibida);

                Mensaje respuesta = procesar(lineaRecibida);

                if (respuesta != null) {
                    String textoRespuesta = respuesta.serializar();
                    salida.println(textoRespuesta);
                    System.out.println("[HANDLER] Enviado a " + clienteId + ": " + textoRespuesta);
                }
            }

        } catch (IOException e) {
            // Esto ocurre normalmente cuando el cliente cierra la ventana
            System.out.println("[HANDLER] Cliente desconectado abruptamente: " + clienteId);
        } finally {
            cerrarConexion();
        }

        System.out.println("[HANDLER] Hilo terminado para: " + clienteId);
    }

    // ── Procesamiento de mensajes ─────────────────────────────────────────────

    /**
     * Decide qué hacer según el tipo de mensaje recibido.
     * @return El mensaje de respuesta, o null si no hay respuesta (ej: PONG no responde nada extra)
     */
    private Mensaje procesar(String lineaRecibida) {
        Mensaje mensaje;

        // Deserializar — si la línea es inválida, respondemos con ERROR
        try {
            mensaje = Mensaje.deserializar(lineaRecibida);
        } catch (IllegalArgumentException e) {
            System.out.println("[HANDLER] Mensaje inválido de " + clienteId + ": " + e.getMessage());
            return Mensaje.error("Mensaje malformado: " + e.getMessage());
        }

        // Procesar según tipo
        return switch (mensaje.getTipo()) {

            case REGISTRO -> {
                // Campos esperados: dpi|nombre|tipoAtencion
                try {
                    String dpi    = mensaje.getCampo(0);
                    String nombre = mensaje.getCampo(1);
                    TipoAtencion tipo = TipoAtencion.valueOf(mensaje.getCampo(2));
                    yield gestor.registrarPasajero(dpi, nombre, tipo);
                } catch (Exception e) {
                    yield Mensaje.error("REGISTRO mal formado: " + e.getMessage());
                }
            }

            case LLAMAR_SIGUIENTE -> {
                // Campos esperados: tipoAtencion
                try {
                    TipoAtencion tipo = TipoAtencion.valueOf(mensaje.getCampo(0));
                    yield gestor.llamarSiguiente(tipo);
                } catch (Exception e) {
                    yield Mensaje.error("LLAMAR_SIGUIENTE mal formado: " + e.getMessage());
                }
            }

            case FIN_ATENCION -> {
                // Campos esperados: dpi
                try {
                    String dpi = mensaje.getCampo(0);
                    yield gestor.finalizarAtencion(dpi);
                } catch (Exception e) {
                    yield Mensaje.error("FIN_ATENCION mal formado: " + e.getMessage());
                }
            }

            case PING -> Mensaje.pong();

            // Si el cliente manda algo que el servidor no espera
            default -> Mensaje.error("El servidor no maneja mensajes de tipo: " + mensaje.getTipo());
        };
    }

    // ── Limpieza ──────────────────────────────────────────────────────────────

    private void cerrarConexion() {
        try {
            if (entrada != null) entrada.close();
            if (salida  != null) salida.close();
            if (socket  != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.out.println("[HANDLER] Error al cerrar conexión: " + e.getMessage());
        }
        System.out.println("[HANDLER] Conexión cerrada: " + clienteId);
    }
}
