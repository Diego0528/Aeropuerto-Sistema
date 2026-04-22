package com.aeropuerto.common;

/**
 * Cola genérica implementada con lista enlazada simple.
 *
 * RESPONSABLE: Pablo Acan
 * RESTRICCIÓN: Sin ArrayList, LinkedList ni ninguna colección de Java.
 *              Solo nodos propios con referencias manuales.
 *
 * OPERACIONES REQUERIDAS:
 *   encolar(T)   → agrega al final        → O(1)
 *   desencolar() → retira del frente      → O(1)
 *   peek()       → consulta frente        → O(1)
 *   isEmpty()    → ¿está vacía?           → O(1)
 *   size()       → cantidad de elementos  → O(1)  ← mantener contador, NO recorrer
 *
 * CÓMO SE USA EN EL SERVIDOR (Diego):
 *   Cola<Pasajero> colaGeneral = new Cola<>();
 *   colaGeneral.encolar(pasajero);
 *   Pasajero siguiente = colaGeneral.desencolar();
 */
public class Cola<T> {

    // ── Nodo interno ─────────────────────────────────────────────────────────
    // Esta clase solo existe para Cola — por eso es privada y estática.
    private static class Nodo<T> {
        T dato;
        Nodo<T> siguiente;

        Nodo(T dato) {
            this.dato = dato;
            this.siguiente = null;
        }
    }

    // ── Atributos ─────────────────────────────────────────────────────────────
    private Nodo<T> frente;   // Primer elemento (el que se desencola)
    private Nodo<T> fin;      // Último elemento (donde se encola)
    private int tamanio;      // Contador — actualizar en encolar() y desencolar()

    // ── Constructor ──────────────────────────────────────────────────────────
    public Cola() {
        frente   = null;
        fin      = null;
        tamanio  = 0;
    }

    // ── Operaciones principales ───────────────────────────────────────────────

    /**
     * Agrega un elemento al final de la cola.
     * @param elemento El objeto a encolar. No puede ser null.
     */
    public void encolar(T elemento) {
        if (elemento == null)
            throw new IllegalArgumentException("No se puede encolar un elemento nulo");

        // TODO Pablo: implementar
        // Pista: crear un nuevo Nodo, enlazarlo al fin actual, actualizar fin y tamanio.
        // Caso especial: si la cola estaba vacía, frente también apunta al nuevo nodo.
        throw new UnsupportedOperationException("Cola.encolar() — pendiente de implementar");
    }

    /**
     * Retira y retorna el elemento del frente de la cola.
     * @return El elemento que estaba al frente.
     * @throws IllegalStateException si la cola está vacía.
     */
    public T desencolar() {
        if (isEmpty())
            throw new IllegalStateException("No se puede desencolar de una cola vacía");

        // TODO Pablo: implementar
        // Pista: guardar el dato del frente, avanzar frente al siguiente, decrementar tamanio.
        // Caso especial: si la cola queda vacía después, fin también debe quedar null.
        throw new UnsupportedOperationException("Cola.desencolar() — pendiente de implementar");
    }

    /**
     * Retorna el elemento del frente SIN retirarlo.
     * @return El elemento al frente.
     * @throws IllegalStateException si la cola está vacía.
     */
    public T peek() {
        if (isEmpty())
            throw new IllegalStateException("La cola está vacía");

        // TODO Pablo: implementar — es la más simple de todas
        throw new UnsupportedOperationException("Cola.peek() — pendiente de implementar");
    }

    /**
     * Indica si la cola no tiene elementos.
     */
    public boolean isEmpty() {
        // TODO Pablo: implementar — una línea con tamanio
        throw new UnsupportedOperationException("Cola.isEmpty() — pendiente de implementar");
    }

    /**
     * Retorna la cantidad de elementos en la cola.
     * O(1) porque mantenemos el contador tamanio.
     */
    public int size() {
        // TODO Pablo: implementar — una línea
        throw new UnsupportedOperationException("Cola.size() — pendiente de implementar");
    }

    @Override
    public String toString() {
        if (isEmpty()) return "Cola[]";

        StringBuilder sb = new StringBuilder("Cola[frente→");
        Nodo<T> actual = frente;
        while (actual != null) {
            sb.append(actual.dato);
            if (actual.siguiente != null) sb.append(", ");
            actual = actual.siguiente;
        }
        sb.append("←fin]");
        return sb.toString();
    }
}
