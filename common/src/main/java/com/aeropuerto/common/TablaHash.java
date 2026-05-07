package com.aeropuerto.common;

/**
 * Tabla hash implementada con arreglo de listas enlazadas (chaining).
 * La clave es el DPI del pasajero (String). El valor es el objeto Pasajero.
 * Thread-safe: todos los métodos están sincronizados.
 *
 * Función de hash: polinomio con base 31 módulo capacidad (número primo).
 * Capacidad inicial: 101 → minimiza colisiones para un volumen razonable de DPIs.
 *
 * Complejidades (caso promedio):
 *   insertar  → O(1)
 *   buscar    → O(1)
 *   eliminar  → O(1)
 *
 * @author Pablo Acan
 */
public class TablaHash {

    // ── Nodo de la lista de chaining ─────────────────────────────────────────
    private static class Entrada {
        String   clave;     // DPI
        Pasajero valor;
        Entrada  siguiente;

        Entrada(String clave, Pasajero valor) {
            this.clave     = clave;
            this.valor     = valor;
            this.siguiente = null;
        }
    }

    // ── Atributos ────────────────────────────────────────────────────────────
    private static final int CAPACIDAD_INICIAL = 101; // Número primo

    private final Entrada[] tabla;
    private final int       capacidad;
    private int             tamanio;    // Cantidad de pares almacenados

    // ── Constructor ──────────────────────────────────────────────────────────
    public TablaHash() {
        this(CAPACIDAD_INICIAL);
    }

    public TablaHash(int capacidad) {
        this.capacidad = capacidad;
        this.tabla     = new Entrada[capacidad];
        this.tamanio   = 0;
    }

    // ── Función de hash ───────────────────────────────────────────────────────

    /**
     * Convierte un DPI (String) en un índice válido del arreglo.
     * Usa un polinomio con base 31 (el mismo que usa Java internamente
     * para String.hashCode(), seguro y con buena distribución).
     */
    private int hash(String dpi) {
        if (dpi == null) return 0;
        int h = 0;
        for (char c : dpi.toCharArray()) {
            h = (h * 31 + c) % capacidad;
        }
        return Math.abs(h);
    }

    // ── Operaciones principales ───────────────────────────────────────────────

    /**
     * Inserta un pasajero con su DPI como clave. O(1) promedio.
     * Si el DPI ya existe, actualiza el valor (no duplica).
     *
     * @param dpi     Clave única del pasajero
     * @param pasajero Objeto a almacenar
     */
    public synchronized void insertar(String dpi, Pasajero pasajero) {
        int indice = hash(dpi);
        Entrada actual = tabla[indice];

        // Recorrer la cadena buscando si ya existe esa clave
        while (actual != null) {
            if (actual.clave.equals(dpi)) {
                // Clave duplicada → solo actualiza el valor
                actual.valor = pasajero;
                return;
            }
            actual = actual.siguiente;
        }

        // No existe → insertar al frente de la cadena (O(1))
        Entrada nueva = new Entrada(dpi, pasajero);
        nueva.siguiente = tabla[indice];
        tabla[indice]   = nueva;
        tamanio++;
    }

    /**
     * Busca un pasajero por su DPI. O(1) promedio.
     *
     * @param dpi Clave a buscar
     * @return El Pasajero asociado, o null si no existe
     */
    public synchronized Pasajero buscar(String dpi) {
        int indice = hash(dpi);
        Entrada actual = tabla[indice];

        while (actual != null) {
            if (actual.clave.equals(dpi)) {
                return actual.valor;
            }
            actual = actual.siguiente;
        }
        return null; // No encontrado
    }

    /**
     * Verifica si un DPI ya está registrado. O(1) promedio.
     */
    public synchronized boolean contiene(String dpi) {
        return buscar(dpi) != null;
    }

    /**
     * Elimina el pasajero con el DPI dado. O(1) promedio.
     *
     * @param dpi Clave a eliminar
     * @return true si se eliminó, false si no existía
     */
    public synchronized boolean eliminar(String dpi) {
        int indice = hash(dpi);
        Entrada actual   = tabla[indice];
        Entrada anterior = null;

        while (actual != null) {
            if (actual.clave.equals(dpi)) {
                if (anterior == null) {
                    // Es el primero de la cadena
                    tabla[indice] = actual.siguiente;
                } else {
                    anterior.siguiente = actual.siguiente;
                }
                tamanio--;
                return true;
            }
            anterior = actual;
            actual   = actual.siguiente;
        }
        return false; // No existía
    }

    // ── Utilidades ────────────────────────────────────────────────────────────

    /** Cantidad de pasajeros registrados en la tabla. */
    public synchronized int size() {
        return tamanio;
    }

    public synchronized boolean isEmpty() {
        return tamanio == 0;
    }

    /**
     * Factor de carga actual (útil para detectar degradación de rendimiento).
     * Si supera 0.75, la tabla empieza a tener muchas colisiones.
     */
    public synchronized double factorCarga() {
        return (double) tamanio / capacidad;
    }

    /**
     * Resumen de ocupación para depuración.
     * Muestra cuántos buckets tienen al menos un elemento.
     */
    @Override
    public synchronized String toString() {
        int bucketUsados = 0;
        int maxCadena    = 0;

        for (Entrada e : tabla) {
            if (e != null) {
                bucketUsados++;
                int longCadena = 0;
                Entrada nodo = e;
                while (nodo != null) { longCadena++; nodo = nodo.siguiente; }
                if (longCadena > maxCadena) maxCadena = longCadena;
            }
        }
        return String.format(
                "TablaHash[capacidad=%d, elementos=%d, buckets_usados=%d, cadena_max=%d, factor_carga=%.2f]",
                capacidad, tamanio, bucketUsados, maxCadena, factorCarga()
        );
    }
}