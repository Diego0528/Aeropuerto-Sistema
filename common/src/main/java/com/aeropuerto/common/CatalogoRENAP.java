package com.aeropuerto.common;

import java.util.HashMap;
import java.util.Map;

/**
 Catálogo ficticio del Registro Nacional de Personas .
 Simula la consulta de datos por DPI para el registro de cada ventanilla.
 */
public class CatalogoRENAP {

    public static class DatosPersona {
        public final String nombre;
        public final String fechaNacimiento;
        public final String genero; // "M" o "F"

        public DatosPersona(String nombre, String fechaNacimiento, String genero) {
            this.nombre          = nombre;
            this.fechaNacimiento = fechaNacimiento;
            this.genero          = genero;
        }
    }

    private static final Map<String, DatosPersona> REGISTRO = new HashMap<>();

    static {
        // ── Adultos mayores (más de 60 años) → cola prioritaria ──
        reg("1234567890101", "Carlos García López",           "1955-03-15", "M");
        reg("2345678901201", "María José Pérez Morales",      "1958-07-22", "F");
        reg("3456789012301", "Juan Pablo Méndez Castillo",    "1950-11-08", "M");
        reg("4567890123401", "Ana Lucía Ramírez Solís",       "1948-02-28", "F");
        reg("5678901234501", "Pedro Antonio Santos Cruz",     "1962-09-14", "M");
        reg("6789012345601", "Sofía Isabel Morales Paz",      "1945-05-30", "F");
        reg("7890123456701", "Roberto Carlos Fuentes Lima",   "1952-12-03", "M");
        reg("8901234567801", "Laura Beatriz González Díaz",   "1955-08-19", "F");
        reg("9012345678901", "Miguel Ángel Torres Lima",      "1940-04-11", "M");
        reg("0123456789001", "Elena Cristina Vargas Pérez",   "1959-06-25", "F");

        // ── Adultos jóvenes → cola general ──
        reg("1111111111111", "Diego Andrino González",        "1995-01-20", "M");
        reg("2222222222222", "Andrea Soto Castellanos",       "1988-06-15", "F");
        reg("3333333333333", "José Luis Girón Méndez",        "1990-09-30", "M");
        reg("4444444444444", "Carmen Alicia López Ruiz",      "1992-03-12", "F");
        reg("5555555555555", "Francisco Ajú Caal",            "2000-11-25", "M");
        reg("6666666666666", "Gabriela Hernández Morales",    "1997-04-18", "F");
        reg("7777777777777", "Luis Fernando Castillo",        "1985-08-07", "M");
        reg("8888888888888", "Valentina Ramos Estrada",       "2001-12-14", "F");
        reg("9999999999999", "Fernando Alejos Recinos",       "1993-02-09", "M");
        reg("1010101010101", "Claudia Patricia Méndez",       "1987-07-03", "F");
        reg("1212121212121", "Héctor Mauricio Pérez",         "1991-10-21", "M");
        reg("1313131313131", "Daniela Sofía Ruiz López",      "1999-05-16", "F");
        reg("1414141414141", "Pablo Andrés Morán Cifuentes",  "1983-03-28", "M");
        reg("1515151515151", "Ingrid Paola Gómez Ajú",        "1996-09-11", "F");
        reg("1616161616161", "Rodrigo Estuardo Lima",         "2002-01-05", "M");

        // ── Menores de edad ──
        reg("1717171717171", "Sofía Alejandra Pérez",         "2010-06-20", "F");
        reg("1818181818181", "Diego Sebastián Morales",       "2012-11-03", "M");
        reg("1919191919191", "Isabella Fernanda López",       "2015-04-17", "F");
        reg("2020202020202", "Mateo Alejandro García",        "2008-09-25", "M");
    }

    private static void reg(String dpi, String nombre, String fecha, String genero) {
        REGISTRO.put(dpi, new DatosPersona(nombre, fecha, genero));
    }

    /**
     * Agrega una nueva persona al registro.
     * Retorna un mensaje de error si el DPI ya existe.
     * Retorna null si se agregó correctamente.
     */
    public static String agregar(String dpi, String nombre, String fechaNacimiento, String genero) {
        if (dpi == null || dpi.trim().isEmpty()) {
            return "Error: el DPI no puede estar vacío.";
        }
        if (REGISTRO.containsKey(dpi.trim())) {
            return "Error: el DPI " + dpi.trim() + " ya está registrado en el sistema.";
        }
        REGISTRO.put(dpi.trim(), new DatosPersona(nombre, fechaNacimiento, genero));
        return null; // null significa que se agregó correctamente
    }

    /**
     * Consulta los datos de una persona por DPI.
     * Retorna null si no existe en el registro.
     */
    public static DatosPersona consultar(String dpi) {
        if (dpi == null) return null;
        return REGISTRO.get(dpi.trim());
    }

    /**
     * Verifica si un DPI existe en el registro.
     */
    public static boolean existe(String dpi) {
        if (dpi == null) return false;
        return REGISTRO.containsKey(dpi.trim());
    }

    /**
     * Retorna el total de personas registradas.
     */
    public static int totalRegistrados() {
        return REGISTRO.size();
    }
}