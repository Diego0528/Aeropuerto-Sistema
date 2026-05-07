package com.aeropuerto.common;

// ════════════════════════════════════════════════════════════════════════════
// TipoAtencion.java  — enum con los tres tipos de cola
// ════════════════════════════════════════════════════════════════════════════
// (Poner en archivo separado TipoAtencion.java, aquí está todo junto
//  para facilitar la revisión. Separar antes de compilar si el IDE lo pide.)

/**
 * Tipos de cola disponibles en el aeropuerto.
 */
/*
public enum TipoAtencion {
    GENERAL,      // Cola estándar para cualquier pasajero
    PRIORITARIA,  // Adultos mayores, embarazadas, personas con discapacidad
    ESPECIAL      // VIP u otros criterios definidos por el aeropuerto
}
*/

// ════════════════════════════════════════════════════════════════════════════
// EstadoPasajero.java  — ciclo de vida de un pasajero en el sistema
// ════════════════════════════════════════════════════════════════════════════
/*
public enum EstadoPasajero {
    EN_ESPERA,    // Registrado y en la cola, aún no llamado
    EN_ATENCION,  // Fue llamado por el operador, se está atendiendo
    ATENDIDO      // Proceso completado
}
*/

// ════════════════════════════════════════════════════════════════════════════
// Pasajero.java — DTO principal del sistema
// ════════════════════════════════════════════════════════════════════════════

/**
 * Representa a un pasajero registrado en el sistema.
 * Es el objeto almacenado tanto en las colas como en la tabla hash.
 *
 * DPI siempre se trata como String (puede contener guiones u otros
 * caracteres según el formato guatemalteco).
 *
 * @author Pablo Acan
 */
public class Pasajero {

    // ── Atributos ────────────────────────────────────────────────────────────
    private final String       dpi;           // Clave única — inmutable
    private final String       nombre;
    private final TipoAtencion tipoAtencion;
    private       EstadoPasajero estado;
    private final int           numeroCola;   // Número correlativo asignado al registrarse

    // ── Constructor ──────────────────────────────────────────────────────────
    public Pasajero(String dpi, String nombre, TipoAtencion tipoAtencion, int numeroCola) {
        this.dpi          = dpi.trim();
        this.nombre       = nombre.trim();
        this.tipoAtencion = tipoAtencion;
        this.numeroCola   = numeroCola;
        this.estado       = EstadoPasajero.EN_ESPERA;
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public String        getDpi()          { return dpi; }
    public String        getNombre()       { return nombre; }
    public TipoAtencion  getTipoAtencion() { return tipoAtencion; }
    public EstadoPasajero getEstado()      { return estado; }
    public int           getNumeroCola()  { return numeroCola; }

    // ── Setter de estado (único campo mutable) ────────────────────────────────
    public synchronized void setEstado(EstadoPasajero nuevoEstado) {
        this.estado = nuevoEstado;
    }

    // ── Serialización para el protocolo de mensajes ──────────────────────────
    /**
     * Convierte el pasajero en una cadena compatible con el protocolo:
     * "dpi|nombre|tipoAtencion|estado|numeroCola"
     */
    public String serializar() {
        return dpi + "|" + nombre + "|" + tipoAtencion.name()
                + "|" + estado.name() + "|" + numeroCola;
    }

    /**
     * Reconstruye un Pasajero desde el formato serializado.
     * Útil en el cliente cuando recibe ESTADO_COLA del servidor.
     */
    public static Pasajero deserializar(String cadena) {
        String[] partes = cadena.split("\\|");
        // partes[0]=dpi  [1]=nombre  [2]=tipo  [3]=estado  [4]=numeroCola
        Pasajero p = new Pasajero(
                partes[0],
                partes[1],
                TipoAtencion.valueOf(partes[2]),
                Integer.parseInt(partes[4])
        );
        p.setEstado(EstadoPasajero.valueOf(partes[3]));
        return p;
    }

    @Override
    public String toString() {
        return "Pasajero{dpi='" + dpi + "', nombre='" + nombre
                + "', tipo=" + tipoAtencion
                + ", estado=" + estado
                + ", #" + numeroCola + "}";
    }
}