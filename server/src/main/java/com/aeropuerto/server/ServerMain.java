package com.aeropuerto.server;

import java.io.*;
import java.net.*;

/**
 * Punto de entrada del servidor central.
 *
 * Responsabilidades:
 *   1. Abrir un ServerSocket en el puerto configurado
 *   2. Aceptar conexiones entrantes en loop infinito
 *   3. Por cada conexión → crear un ClientHandler en un hilo nuevo
 *
 * El servidor NO termina solo — corre hasta que lo cierras manualmente
 * (Ctrl+C en terminal, o Stop en IntelliJ).
 *
 * PARA CORRER:
 *   Desde IntelliJ: click derecho en ServerMain → Run
 *   Con puerto custom: edita PUERTO abajo, o pásalo como argumento:
 *     java -jar server.jar 6000
 */
public class ServerMain {

    // Puerto por defecto. Cambia aquí si hay conflicto en la red del laboratorio.
    private static final int PUERTO_DEFAULT = 5000;

    public static void main(String[] args) {
        int puerto = PUERTO_DEFAULT;

        // Permitir puerto como argumento: java -jar server.jar 6000
        if (args.length > 0) {
            try {
                puerto = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("[SERVER] Puerto inválido '" + args[0] + "'. Usando " + PUERTO_DEFAULT);
            }
        }

        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║  AEROPUERTO GUATEMALA — SERVIDOR CENTRAL ║");
        System.out.println("╚══════════════════════════════════════════╝");
        System.out.println("[SERVER] Iniciando en puerto " + puerto + "...");

        // Pre-inicializar el gestor para detectar errores temprano
        GestorColas.getInstance();
        System.out.println("[SERVER] GestorColas inicializado OK");

        try (ServerSocket serverSocket = new ServerSocket(puerto)) {
            // SO_REUSEADDR: permite reiniciar el servidor rápido sin esperar
            // que el OS libere el puerto (útil en demos y pruebas)
            serverSocket.setReuseAddress(true);

            System.out.println("[SERVER] Escuchando en puerto " + puerto);
            System.out.println("[SERVER] IP local: " + InetAddress.getLocalHost().getHostAddress());
            System.out.println("[SERVER] Esperando clientes... (Ctrl+C para detener)\n");

            // Loop principal — corre para siempre
            while (true) {
                // accept() bloquea hasta que llega una conexión
                Socket socketCliente = serverSocket.accept();

                // Crear y lanzar hilo para este cliente
                // El servidor vuelve a accept() inmediatamente — no bloquea
                Thread hilo = new Thread(new ClientHandler(socketCliente));
                hilo.setDaemon(true); // El hilo termina si el servidor se detiene
                hilo.start();

                System.out.println("[SERVER] Nueva conexión aceptada. Hilo iniciado.");
            }

        } catch (BindException e) {
            System.out.println("[ERROR] El puerto " + puerto + " ya está en uso.");
            System.out.println("        Cierra el proceso que lo usa o cambia PUERTO_DEFAULT en ServerMain.java");
        } catch (IOException e) {
            System.out.println("[ERROR] Error en el servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
