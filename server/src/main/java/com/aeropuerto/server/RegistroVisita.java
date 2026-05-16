package com.aeropuerto.server;

import com.aeropuerto.common.TipoAtencion;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Registro inmutable de una visita de pasajero al sistema.
 * Se almacena en BaseDatos al registrar, llamar y finalizar la atencion.
 */
public class RegistroVisita {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String      dpi;
    private final String      nombre;
    private final TipoAtencion tipo;
    private final int         numeroCola;
    private final String      horaRegistro;

    // Campos que se actualizan despues del registro inicial
    private String horaLlamada  = "";
    private String horaFin      = "";
    private String vuelo        = "";
    private String observaciones = "";
    private long   duracionSeg  = 0;

    public RegistroVisita(String dpi, String nombre, TipoAtencion tipo, int numeroCola) {
        this.dpi          = dpi;
        this.nombre       = nombre;
        this.tipo         = tipo;
        this.numeroCola   = numeroCola;
        this.horaRegistro = LocalDateTime.now().format(FMT);
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String      getDpi()           { return dpi; }
    public String      getNombre()        { return nombre; }
    public TipoAtencion getTipo()         { return tipo; }
    public int         getNumeroCola()    { return numeroCola; }
    public String      getHoraRegistro()  { return horaRegistro; }
    public String      getHoraLlamada()   { return horaLlamada; }
    public String      getHoraFin()       { return horaFin; }
    public String      getVuelo()         { return vuelo; }
    public String      getObservaciones() { return observaciones; }
    public long        getDuracionSeg()   { return duracionSeg; }

    // ── Actualizaciones ───────────────────────────────────────────────────────

    public void marcarLlamada() {
        this.horaLlamada = LocalDateTime.now().format(FMT);
    }

    public void marcarFin(String vuelo, String observaciones, long duracionSeg) {
        this.horaFin       = LocalDateTime.now().format(FMT);
        this.vuelo         = vuelo         != null ? vuelo         : "";
        this.observaciones = observaciones != null ? observaciones : "";
        this.duracionSeg   = duracionSeg;
    }

    /** Serializa a una linea CSV para almacenamiento en disco. */
    public String toCSV() {
        return String.join(";",
            dpi, esc(nombre), tipo.name(), String.valueOf(numeroCola),
            horaRegistro, horaLlamada, horaFin,
            esc(vuelo), esc(observaciones), String.valueOf(duracionSeg)
        );
    }

    /** Cabecera CSV para el archivo de registros. */
    public static String csvHeader() {
        return "dpi;nombre;tipo;numero_cola;hora_registro;hora_llamada;hora_fin;vuelo;observaciones;duracion_seg";
    }

    /** Escapa punto y coma en campos de texto libre. */
    private static String esc(String s) {
        return s == null ? "" : s.replace(";", ",");
    }

    @Override
    public String toString() {
        return "RegistroVisita{dpi=" + dpi + ", nombre=" + nombre
            + ", tipo=" + tipo + ", turno=" + numeroCola + "}";
    }
}
