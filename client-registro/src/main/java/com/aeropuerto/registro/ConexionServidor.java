package com.aeropuerto.registro;

import com.aeropuerto.common.Mensaje;

import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

/**
 * Conexión socket con el servidor central.
 *
 * NUEVA FUNCIONALIDAD — Auto-reconexión:
 *   Si la conexión se pierde (servidor caído o red inestable), esta clase
 *   inicia automáticamente un hilo de reintento cada 5 segundos.
 *   Los callbacks informan a la UI cuándo se pierde y cuándo se restaura.
 *
 * USO:
 *   ConexionServidor cs = new ConexionServidor(host, puerto);
 *   cs.setOnConexionPerdida(() -> Platform.runLater(() -> marcarDesconectado()));
 *   cs.setOnConexionRestaurada(() -> Platform.runLater(() -> marcarConectado()));
 *   cs.conectarEIdentificar("REGISTRO", nombrePc);
 */
public class ConexionServidor {

    private static final int REINTENTOS_CADA_MS = 5000; // 5 segundos entre intentos

    private Socket        socket;
    private BufferedReader entrada;
    private PrintWriter    salida;
    private volatile boolean conectado = false;

    private final String host;
    private final int    puerto;

    // Datos de identificación guardados para re-identificarse al reconectar
    private String tipoCliente = "";
    private String nombrePc    = "";

    // Callbacks de UI — se llaman desde el hilo de reconexión (usa Platform.runLater al registrar)
    private Runnable onConexionPerdida;
    private Runnable onConexionRestaurada;

    // Control del hilo de reconexión
    private volatile boolean intentandoReconectar = false;

    // ── Constructor ───────────────────────────────────────────────────────────

    public ConexionServidor(String host, int puerto) {
        this.host   = host;
        this.puerto = puerto;
    }

    // ── Callbacks ─────────────────────────────────────────────────────────────

    /** Se llama cuando la conexión se pierde inesperadamente. */
    public void setOnConexionPerdida(Runnable callback) {
        this.onConexionPerdida = callback;
    }

    /** Se llama cuando la reconexión automática tiene éxito. */
    public void setOnConexionRestaurada(Runnable callback) {
        this.onConexionRestaurada = callback;
    }

    // ── Conexión ──────────────────────────────────────────────────────────────

    /**
     * Abre el socket y se identifica con el servidor.
     * Guarda el tipo y nombre para poder re-identificarse en reconexiones.
     *
     * @throws IOException si no puede conectar (lanza excepción específica al caller)
     */
    public void conectarEIdentificar(String tipo, String pc) throws IOException {
        this.tipoCliente = tipo;
        this.nombrePc    = pc;
        conectar();
        enviarYRecibir(Mensaje.identificar(tipo, pc)); // lanza IOException si falla
    }

    /**
     * Abre la conexión con el servidor (sin identificar).
     * Preferir conectarEIdentificar() para uso normal.
     */
    public void conectar() throws IOException {
        try {
            socket = new Socket(host, puerto);
        } catch (IOException e) {
            if (!"localhost".equals(host) && !"127.0.0.1".equals(host)) {
                socket = new Socket("localhost", puerto);
                System.out.println("[CONEXION] Fallback a localhost:" + puerto);
            } else {
                throw e;
            }
        }
        entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        salida  = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        conectado = true;
        System.out.println("[CONEXION] Conectado a " + host + ":" + puerto);
    }

    /**
     * Envía un mensaje al servidor y espera la respuesta.
     *
     * Si la conexión se pierde, dispara el hilo de reconexión automática
     * y lanza la excepción apropiada para que el caller la muestre en la UI.
     *
     * @throws ConnectException       si el servidor no está disponible
     * @throws SocketTimeoutException si el servidor no respondió a tiempo
     * @throws SocketException        si la conexión fue interrumpida
     * @throws IOException            para cualquier otro error de red
     */
    public Mensaje enviarYRecibir(Mensaje mensaje) throws IOException {
        if (!conectado) {
            throw new SocketException("Sin conexión con el servidor. Reconectando...");
        }

        try {
            salida.println(mensaje.serializar());
            String respuesta = entrada.readLine();
            if (respuesta == null) {
                throw new SocketException("El servidor cerró la conexión inesperadamente.");
            }
            return Mensaje.deserializar(respuesta);

        } catch (IOException e) {
            // La conexión se perdió — iniciar reconexión automática
            conectado = false;
            iniciarReconexionAutomatica();
            throw e; // Re-lanzar para que el caller muestre el error en la UI
        }
    }

    // ── Reconexión automática ─────────────────────────────────────────────────

    private void iniciarReconexionAutomatica() {
        if (intentandoReconectar) return; // Ya hay un hilo de reconexión activo
        intentandoReconectar = true;

        // Notificar a la UI que se perdió la conexión
        if (onConexionPerdida != null) {
            try { onConexionPerdida.run(); }
            catch (Exception e) { /* ignorar errores en callbacks de UI */ }
        }

        Thread hiloReconexion = new Thread(() -> {
            System.out.println("[CONEXION] Iniciando reconexión automática a " + host + ":" + puerto + "...");

            while (!conectado && !Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(REINTENTOS_CADA_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

                try {
                    conectar(); // Abre nuevo socket

                    // Re-identificarse si tenemos datos de identificación
                    if (!tipoCliente.isEmpty()) {
                        enviarYRecibir(Mensaje.identificar(tipoCliente, nombrePc));
                    }

                    // ¡Reconexión exitosa!
                    System.out.println("[CONEXION] Reconexión exitosa a " + host + ":" + puerto);
                    intentandoReconectar = false;

                    if (onConexionRestaurada != null) {
                        try { onConexionRestaurada.run(); }
                        catch (Exception e) { /* ignorar */ }
                    }
                    return; // Salir del hilo

                } catch (IOException e) {
                    System.out.println("[CONEXION] Reintento fallido: " + mensajeError(e));
                }
            }

            intentandoReconectar = false;
        }, "reconexion-" + tipoCliente);

        hiloReconexion.setDaemon(true);
        hiloReconexion.start();
    }

    // ── Desconexión intencional ───────────────────────────────────────────────

    /**
     * Cierra la conexión limpiamente (por ejemplo, al cerrar la ventana).
     * Detiene también el hilo de reconexión si estaba activo.
     */
    public void desconectar() {
        conectado             = false;
        intentandoReconectar  = false; // Detener intentos de reconexión
        cerrarSocket();
        System.out.println("[CONEXION] Desconectado del servidor.");
    }

    private void cerrarSocket() {
        try {
            if (entrada != null) entrada.close();
            if (salida  != null) salida.close();
            if (socket  != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.out.println("[CONEXION] Error menor al cerrar: " + e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public boolean isConectado() { return conectado; }

    /**
     * Convierte una IOException en un mensaje legible para el operador.
     * Identifica el tipo de error para dar contexto real en la UI.
     */
    public static String mensajeError(IOException e) {
        if (e instanceof ConnectException) {
            return "Servidor no accesible. Verifique que el servidor esté encendido.";
        } else if (e instanceof SocketTimeoutException) {
            return "Sin respuesta del servidor. Tiempo de espera agotado.";
        } else if (e instanceof SocketException) {
            return "Conexión con el servidor interrumpida: " + e.getMessage();
        } else {
            return "Error de red: " + e.getMessage();
        }
    }
}
