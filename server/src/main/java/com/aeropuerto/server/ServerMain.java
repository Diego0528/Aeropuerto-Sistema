package com.aeropuerto.server;

import com.aeropuerto.common.CommonPlaceholder;

/**
 * Punto de entrada del Servidor Central.
 *
 * RESPONSABILIDADES FUTURAS (Diego):
 *   - Abrir ServerSocket en puerto configurable
 *   - Gestionar hilos por conexión de cliente (ThreadPool o Thread-per-client)
 *   - Mantener el estado global: colas activas, tabla hash de pasajeros
 *   - Procesar mensajes del protocolo (TipoMensaje) y responder
 *
 * Por ahora solo valida que el módulo common es accesible y compila.
 */
public class ServerMain {

    public static void main(String[] args) {
        System.out.println("=== " + CommonPlaceholder.version() + " ===");
        System.out.println("[SERVER] Iniciando servidor de colas del aeropuerto...");
        System.out.println("[SERVER] TODO: Implementar ServerSocket y lógica de colas.");
    }
}
