package com.aeropuerto.common;

/**
 * Cola genérica implementada con lista enlazada simple.
 * Thread-safe: todos los métodos están sincronizados para
 * soportar acceso concurrente desde múltiples hilos del servidor.
 *
 * @param <T> Tipo de elemento almacenado en la cola
 * @author Pablo Acan
 */
public class Cola<T> {

    // ── Nodo interno ────────────────────────────────────────────────────────
    private static class Nodo<T> {
        T dato;
        Nodo<T> siguiente;

        Nodo(T dato) {
            this.dato = dato;
            this.siguiente = null;
        }
    }

    // ── Atributos ────────────────────────────────────────────────────────────
    private Nodo<T> frente;   // Primer elemento (próximo a salir)
    private Nodo<T> fin;      // Último elemento (último en entrar)
    private int tamanio;      // Contador de elementos

    // ── Constructor ──────────────────────────────────────────────────────────
    public Cola() {
        this.frente  = null;
        this.fin     = null;
        this.tamanio = 0;
    }

    // ── Operaciones principales ───────────────────────────────────────────────

    /**
     * Agrega un elemento al final de la cola. O(1)
     * @param elemento Elemento a encolar
     */
    public synchronized void encolar(T elemento) {
        Nodo<T> nuevo = new Nodo<>(elemento);
        if (fin == null) {
            // Cola vacía: frente y fin apuntan al único nodo
            frente = nuevo;
            fin    = nuevo;
        } else {
            fin.siguiente = nuevo;
            fin = nuevo;
        }
        tamanio++;
    }

    /**
     * Retira y retorna el elemento del frente. O(1)
     * @return Elemento del frente, o null si la cola está vacía
     */
    public synchronized T desencolar() {
        if (frente == null) {
            return null;
        }
        T dato = frente.dato;
        frente = frente.siguiente;
        if (frente == null) {
            // La cola quedó vacía
            fin = null;
        }
        tamanio--;
        return dato;
    }

    /**
     * Consulta el frente sin retirarlo. O(1)
     * @return Elemento del frente, o null si la cola está vacía
     */
    public synchronized T peek() {
        return (frente != null) ? frente.dato : null;
    }

    /**
     * Verifica si la cola está vacía. O(1)
     */
    public synchronized boolean isEmpty() {
        return frente == null;
    }

    /**
     * Retorna el número de elementos en la cola. O(1)
     */
    public synchronized int size() {
        return tamanio;
    }

    /**
     * Retorna una representación en texto de todos los elementos
     * (útil para enviar ESTADO_COLA al cliente).
     * Formato: "elemento1,elemento2,elemento3"
     */
    public synchronized String toStringLineal() {
        if (frente == null) return "";
        StringBuilder sb = new StringBuilder();
        Nodo<T> actual = frente;
        while (actual != null) {
            sb.append(actual.dato.toString());
            if (actual.siguiente != null) sb.append(",");
            actual = actual.siguiente;
        }
        return sb.toString();
    }

    @Override
    public synchronized String toString() {
        return "Cola[tamanio=" + tamanio + ", elementos=[" + toStringLineal() + "]]";
    }
}