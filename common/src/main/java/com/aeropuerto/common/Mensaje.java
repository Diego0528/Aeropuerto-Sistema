package com.aeropuerto.common;

/**
 * Representa un mensaje del protocolo de comunicación socket.
 *
 * Formato en el cable (texto plano):
 *   TIPO|campo1|campo2|campo3\n
 *
 * Ejemplo real:
 *   REGISTRO|1234567890123|Juan Pérez|GENERAL\n
 *   CONFIRMACION|1234567890123|5\n
 *   ERROR|DPI ya registrado en el sistema\n
 *
 * USO:
 *   // Crear y serializar (para enviar por socket)
 *   Mensaje m = new Mensaje(TipoMensaje.REGISTRO, "123", "Juan", "GENERAL");
 *   PrintWriter.println(m.serializar());
 *
 *   // Deserializar (al recibir por socket)
 *   String linea = bufferedReader.readLine();
 *   Mensaje m = Mensaje.deserializar(linea);
 *   if (m.getTipo() == TipoMensaje.REGISTRO) { ... }
 */
public class Mensaje {

    private static final String SEPARADOR = "|";
    private static final String SEPARADOR_REGEX = "\\|"; // Para split()

    private final TipoMensaje tipo;
    private final String[] campos; // Campos adicionales según el tipo

    // ── Constructores ─────────────────────────────────────────────────────────

    /**
     * Crea un mensaje con tipo y campos variables.
     * @param tipo    El tipo de mensaje (TipoMensaje enum)
     * @param campos  Campos adicionales según el protocolo (pueden ser 0)
     */
    public Mensaje(TipoMensaje tipo, String... campos) {
        if (tipo == null)
            throw new IllegalArgumentException("El tipo de mensaje no puede ser nulo");
        this.tipo   = tipo;
        this.campos = campos != null ? campos : new String[0];
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public TipoMensaje getTipo() { return tipo; }

    /**
     * Retorna el campo en la posición indicada (base 0).
     * @throws IllegalArgumentException si el índice no existe
     */
    public String getCampo(int indice) {
        if (indice < 0 || indice >= campos.length)
            throw new IllegalArgumentException(
                "Índice " + indice + " fuera de rango. Este mensaje tiene " + campos.length + " campos.");
        return campos[indice];
    }

    public int getCantidadCampos() { return campos.length; }

    // ── Serialización ─────────────────────────────────────────────────────────

    /**
     * Convierte el mensaje a texto para enviarlo por socket.
     * El \n lo agrega PrintWriter.println() — NO lo incluimos aquí.
     *
     * Ejemplo resultado: "REGISTRO|1234567890123|Juan Pérez|GENERAL"
     */
    public String serializar() {
        if (campos.length == 0) return tipo.name();

        StringBuilder sb = new StringBuilder(tipo.name());
        for (String campo : campos) {
            sb.append(SEPARADOR).append(campo);
        }
        return sb.toString();
    }

    /**
     * Construye un Mensaje a partir de una línea de texto recibida por socket.
     *
     * @param linea  Texto recibido (sin \n). Ejemplo: "REGISTRO|123|Juan|GENERAL"
     * @return       El Mensaje correspondiente
     * @throws IllegalArgumentException si la línea es inválida o el tipo no existe
     */
    public static Mensaje deserializar(String linea) {
        if (linea == null || linea.isBlank())
            throw new IllegalArgumentException("No se puede deserializar una línea nula o vacía");

        String[] partes = linea.split(SEPARADOR_REGEX, -1);
        // partes[0] = tipo, partes[1..n] = campos

        TipoMensaje tipo;
        try {
            tipo = TipoMensaje.valueOf(partes[0]);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tipo de mensaje desconocido: '" + partes[0] + "'");
        }

        // Extraer campos (todo después del tipo)
        String[] campos = new String[partes.length - 1];
        System.arraycopy(partes, 1, campos, 0, campos.length);

        return new Mensaje(tipo, campos);
    }

    // ── Fábrica — mensajes comunes predefinidos ───────────────────────────────
    // Facilitan la creación sin recordar el orden de campos exacto.

    public static Mensaje registro(String dpi, String nombre, TipoAtencion tipo) {
        return new Mensaje(TipoMensaje.REGISTRO, dpi, nombre, tipo.name());
    }

    public static Mensaje confirmacion(String dpi, int numeroCola) {
        return new Mensaje(TipoMensaje.CONFIRMACION, dpi, String.valueOf(numeroCola));
    }

    public static Mensaje error(String mensajeError) {
        return new Mensaje(TipoMensaje.ERROR, mensajeError);
    }

    public static Mensaje llamarSiguiente(TipoAtencion tipo) {
        return new Mensaje(TipoMensaje.LLAMAR_SIGUIENTE, tipo.name());
    }

    public static Mensaje pasajeroLlamado(String dpi, String nombre, int numeroCola) {
        return new Mensaje(TipoMensaje.PASAJERO_LLAMADO, dpi, nombre, String.valueOf(numeroCola));
    }

    public static Mensaje colaVacia(TipoAtencion tipo) {
        return new Mensaje(TipoMensaje.COLA_VACIA, tipo.name());
    }

    public static Mensaje finAtencion(String dpi) {
        return new Mensaje(TipoMensaje.FIN_ATENCION, dpi);
    }

    public static Mensaje ping() { return new Mensaje(TipoMensaje.PING); }
    public static Mensaje pong() { return new Mensaje(TipoMensaje.PONG); }

    public static Mensaje identificar(String tipoCliente, String nombrePc) {
        return new Mensaje(TipoMensaje.IDENTIFICAR, tipoCliente, nombrePc);
    }

    public static Mensaje identificarOk() {
        return new Mensaje(TipoMensaje.IDENTIFICAR_OK);
    }

    /**
     * Crea un mensaje LOG_ENTRY para enviar al monitor.
     * El mensaje (último campo) puede contener '|' sin problema —
     * el receptor reconstruye uniendo campos 4+ con '|'.
     */
    public static Mensaje logEntry(LogEntry entry) {
        return new Mensaje(TipoMensaje.LOG_ENTRY,
                String.valueOf(entry.getId()),
                entry.getTimestamp(),
                entry.getNivel().name(),
                entry.getModulo(),
                entry.getMensaje());
    }

    /**
     * Crea un STATUS_UPDATE para notificar conexión/desconexión de un cliente.
     * accion = "CONECTADO" | "DESCONECTADO"
     */
    public static Mensaje statusUpdate(String accion, ClienteInfo info) {
        return new Mensaje(TipoMensaje.STATUS_UPDATE,
                accion,
                info.getIp(),
                String.valueOf(info.getPuerto()),
                info.getTipo().name(),
                info.getNombrePc(),
                info.getTimestamp());
    }

    // ── toString ──────────────────────────────────────────────────────────────

    @Override
    public String toString() { return "Mensaje{" + serializar() + "}"; }
}
