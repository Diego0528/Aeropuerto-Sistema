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
 *   2. ClientHandler lee el primer mensaje para identificar al cliente (IDENTIFICAR)
 *   3a. Si es monitor (LOGS/MONITOR): entra en modo push — el servidor le empuja eventos
 *   3b. Si es cliente operativo: loop normal de request-response
 *   4. Cuando el socket cierra → el hilo termina solo
 */
public class ClientHandler implements Runnable {

    private final Socket     socket;
    private final GestorColas gestor;
    private final String     clienteId;

    private BufferedReader entrada;
    private PrintWriter    salida;

    private ClienteInfo infoRegistrada = null; // null si no envió IDENTIFICAR

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
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            salida  = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

            String primeraLinea = entrada.readLine();
            if (primeraLinea == null) return; // desconexión inmediata

            // Intentar leer identificación
            ClienteInfo info = intentarIdentificar(primeraLinea);

            if (info != null && info.esMonitor()) {
                manejarMonitor(info);
            } else if (info != null) {
                // Cliente operativo identificado — loop normal (primera línea ya procesada)
                loopNormal(null);
            } else {
                // Sin IDENTIFICAR (compatibilidad hacia atrás) — procesar primera línea como mensaje
                loopNormal(primeraLinea);
            }

        } catch (IOException e) {
            System.out.println("[HANDLER] Cliente desconectado abruptamente: " + clienteId);
        } finally {
            if (infoRegistrada != null) {
                RegistroConexiones.getInstance().eliminarCliente(clienteId);
            }
            cerrarConexion();
        }

        System.out.println("[HANDLER] Hilo terminado para: " + clienteId);
    }

    // ── Identificación ────────────────────────────────────────────────────────

    /**
     * Intenta parsear una línea como IDENTIFICAR.
     * @return ClienteInfo si era IDENTIFICAR, null si era otro tipo de mensaje
     */
    private ClienteInfo intentarIdentificar(String linea) {
        try {
            Mensaje msg = Mensaje.deserializar(linea);
            if (msg.getTipo() != TipoMensaje.IDENTIFICAR) return null;

            String tipoStr  = msg.getCampo(0);
            String nombrePc = msg.getCampo(1);

            ClienteInfo.TipoCliente tipo;
            try { tipo = ClienteInfo.TipoCliente.valueOf(tipoStr); }
            catch (Exception e) { tipo = ClienteInfo.TipoCliente.DESCONOCIDO; }

            String ip     = socket.getInetAddress().getHostAddress();
            int    puerto = socket.getPort();
            ClienteInfo info = new ClienteInfo(ip, puerto, tipo, nombrePc);

            salida.println(Mensaje.identificarOk().serializar());

            if (!info.esMonitor()) {
                RegistroConexiones.getInstance().registrarCliente(info);
                infoRegistrada = info;
            }
            return info;

        } catch (Exception e) {
            return null; // No era IDENTIFICAR
        }
    }

    // ── Modo cliente operativo ────────────────────────────────────────────────

    private void loopNormal(String pendiente) throws IOException {
        // Procesar mensaje pendiente (primera línea si no era IDENTIFICAR)
        if (pendiente != null) {
            System.out.println("[HANDLER] Recibido de " + clienteId + ": " + pendiente);
            Mensaje resp = procesar(pendiente);
            if (resp != null) {
                salida.println(resp.serializar());
                System.out.println("[HANDLER] Enviado a " + clienteId + ": " + resp.serializar());
            }
        }

        // Loop principal
        String linea;
        while ((linea = entrada.readLine()) != null) {
            System.out.println("[HANDLER] Recibido de " + clienteId + ": " + linea);
            Mensaje resp = procesar(linea);
            if (resp != null) {
                String texto = resp.serializar();
                salida.println(texto);
                System.out.println("[HANDLER] Enviado a " + clienteId + ": " + texto);
            }
        }
    }

    // ── Modo monitor (push) ───────────────────────────────────────────────────

    private void manejarMonitor(ClienteInfo info) {
        RegistroConexiones registro = RegistroConexiones.getInstance();
        LogManager logManager = LogManager.getInstance();

        // Registrar monitor — envía estado actual de conexiones al nuevo monitor
        registro.registrarMonitor(clienteId, info, salida);

        // Enviar historial de logs (ráfaga inicial)
        logManager.empujarHistorialA(salida);

        System.out.println("[HANDLER] Monitor activo: " + clienteId + " [" + info.getTipo() + "]");

        // Mantener conexión viva — esperar a que el monitor se desconecte
        // (el servidor empujará eventos asíncronamente a través de 'salida')
        try {
            String linea;
            while ((linea = entrada.readLine()) != null) {
                try {
                    Mensaje msg = Mensaje.deserializar(linea);
                    if (msg.getTipo() == TipoMensaje.PING) {
                        salida.println(Mensaje.pong().serializar());
                    }
                } catch (Exception e) { /* ignorar mensajes malformados del monitor */ }
            }
        } catch (IOException e) {
            // Normal — monitor cerró la ventana
        } finally {
            registro.eliminarMonitor(clienteId);
            System.out.println("[HANDLER] Monitor desconectado: " + clienteId);
        }
    }

    // ── Procesamiento de mensajes operativos ──────────────────────────────────

    private Mensaje procesar(String lineaRecibida) {
        Mensaje mensaje;
        try {
            mensaje = Mensaje.deserializar(lineaRecibida);
        } catch (IllegalArgumentException e) {
            System.out.println("[HANDLER] Mensaje inválido de " + clienteId + ": " + e.getMessage());
            return Mensaje.error("Mensaje malformado: " + e.getMessage());
        }

        return switch (mensaje.getTipo()) {

            case REGISTRO -> {
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
                try {
                    TipoAtencion tipo = TipoAtencion.valueOf(mensaje.getCampo(0));
                    yield gestor.llamarSiguiente(tipo);
                } catch (Exception e) {
                    yield Mensaje.error("LLAMAR_SIGUIENTE mal formado: " + e.getMessage());
                }
            }

            case FIN_ATENCION -> {
                try {
                    String dpi = mensaje.getCampo(0);
                    yield gestor.finalizarAtencion(dpi);
                } catch (Exception e) {
                    yield Mensaje.error("FIN_ATENCION mal formado: " + e.getMessage());
                }
            }

            case PING -> Mensaje.pong();

            // IDENTIFICAR ya fue procesado antes de entrar a este loop
            case IDENTIFICAR -> null;

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
