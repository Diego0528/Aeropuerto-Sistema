package com.aeropuerto.common;

/**
 * Cola genérica implementada con lista enlazada simple.
 *
 * RESPONSABLE: Pablo Acan
 * RESTRICCIÓN: Sin ArrayList, LinkedList ni ninguna colección de Java.
 *              Solo nodos propios con referencias manuales.
 *
 * OPERACIONES:
 *   encolar(T)   → agrega al final        → O(1)
 *   desencolar() → retira del frente      → O(1), retorna null si vacía
 *   peek()       → consulta frente        → O(1), retorna null si vacía
 *   isEmpty()    → ¿está vacía?           → O(1)
 *   size()       → cantidad de elementos  → O(1)
 */
public class Cola<T> {

    private static class Nodo<T> {
        T dato;
        Nodo<T> siguiente;

        Nodo(T dato) {
            this.dato = dato;
            this.siguiente = null;
        }
    }

    private Nodo<T> frente;
    private Nodo<T> fin;
    private int tamanio;

    public Cola() {
        frente  = null;
        fin     = null;
        tamanio = 0;
    }

    public void encolar(T elemento) {
        if (elemento == null)
            throw new IllegalArgumentException("No se puede encolar un elemento nulo");

        Nodo<T> nuevo = new Nodo<>(elemento);
        if (fin == null) {
            frente = nuevo;
        } else {
            fin.siguiente = nuevo;
        }
        fin = nuevo;
        tamanio++;
    }

    /** Retira y retorna el frente, o null si la cola está vacía. */
    public T desencolar() {
        if (isEmpty()) return null;
        T dato = frente.dato;
        frente = frente.siguiente;
        if (frente == null) fin = null;
        tamanio--;
        return dato;
    }

    /** Retorna el frente sin retirarlo, o null si la cola está vacía. */
    public T peek() {
        if (isEmpty()) return null;
        return frente.dato;
    }

    public boolean isEmpty() {
        return tamanio == 0;
    }

    public int size() {
        return tamanio;
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
