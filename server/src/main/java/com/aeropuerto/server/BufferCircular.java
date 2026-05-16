package com.aeropuerto.server;

/**
 * Buffer circular genérico (ring buffer) de capacidad fija.
 *
 * Cuando está lleno, el elemento más antiguo se sobreescribe automáticamente.
 * Todas las operaciones son O(1). Thread-safe mediante synchronized.
 *
 * Uso en este proyecto: almacenar los últimos N logs en memoria sin crecer indefinidamente.
 */
public class BufferCircular<T> {

    private final Object[] datos;
    private final int      capacidad;
    private int            inicio = 0;  // índice del elemento más antiguo
    private int            tamaño = 0;

    public BufferCircular(int capacidad) {
        if (capacidad < 1) throw new IllegalArgumentException("Capacidad debe ser > 0");
        this.capacidad = capacidad;
        this.datos     = new Object[capacidad];
    }

    /** Agrega un elemento. Si el buffer está lleno, sobreescribe el más antiguo. O(1). */
    public synchronized void agregar(T elemento) {
        int destino = (inicio + tamaño) % capacidad;
        datos[destino] = elemento;
        if (tamaño < capacidad) {
            tamaño++;
        } else {
            // Buffer lleno: avanzar inicio para descartar el más antiguo
            inicio = (inicio + 1) % capacidad;
        }
    }

    /**
     * Retorna todos los elementos en orden cronológico (más antiguo primero).
     * El caller debe hacer cast: (T) obtenerTodos()[i]
     */
    public synchronized Object[] obtenerTodos() {
        Object[] resultado = new Object[tamaño];
        for (int i = 0; i < tamaño; i++) {
            resultado[i] = datos[(inicio + i) % capacidad];
        }
        return resultado;
    }

    /** Cantidad de elementos actualmente almacenados. */
    public synchronized int tamaño() { return tamaño; }

    /** Capacidad máxima del buffer. */
    public int capacidad() { return capacidad; }
}
