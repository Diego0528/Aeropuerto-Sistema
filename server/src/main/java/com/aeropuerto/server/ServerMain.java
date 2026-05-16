package com.aeropuerto.server;

import com.aeropuerto.common.ConfigServidor;

import java.io.*;
import java.net.*;

/**
 * Punto de entrada del servidor central.
 *
 * Responsabilidades:
 *   1. Leer configuracion desde config.txt (ip, puerto, puerto_chat)
 *   2. Arrancar el servidor de chat interno en un hilo separado
 *   3. Aceptar conexiones de modulos en loop infinito
 *   4. Por cada conexion → crear un ClientHandler en un hilo nuevo
 *
 * PARA CORRER:
 *   java -jar server-1.0-SNAPSHOT.jar
 *   Coloca config.txt en el mismo directorio que el JAR.
 */
public class ServerMain {

    public static void main(String[] args) {
        ConfigServidor cfg  = ConfigServidor.getInstance();
        int puerto          = cfg.getPuerto();
        int puertoChat      = cfg.getPuertoChat();

        // Override opcional por argumento: java -jar server.jar 6000
        if (args.length > 0) {
            try { puerto = Integer.parseInt(args[0]); }
            catch (NumberFormatException e) {
                System.out.println("[SERVER] Puerto invalido '" + args[0] + "'. Usando " + puerto);
            }
        }

        // ── Inicializar sistema de monitoreo ANTES del primer println ─────────
        RegistroConexiones.getInstance();
        LogManager logManager = LogManager.getInstance();
        System.setOut(new LogInterceptor(System.out, logManager));

        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║  AEROPUERTO GUATEMALA — SERVIDOR CENTRAL ║");
        System.out.println("╚══════════════════════════════════════════╝");
        System.out.println("[SERVER] Puerto principal: " + puerto);
        System.out.println("[SERVER] Puerto chat:      " + puertoChat);

        GestorColas.getInstance();
        System.out.println("[GESTOR] GestorColas inicializado con persistencia en disco.");

        // ── Arrancar servidor de chat en hilo separado ────────────────────────
        Thread hiloChat = new Thread(new ChatServer(puertoChat), "chat-server");
        hiloChat.setDaemon(true);
        hiloChat.start();

        // ── Servidor principal ────────────────────────────────────────────────
        try (ServerSocket serverSocket = new ServerSocket(puerto)) {
            serverSocket.setReuseAddress(true);

            System.out.println("[SERVER] GestorColas inicializado OK");
            System.out.println("[SERVER] Escuchando en puerto " + puerto);
            System.out.println("[SERVER] IP local: " + InetAddress.getLocalHost().getHostAddress());
            System.out.println("[SERVER] Esperando clientes... (Ctrl+C para detener)\n");

            while (true) {
                Socket socketCliente = serverSocket.accept();
                Thread hilo = new Thread(new ClientHandler(socketCliente));
                hilo.setDaemon(true);
                hilo.start();
                System.out.println("[SERVER] Nueva conexion aceptada. Hilo iniciado.");
            }

        } catch (BindException e) {
            System.out.println("[ERROR] El puerto " + puerto + " ya esta en uso.");
            System.out.println("        Cierra el proceso que lo usa o cambia el puerto en config.txt");
        } catch (IOException e) {
            System.out.println("[ERROR] Error en el servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
}