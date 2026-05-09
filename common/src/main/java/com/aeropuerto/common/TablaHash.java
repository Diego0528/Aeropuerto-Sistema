package com.aeropuerto.common;

/**
 * Tabla hash genérica con encadenamiento para resolución de colisiones.
 *
 * Uso en el servidor:
 *   TablaHash<String, Pasajero> indice = new TablaHash<>();
 *   indice.insertar(dpi, pasajero);
 *   Pasajero p = indice.buscar(dpi);
 */
public class TablaHash<K, V> {

    private static final int CAPACIDAD_DEFECTO = 16;

    private static class Nodo<K, V> {
        final K clave;
        V valor;
        Nodo<K, V> siguiente;

        Nodo(K clave, V valor) {
            this.clave = clave;
            this.valor = valor;
        }
    }

    private final Nodo<K, V>[] tabla;
    private int tamanio;

    @SuppressWarnings("unchecked")
    public TablaHash() {
        tabla = new Nodo[CAPACIDAD_DEFECTO];
    }

    @SuppressWarnings("unchecked")
    public TablaHash(int capacidad) {
        if (capacidad < 1)
            throw new IllegalArgumentException("La capacidad debe ser mayor a 0");
        tabla = new Nodo[capacidad];
    }

    private int indice(K clave) {
        return Math.abs(clave.hashCode() % tabla.length);
    }

    public void insertar(K clave, V valor) {
        if (clave == null) throw new IllegalArgumentException("La clave no puede ser nula");
        int i = indice(clave);
        Nodo<K, V> nodo = tabla[i];
        while (nodo != null) {
            if (nodo.clave.equals(clave)) {
                nodo.valor = valor;
                return;
            }
            nodo = nodo.siguiente;
        }
        Nodo<K, V> nuevo = new Nodo<>(clave, valor);
        nuevo.siguiente = tabla[i];
        tabla[i] = nuevo;
        tamanio++;
    }

    public V buscar(K clave) {
        if (clave == null) return null;
        int i = indice(clave);
        Nodo<K, V> nodo = tabla[i];
        while (nodo != null) {
            if (nodo.clave.equals(clave)) return nodo.valor;
            nodo = nodo.siguiente;
        }
        return null;
    }

    public boolean eliminar(K clave) {
        if (clave == null) return false;
        int i = indice(clave);
        Nodo<K, V> nodo = tabla[i];
        Nodo<K, V> anterior = null;
        while (nodo != null) {
            if (nodo.clave.equals(clave)) {
                if (anterior == null) tabla[i] = nodo.siguiente;
                else anterior.siguiente = nodo.siguiente;
                tamanio--;
                return true;
            }
            anterior = nodo;
            nodo = nodo.siguiente;
        }
        return false;
    }

    public boolean contiene(K clave) {
        return buscar(clave) != null;
    }

    public boolean isEmpty() {
        return tamanio == 0;
    }

    public int size() {
        return tamanio;
    }
}
