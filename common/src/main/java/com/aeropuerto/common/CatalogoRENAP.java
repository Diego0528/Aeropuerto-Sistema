package com.aeropuerto.common;

import java.util.HashMap;
import java.util.Map;

/**
 * Catálogo ficticio del Registro Nacional de Personas (RENAP).
 * Simula la consulta de datos por DPI para el kiosko de registro.
 */
public class CatalogoRENAP {

    public static class DatosPersona {
        public final String nombre;
        public final String fechaNacimiento;
        public final String genero;

        public DatosPersona(String nombre, String fechaNacimiento, String genero) {
            this.nombre = nombre;
            this.fechaNacimiento = fechaNacimiento;
            this.genero = genero;
        }
    }

    private static final Map<String, DatosPersona> REGISTRO = new HashMap<>();

    static {
        reg("1234567890101", "Carlos García López",        "1985-03-15", "M");
        reg("2345678901201", "María José Pérez Morales",   "1990-07-22", "F");
        reg("3456789012301", "Juan Pablo Méndez Castillo", "1975-11-08", "M");
        reg("4567890123401", "Ana Lucía Ramírez Solís",    "1998-02-28", "F");
        reg("5678901234501", "Pedro Antonio Santos Cruz",  "1962-09-14", "M");
        reg("6789012345601", "Sofía Isabel Morales Paz",   "2000-05-30", "F");
        reg("7890123456701", "Roberto Carlos Fuentes",     "1980-12-03", "M");
        reg("8901234567801", "Laura Beatriz González",     "1955-08-19", "F");
        reg("9012345678901", "Miguel Ángel Torres Lima",   "1993-04-11", "M");
        reg("0123456789001", "Elena Cristina Vargas",      "1970-06-25", "F");
        reg("1111111111111", "Diego Andrino González",     "1995-01-20", "M");
        reg("2222222222222", "Andrea Soto Castellanos",    "1988-06-15", "F");
        reg("3333333333333", "José Luis Girón Méndez",     "1950-09-30", "M");
        reg("4444444444444", "Carmen Alicia López",        "1948-03-12", "F");
        reg("5555555555555", "Francisco Ajú Caal",         "2005-11-25", "M");
    }

    private static void reg(String dpi, String nombre, String fecha, String genero) {
        REGISTRO.put(dpi, new DatosPersona(nombre, fecha, genero));
    }

    public static DatosPersona consultar(String dpi) {
        if (dpi == null) return null;
        return REGISTRO.get(dpi.trim());
    }

    public static boolean existe(String dpi) {
        if (dpi == null) return false;
        return REGISTRO.containsKey(dpi.trim());
    }
}
