package com.aeropuerto.common;

/**
 * Representa a un pasajero dentro del sistema de colas.
 *
 * Es el objeto central que viaja entre server y clientes (como texto
 * dentro de Mensaje, no como objeto serializado).
 *
 * Immutable en DPI y nombre — solo el estado y número de cola cambian.
 */
public class Pasajero {

    // ── Campos ───────────────────────────────────────────────────────────────
    private final String dpi;          // Identificador único. NUNCA cambia.
    private final String nombre;       // Nombre completo del pasajero
    private final TipoAtencion tipo;   // A qué cola pertenece
    private EstadoPasajero estado;     // Cambia durante el ciclo de vida
    private int numeroCola;            // Número asignado al encolarse (1, 2, 3...)

    // ── Constructores ─────────────────────────────────────────────────────────
    public Pasajero(String dpi, String nombre, TipoAtencion tipo) {
        if (dpi == null || dpi.isBlank())
            throw new IllegalArgumentException("El DPI no puede ser nulo o vacío");
        if (nombre == null || nombre.isBlank())
            throw new IllegalArgumentException("El nombre no puede ser nulo o vacío");
        if (tipo == null)
            throw new IllegalArgumentException("El tipo de atención no puede ser nulo");

        this.dpi        = dpi.trim();
        this.nombre     = nombre.trim();
        this.tipo       = tipo;
        this.estado     = EstadoPasajero.EN_ESPERA;
        this.numeroCola = 0;
    }

    public Pasajero(String dpi, String nombre, TipoAtencion tipo, int numeroCola) {
        this(dpi, nombre, tipo);
        if (numeroCola >= 1) this.numeroCola = numeroCola;
    }

    // ── Getters ──────────────────────────────────────────────────────────────
    public String getDpi()             { return dpi; }
    public String getNombre()          { return nombre; }
    public TipoAtencion getTipo()      { return tipo; }
    public EstadoPasajero getEstado()  { return estado; }
    public int getNumeroCola()         { return numeroCola; }

    // ── Setters (solo los campos que cambian) ────────────────────────────────
    public void setEstado(EstadoPasajero estado) {
        if (estado == null)
            throw new IllegalArgumentException("El estado no puede ser nulo");
        this.estado = estado;
    }

    public void setNumeroCola(int numeroCola) {
        if (numeroCola < 1)
            throw new IllegalArgumentException("El número de cola debe ser mayor a 0");
        this.numeroCola = numeroCola;
    }

    // ── Utilidades ───────────────────────────────────────────────────────────
    @Override
    public String toString() {
        return "Pasajero{dpi='" + dpi + "', nombre='" + nombre +
               "', tipo=" + tipo + ", estado=" + estado +
               ", numeroCola=" + numeroCola + "}";
    }

    @Override
    public boolean equals(Object o) {
        // Dos pasajeros son iguales si tienen el mismo DPI — nada más importa
        if (this == o) return true;
        if (!(o instanceof Pasajero)) return false;
        return dpi.equals(((Pasajero) o).dpi);
    }

    @Override
    public int hashCode() {
        return dpi.hashCode();
    }
}
