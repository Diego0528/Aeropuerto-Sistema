package com.aeropuerto.especial;

import com.aeropuerto.common.Mensaje;

import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

/** Conexión socket con auto-reconexión — Cola Especial/VIP. Idéntico al de client-registro. */
public class ConexionServidor {

    private static final int REINTENTOS_CADA_MS = 5000;

    private Socket         socket;
    private BufferedReader entrada;
    private PrintWriter    salida;
    private volatile boolean conectado = false;

    private final String host;
    private final int    puerto;

    private String   tipoCliente          = "";
    private String   nombrePc             = "";
    private Runnable onConexionPerdida;
    private Runnable onConexionRestaurada;
    private volatile boolean intentandoReconectar = false;

    public ConexionServidor(String host, int puerto) {
        this.host   = host;
        this.puerto = puerto;
    }

    public void setOnConexionPerdida(Runnable cb)    { this.onConexionPerdida    = cb; }
    public void setOnConexionRestaurada(Runnable cb) { this.onConexionRestaurada = cb; }

    public void conectarEIdentificar(String tipo, String pc) throws IOException {
        this.tipoCliente = tipo;
        this.nombrePc    = pc;
        conectar();
        enviarYRecibir(Mensaje.identificar(tipo, pc));
    }

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
        entrada   = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        salida    = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        conectado = true;
        System.out.println("[CONEXION] Conectado a " + host + ":" + puerto);
    }

    public Mensaje enviarYRecibir(Mensaje mensaje) throws IOException {
        if (!conectado) throw new SocketException("Sin conexión con el servidor. Reconectando...");
        try {
            salida.println(mensaje.serializar());
            String r = entrada.readLine();
            if (r == null) throw new SocketException("El servidor cerró la conexión.");
            return Mensaje.deserializar(r);
        } catch (IOException e) {
            conectado = false;
            iniciarReconexionAutomatica();
            throw e;
        }
    }

    private void iniciarReconexionAutomatica() {
        if (intentandoReconectar) return;
        intentandoReconectar = true;
        if (onConexionPerdida != null) try { onConexionPerdida.run(); } catch (Exception ignored) {}
        Thread hilo = new Thread(() -> {
            while (!conectado && !Thread.currentThread().isInterrupted()) {
                try { Thread.sleep(REINTENTOS_CADA_MS); } catch (InterruptedException e) { break; }
                try {
                    conectar();
                    if (!tipoCliente.isEmpty()) enviarYRecibir(Mensaje.identificar(tipoCliente, nombrePc));
                    System.out.println("[CONEXION] Reconexión exitosa.");
                    intentandoReconectar = false;
                    if (onConexionRestaurada != null) try { onConexionRestaurada.run(); } catch (Exception ignored) {}
                    return;
                } catch (IOException e) {
                    System.out.println("[CONEXION] Reintento fallido: " + mensajeError(e));
                }
            }
            intentandoReconectar = false;
        }, "reconexion-" + tipoCliente);
        hilo.setDaemon(true);
        hilo.start();
    }

    public void desconectar() {
        conectado = false;
        intentandoReconectar = false;
        try {
            if (entrada != null) entrada.close();
            if (salida  != null) salida.close();
            if (socket  != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }

    public boolean isConectado() { return conectado; }

    public static String mensajeError(IOException e) {
        if (e instanceof ConnectException)       return "Servidor no accesible. Verifique que esté encendido.";
        if (e instanceof SocketTimeoutException) return "Sin respuesta del servidor. Tiempo de espera agotado.";
        if (e instanceof SocketException)        return "Conexión interrumpida: " + e.getMessage();
        return "Error de red: " + e.getMessage();
    }
}
