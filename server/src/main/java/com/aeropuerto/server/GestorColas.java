package com.aeropuerto.server;

import com.aeropuerto.common.*;

/**
 * Gestor central del estado del servidor.
 *
 * Mantiene las tres colas de atención, la tabla hash activa por DPI,
 * y delega la persistencia histórica a BaseDatos.
 *
 * THREAD SAFETY: todos los métodos son synchronized porque múltiples
 * ClientHandler (hilos) acceden a este gestor simultáneamente.
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

    // ── Estado activo (en memoria, se pierde al reiniciar) ────────────────────
    private final Cola<Pasajero> colaGeneral;
    private final Cola<Pasajero> colaPrioritaria;
    private final Cola<Pasajero> colaEspecial;

    // Índice DPI → Pasajero activo (solo los que están EN_ESPERA o EN_ATENCION)
    private final TablaHash<String, Pasajero> indicePorDpi;

    // Contadores de turno independientes por cola
    private int contadorGeneral;
    private int contadorPrioritaria;
    private int contadorEspecial;

    // ── Constructor ───────────────────────────────────────────────────────────
    private GestorColas() {
        colaGeneral     = new Cola<>();
        colaPrioritaria = new Cola<>();
        colaEspecial    = new Cola<>();
        indicePorDpi    = new TablaHash<>();

        contadorGeneral     = 0;
        contadorPrioritaria = 0;
        contadorEspecial    = 0;

        // Pre-inicializar la base de datos (carga historial desde disco)
        BaseDatos.getInstance();
        System.out.println("[GESTOR] GestorColas inicializado con persistencia en disco.");
    }

    // ── Operaciones principales ───────────────────────────────────────────────

    /**
     * Registra un nuevo pasajero en la cola indicada.
     *
     * Errores posibles:
     *   - DPI ya tiene un ticket activo (EN_ESPERA o EN_ATENCION)
     *
     * @return CONFIRMACION si todo salió bien, ERROR con causa específica si falló
     */
    public synchronized Mensaje registrarPasajero(String dpi, String nombre, TipoAtencion tipo) {

        // Validar que no haya ticket activo para este DPI
        if (indicePorDpi.contiene(dpi)) {
            Pasajero existente = indicePorDpi.buscar(dpi);
            String estadoActual = existente != null ? existente.getEstado().name() : "desconocido";
            return Mensaje.error(
                "El pasajero con DPI " + dpi + " ya tiene un ticket activo " +
                "(estado: " + estadoActual + "). " +
                "Debe atenderse el ticket actual antes de generar uno nuevo."
            );
        }

        // Crear pasajero y asignar turno
        int numeroCola = siguienteNumero(tipo);
        Pasajero pasajero = new Pasajero(dpi, nombre, tipo);
        pasajero.setNumeroCola(numeroCola);

        // Agregar a la cola activa y al índice
        colaParaTipo(tipo).encolar(pasajero);
        indicePorDpi.insertar(dpi, pasajero);

        // Persistir en base de datos histórica
        RegistroVisita visita = new RegistroVisita(dpi, nombre, tipo, numeroCola);
        BaseDatos.getInstance().registrarVisita(visita);

        System.out.println("[GESTOR] Registrado: " + pasajero);
        return Mensaje.confirmacion(dpi, numeroCola);
    }

    /**
     * Llama al siguiente pasajero de la cola indicada.
     *
     * Errores posibles:
     *   - Cola vacía
     *
     * @return PASAJERO_LLAMADO con sus datos, o COLA_VACIA si no hay nadie esperando
     */
    public synchronized Mensaje llamarSiguiente(TipoAtencion tipo) {
        Cola<Pasajero> cola = colaParaTipo(tipo);

        if (cola.isEmpty()) {
            System.out.println("[GESTOR] Cola " + tipo + " vacía al intentar llamar siguiente.");
            return Mensaje.colaVacia(tipo);
        }

        Pasajero pasajero = cola.desencolar();
        pasajero.setEstado(EstadoPasajero.EN_ATENCION);
        // El pasajero permanece en indicePorDpi hasta que se finalice la atención

        // Marcar en la base de datos que fue llamado a ventanilla
        BaseDatos.getInstance().marcarLlamada(pasajero.getDpi());

        System.out.println("[GESTOR] Llamado a ventanilla: " + pasajero);
        return Mensaje.pasajeroLlamado(
            pasajero.getDpi(), pasajero.getNombre(), pasajero.getNumeroCola()
        );
    }

    /**
     * Finaliza la atención de un pasajero con los datos del operador.
     *
     * @param dpi              DPI del pasajero
     * @param vuelo            Vuelo confirmado (puede ser vacío)
     * @param observaciones    Notas del operador (puede ser vacío)
     * @param duracionSegundos Tiempo de atención en segundos (medido en el cliente)
     *
     * Errores posibles:
     *   - DPI no encontrado (ya fue atendido, nunca existió, o servidor reiniciado)
     *   - Pasajero no está EN_ATENCION (aún en espera — no debería ocurrir en uso normal)
     */
    public synchronized Mensaje finalizarAtencion(String dpi, String vuelo,
                                                   String observaciones, long duracionSegundos) {
        Pasajero pasajero = indicePorDpi.buscar(dpi);

        if (pasajero == null) {
            return Mensaje.error(
                "Pasajero con DPI " + dpi + " no encontrado en el sistema activo. " +
                "Posibles causas: ya fue atendido previamente, el servidor se reinició " +
                "y perdió el estado en memoria, o el DPI es incorrecto."
            );
        }

        if (pasajero.getEstado() != EstadoPasajero.EN_ATENCION) {
            return Mensaje.error(
                "El pasajero con DPI " + dpi + " no está en estado EN_ATENCION " +
                "(estado actual: " + pasajero.getEstado() + "). " +
                "Solo se puede finalizar una atención que haya sido llamada primero."
            );
        }

        pasajero.setEstado(EstadoPasajero.ATENDIDO);
        indicePorDpi.eliminar(dpi); // Ya no está activo — puede registrarse de nuevo

        // Persistir datos finales en la base de datos
        BaseDatos.getInstance().marcarFin(dpi, vuelo, observaciones, duracionSegundos);

        System.out.println("[GESTOR] Atención finalizada: " + pasajero
            + " | Vuelo: " + vuelo + " | Duración: " + duracionSegundos + "s");

        return Mensaje.confirmacion(dpi, pasajero.getNumeroCola());
    }

    // ── Consultas de estado ───────────────────────────────────────────────────

    public synchronized int totalEnCola(TipoAtencion tipo) {
        return colaParaTipo(tipo).size();
    }

    // ── Helpers internos ──────────────────────────────────────────────────────

    private Cola<Pasajero> colaParaTipo(TipoAtencion tipo) {
        return switch (tipo) {
            case GENERAL     -> colaGeneral;
            case PRIORITARIA -> colaPrioritaria;
            case ESPECIAL    -> colaEspecial;
        };
    }

    private int siguienteNumero(TipoAtencion tipo) {
        return switch (tipo) {
            case GENERAL     -> ++contadorGeneral;
            case PRIORITARIA -> ++contadorPrioritaria;
            case ESPECIAL    -> ++contadorEspecial;
        };
    }
}
