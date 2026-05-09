package com.aeropuerto.general;

import com.aeropuerto.common.Mensaje;

import java.io.*;
import java.net.Socket;

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

    public void conectar() throws IOException {
        socket    = new Socket(host, puerto);
        entrada   = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        salida    = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        conectado = true;
    }

    public Mensaje enviarYRecibir(Mensaje mensaje) throws IOException {
        if (!conectado)
            throw new IllegalStateException("No hay conexion con el servidor");
        salida.println(mensaje.serializar());
        String respuesta = entrada.readLine();
        if (respuesta == null)
            throw new IOException("El servidor cerro la conexion");
        return Mensaje.deserializar(respuesta);
    }

    public void desconectar() {
        try {
            conectado = false;
            if (entrada != null) entrada.close();
            if (salida  != null) salida.close();
            if (socket  != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.out.println("[CONEXION] Error al desconectar: " + e.getMessage());
        }
    }

    public boolean isConectado() { return conectado; }
}
