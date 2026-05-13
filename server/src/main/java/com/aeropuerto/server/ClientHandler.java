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
 *   2. ClientHandler lee el primer mensaje → identifica al cliente (IDENTIFICAR)
 *   3a. Si es monitor (LOGS/MONITOR): modo push — servidor empuja eventos
 *   3b. Si es cliente operativo: loop request-response
 *   4. Socket cierra → hilo termina
 */
public class ClientHandler implements Runnable {

    private final Socket      socket;
    private final GestorColas gestor;
    private final String      clienteId;

    private BufferedReader entrada;
    private PrintWriter    salida;

    private ClienteInfo infoRegistrada = null;

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

            ClienteInfo info = intentarIdentificar(primeraLinea);

            if (info != null && info.esMonitor()) {
                manejarMonitor(info);
            } else if (info != null) {
                loopNormal(null); // identificación ya procesada
            } else {
                loopNormal(primeraLinea); // sin IDENTIFICAR — procesar como mensaje
            }

        } catch (IOException e) {
            System.out.println("[HANDLER] Cliente desconectado abruptamente: " + clienteId
                + " | Causa: " + e.getMessage());
        } finally {
            if (infoRegistrada != null) {
                RegistroConexiones.getInstance().eliminarCliente(clienteId);
            }
            cerrarConexion();
        }

        System.out.println("[HANDLER] Hilo terminado para: " + clienteId);
    }

    // ── Identificación ────────────────────────────────────────────────────────

    private ClienteInfo intentarIdentificar(String linea) {
        try {
            Mensaje msg = Mensaje.deserializar(linea);
            if (msg.getTipo() != TipoMensaje.IDENTIFICAR) return null;

            String tipoStr  = msg.getCampo(0);
            String nombrePc = msg.getCampo(1);

            ClienteInfo.TipoCliente tipo;
            try { tipo = ClienteInfo.TipoCliente.valueOf(tipoStr); }
            catch (Exception e) { tipo = ClienteInfo.TipoCliente.DESCONOCIDO; }

            ClienteInfo info = new ClienteInfo(
                socket.getInetAddress().getHostAddress(),
                socket.getPort(), tipo, nombrePc
            );

            salida.println(Mensaje.identificarOk().serializar());

            if (!info.esMonitor()) {
                RegistroConexiones.getInstance().registrarCliente(info);
                infoRegistrada = info;
            }
            return info;

        } catch (Exception e) {
            return null; // No era IDENTIFICAR — procesamos como mensaje normal
        }
    }

    // ── Modo cliente operativo ────────────────────────────────────────────────

    private void loopNormal(String pendiente) throws IOException {
        if (pendiente != null) {
            Mensaje resp = procesar(pendiente);
            if (resp != null) salida.println(resp.serializar());
        }

        String linea;
        while ((linea = entrada.readLine()) != null) {
            Mensaje resp = procesar(linea);
            if (resp != null) salida.println(resp.serializar());
        }
    }

    // ── Modo monitor (push) ───────────────────────────────────────────────────

    private void manejarMonitor(ClienteInfo info) {
        RegistroConexiones registro  = RegistroConexiones.getInstance();
        LogManager         logManager = LogManager.getInstance();

        registro.registrarMonitor(clienteId, info, salida);
        logManager.empujarHistorialA(salida);

        System.out.println("[HANDLER] Monitor activo: " + clienteId + " [" + info.getTipo() + "]");

        try {
            String linea;
            while ((linea = entrada.readLine()) != null) {
                try {
                    Mensaje msg = Mensaje.deserializar(linea);
                    if (msg.getTipo() == TipoMensaje.PING) {
                        salida.println(Mensaje.pong().serializar());
                    }
                } catch (Exception e) { /* ignorar mensajes malformados */ }
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
            return Mensaje.error("Mensaje inválido recibido — causa: " + e.getMessage());
        }

        return switch (mensaje.getTipo()) {

            case REGISTRO -> {
                try {
                    String dpi    = mensaje.getCampo(0);
                    String nombre = mensaje.getCampo(1);
                    TipoAtencion tipo = TipoAtencion.valueOf(mensaje.getCampo(2));
                    yield gestor.registrarPasajero(dpi, nombre, tipo);
                } catch (IllegalArgumentException e) {
                    yield Mensaje.error("Tipo de atención inválido en REGISTRO: " + e.getMessage());
                } catch (Exception e) {
                    yield Mensaje.error("Error al procesar REGISTRO: " + e.getMessage());
                }
            }

            case LLAMAR_SIGUIENTE -> {
                try {
                    TipoAtencion tipo = TipoAtencion.valueOf(mensaje.getCampo(0));
                    yield gestor.llamarSiguiente(tipo);
                } catch (IllegalArgumentException e) {
                    yield Mensaje.error("Tipo de cola inválido en LLAMAR_SIGUIENTE: " + e.getMessage());
                } catch (Exception e) {
                    yield Mensaje.error("Error al procesar LLAMAR_SIGUIENTE: " + e.getMessage());
                }
            }

            case FIN_ATENCION -> {
                try {
                    String dpi  = mensaje.getCampo(0);
                    // Campos opcionales con valores por defecto si no se envían
                    String vuelo = mensaje.getCantidadCampos() > 1 ? mensaje.getCampo(1) : "";
                    String obs   = mensaje.getCantidadCampos() > 2 ? mensaje.getCampo(2) : "";
                    long durSeg  = 0;
                    if (mensaje.getCantidadCampos() > 3) {
                        try { durSeg = Long.parseLong(mensaje.getCampo(3)); }
                        catch (NumberFormatException e) { durSeg = 0; }
                    }
                    yield gestor.finalizarAtencion(dpi, vuelo, obs, durSeg);
                } catch (Exception e) {
                    yield Mensaje.error("Error al procesar FIN_ATENCION: " + e.getMessage());
                }
            }

            case PING -> Mensaje.pong();

            case IDENTIFICAR -> null; // ya fue procesado antes del loop

            default -> Mensaje.error(
                "El servidor no reconoce el tipo de mensaje: " + mensaje.getTipo() +
                ". Tipos soportados: REGISTRO, LLAMAR_SIGUIENTE, FIN_ATENCION, PING, IDENTIFICAR."
            );
        };
    }

    // ── Limpieza ──────────────────────────────────────────────────────────────

    private void cerrarConexion() {
        try {
            if (entrada != null) entrada.close();
            if (salida  != null) salida.close();
            if (socket  != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.out.println("[HANDLER] Error menor al cerrar conexión " + clienteId + ": " + e.getMessage());
        }
    }
}
