package com.aeropuerto.server;

import com.aeropuerto.common.*;

/**
 * Gestor central del estado del servidor.
 *
 * Mantiene las tres colas de atención y la tabla hash de búsqueda por DPI.
 * Es un Singleton — existe una sola instancia en todo el servidor.
 *
 * IMPORTANTE — Thread safety:
 *   Múltiples clientes se conectan en hilos simultáneos. Todos acceden a este
 *   gestor. Los métodos son synchronized para evitar condiciones de carrera.
 *   Ejemplo del problema sin synchronized:
 *     Hilo A y Hilo B llaman encolarPasajero() al mismo tiempo → ambos leen
 *     contadorGeneral = 5 → ambos asignan numeroCola = 6 → dos pasajeros con
 *     el mismo número. synchronized previene eso.
 */
public class GestorColas {

    // ── Singleton ─────────────────────────────────────────────────────────────
    private static GestorColas instancia;

    public static synchronized GestorColas getInstance() {
        if (instancia == null) {
            instancia = new GestorColas();
        }
        return instancia;
    }

    // ── Estado del servidor ───────────────────────────────────────────────────
    private final Cola<Pasajero> colaGeneral;
    private final Cola<Pasajero> colaPrioritaria;
    private final Cola<Pasajero> colaEspecial;
    private final TablaHash<String, Pasajero> indicePorDpi;

    // Contadores independientes por cola para numerar turnos
    private int contadorGeneral;
    private int contadorPrioritaria;
    private int contadorEspecial;

    // ── Constructor privado ───────────────────────────────────────────────────
    private GestorColas() {
        colaGeneral      = new Cola<>();
        colaPrioritaria  = new Cola<>();
        colaEspecial     = new Cola<>();
        indicePorDpi     = new TablaHash<>();
        contadorGeneral      = 0;
        contadorPrioritaria  = 0;
        contadorEspecial     = 0;
    }

    // ── Operaciones principales ───────────────────────────────────────────────

    /**
     * Registra un nuevo pasajero en el sistema.
     *
     * @return El Mensaje de respuesta: CONFIRMACION si todo salió bien, ERROR si el DPI ya existe.
     */
    public synchronized Mensaje registrarPasajero(String dpi, String nombre, TipoAtencion tipo) {
        // Verificar duplicado por DPI
        if (indicePorDpi.contiene(dpi)) {
            return Mensaje.error("El DPI " + dpi + " ya está registrado en el sistema");
        }

        // Crear pasajero y asignar número de turno
        Pasajero pasajero = new Pasajero(dpi, nombre, tipo);
        int numeroCola = siguienteNumero(tipo);
        pasajero.setNumeroCola(numeroCola);

        // Insertar en la cola correspondiente y en el índice
        colaParaTipo(tipo).encolar(pasajero);
        indicePorDpi.insertar(dpi, pasajero);

        System.out.println("[GESTOR] Registrado: " + pasajero);
        return Mensaje.confirmacion(dpi, numeroCola);
    }

    /**
     * Llama al siguiente pasajero de la cola indicada.
     *
     * @return PASAJERO_LLAMADO con sus datos, o COLA_VACIA si no hay nadie.
     */
    public synchronized Mensaje llamarSiguiente(TipoAtencion tipo) {
        Cola<Pasajero> cola = colaParaTipo(tipo);

        if (cola.isEmpty()) {
            return Mensaje.colaVacia(tipo);
        }

        Pasajero pasajero = cola.desencolar();
        pasajero.setEstado(EstadoPasajero.EN_ATENCION);
        // Nota: lo dejamos en la TablaHash para poder marcarlo como ATENDIDO después

        System.out.println("[GESTOR] Llamado: " + pasajero);
        return Mensaje.pasajeroLlamado(pasajero.getDpi(), pasajero.getNombre(), pasajero.getNumeroCola());
    }

    /**
     * Marca un pasajero como completamente atendido.
     *
     * @return CONFIRMACION si se marcó bien, ERROR si el DPI no existe.
     */
    public synchronized Mensaje finalizarAtencion(String dpi) {
        Pasajero pasajero = indicePorDpi.buscar(dpi);

        if (pasajero == null) {
            return Mensaje.error("No se encontró pasajero con DPI: " + dpi);
        }

        pasajero.setEstado(EstadoPasajero.ATENDIDO);
        indicePorDpi.eliminar(dpi); // Ya no necesitamos indexarlo
        System.out.println("[GESTOR] Atendido: " + pasajero);
        return Mensaje.confirmacion(dpi, pasajero.getNumeroCola());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Cola<Pasajero> colaParaTipo(TipoAtencion tipo) {
        return switch (tipo) {
            case GENERAL      -> colaGeneral;
            case PRIORITARIA  -> colaPrioritaria;
            case ESPECIAL     -> colaEspecial;
        };
    }

    private int siguienteNumero(TipoAtencion tipo) {
        return switch (tipo) {
            case GENERAL      -> ++contadorGeneral;
            case PRIORITARIA  -> ++contadorPrioritaria;
            case ESPECIAL     -> ++contadorEspecial;
        };
    }

    // ── Consultas de estado (para ESTADO_COLA) ────────────────────────────────

    public synchronized int totalEnCola(TipoAtencion tipo) {
        return colaParaTipo(tipo).size();
    }
}
