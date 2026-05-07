package com.aeropuerto.common;

/**
 * Cola genérica implementada con lista enlazada simple.
 *
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


    // ── Constructor ──────────────────────────────────────────────────────────
    public Cola() {
    }

    // ── Operaciones principales ───────────────────────────────────────────────

    /**
     */
    }

    /**
     */
    }

    /**
     */
    }

    /**
     */
    }

    /**
     */
    }

        Nodo<T> actual = frente;
        while (actual != null) {
            if (actual.siguiente != null) sb.append(", ");
            actual = actual.siguiente;
        }
        return sb.toString();
    }
}