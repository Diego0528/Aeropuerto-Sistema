package com.aeropuerto.common;

/**
 * Tabla hash con encadenamiento (chaining) para búsqueda de pasajeros por DPI.
 *
 * RESPONSABLE: Pablo Acan
 * RESTRICCIÓN: Sin HashMap, Hashtable ni ninguna colección de Java.
 *              Solo arreglo de nodos con referencias manuales.
 *
 * OPERACIONES REQUERIDAS:
 *   insertar(dpi, pasajero) → O(1) promedio
 *   buscar(dpi)             → O(1) promedio — retorna Pasajero o null
 *   eliminar(dpi)           → O(1) promedio
 *   contiene(dpi)           → O(1) promedio
 *
 * CÓMO SE USA EN EL SERVIDOR (Diego):
 *   TablaHash tabla = new TablaHash();
 *   tabla.insertar(pasajero.getDpi(), pasajero);
 *   Pasajero p = tabla.buscar("1234567890123");  // null si no existe
 *   tabla.eliminar("1234567890123");
 *
 * VENTAJA COMPETITIVA:
 *   Otros grupos probablemente recorren la cola entera para buscar un pasajero → O(n).
 *   Con esta tabla, la búsqueda es O(1) promedio. Eso se nota en la demo.
 */
public class TablaHash {

    // ── Nodo de la cadena ────────────────────────────────────────────────────
    private static class Nodo {
        String clave;       // DPI del pasajero
        Pasajero valor;     // El pasajero en sí
        Nodo siguiente;     // Siguiente en la cadena (para colisiones)

        Nodo(String clave, Pasajero valor) {
            this.clave     = clave;
            this.valor     = valor;
            this.siguiente = null;
        }
    }

    // ── Constantes ────────────────────────────────────────────────────────────
    /**
     * Capacidad inicial. Número primo para reducir colisiones.
     * 101 es más que suficiente para una demo — pocos pasajeros simultáneos.
     */
    private static final int CAPACIDAD_INICIAL = 101;

    // ── Atributos ─────────────────────────────────────────────────────────────
    private final Nodo[] tabla;    // Arreglo de cabezas de cadena
    private int tamanio;           // Cantidad de pares insertados

    // ── Constructor ──────────────────────────────────────────────────────────
    public TablaHash() {
        tabla   = new Nodo[CAPACIDAD_INICIAL];
        tamanio = 0;
        // Java inicializa el arreglo con null — no hace falta un loop
    }

    // ── Función de hash ──────────────────────────────────────────────────────

    /**
     * Convierte un DPI (String) en un índice válido para el arreglo.
     *
     * Algoritmo: acumula caracteres con multiplicador primo 31.
     * Mismo principio que String.hashCode() de Java, pero propio.
     *
     * @param clave El DPI del pasajero
     * @return      Índice entre 0 y CAPACIDAD_INICIAL - 1
     */
    private int hash(String clave) {
        int h = 0;
        for (char c : clave.toCharArray()) {
            h = (h * 31 + c) % CAPACIDAD_INICIAL;
        }
        return Math.abs(h); // Math.abs por si acaso hay overflow negativo
    }

    // ── Operaciones principales ───────────────────────────────────────────────

    /**
     * Inserta un pasajero indexado por su DPI.
     * Si el DPI ya existe, actualiza el valor (no duplica).
     *
     * @param dpi      Clave de búsqueda
     * @param pasajero El objeto a almacenar
     */
    public void insertar(String dpi, Pasajero pasajero) {
        if (dpi == null || pasajero == null)
            throw new IllegalArgumentException("Clave y valor no pueden ser nulos");

        // TODO Pablo: implementar
        // Pista:
        //   1. Calcular índice: int i = hash(dpi)
        //   2. Recorrer la cadena en tabla[i] buscando si ya existe esa clave
        //   3. Si existe → actualizar valor (no crear nodo nuevo)
        //   4. Si no existe → crear nuevo Nodo y ponerlo al inicio de la cadena
        //      (insertar al inicio es O(1) — no al final)
        //   5. Incrementar tamanio solo si fue inserción nueva
        throw new UnsupportedOperationException("TablaHash.insertar() — pendiente de implementar");
    }

    /**
     * Busca un pasajero por DPI.
     *
     * @param dpi El DPI a buscar
     * @return    El Pasajero si existe, null si no está en la tabla
     */
    public Pasajero buscar(String dpi) {
        if (dpi == null) return null;

        // TODO Pablo: implementar
        // Pista:
        //   1. Calcular índice: int i = hash(dpi)
        //   2. Recorrer la cadena en tabla[i]
        //   3. Comparar cada nodo.clave con dpi usando .equals() — NO ==
        //   4. Si encuentra → retornar nodo.valor
        //   5. Si llega al final sin encontrar → retornar null
        throw new UnsupportedOperationException("TablaHash.buscar() — pendiente de implementar");
    }

    /**
     * Elimina un pasajero de la tabla.
     *
     * @param dpi El DPI del pasajero a eliminar
     * @return    true si fue eliminado, false si no existía
     */
    public boolean eliminar(String dpi) {
        if (dpi == null) return false;

        // TODO Pablo: implementar
        // Pista — esto es lo más difícil de la tabla hash:
        //   1. Calcular índice: int i = hash(dpi)
        //   2. Recorrer la cadena con DOS referencias: anterior y actual
        //      - anterior empieza en null
        //      - actual empieza en tabla[i]
        //   3. Cuando encuentras el nodo con la clave:
        //      - Si anterior == null: el nodo es la cabeza → tabla[i] = actual.siguiente
        //      - Si anterior != null: anterior.siguiente = actual.siguiente
        //   4. Decrementar tamanio, retornar true
        //   5. Si llega al final sin encontrar → retornar false
        throw new UnsupportedOperationException("TablaHash.eliminar() — pendiente de implementar");
    }

    /**
     * Verifica si existe un pasajero con ese DPI.
     */
    public boolean contiene(String dpi) {
        // TODO Pablo: una línea usando buscar()
        throw new UnsupportedOperationException("TablaHash.contiene() — pendiente de implementar");
    }

    /**
     * Retorna la cantidad de pasajeros almacenados.
     */
    public int size() { return tamanio; }

    /**
     * Indica si la tabla no tiene ningún pasajero.
     */
    public boolean isEmpty() { return tamanio == 0; }
}
