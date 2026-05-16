package com.aeropuerto.server;

import com.aeropuerto.common.TablaHash;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Capa de persistencia en disco de los registros de visitas.
 * Singleton — un solo archivo CSV por ejecucion del servidor.
 *
 * El archivo se escribe en el directorio de trabajo del servidor.
 * Nombre: registros_YYYYMMDD.csv
 *
 * No usa base de datos externa — solo I/O estandar de Java.
 */
public class BaseDatos {

    private static BaseDatos instancia;
    private static final String ARCHIVO = "registros_aeropuerto.csv";

    // Indice en memoria: dpi → RegistroVisita (solo sesion actual)
    private final TablaHash<String, RegistroVisita> registros;

    private final File archivoCSV;
    private boolean    encabezadoEscrito = false;

    private BaseDatos() {
        this.registros  = new TablaHash<>();
        this.archivoCSV = new File(ARCHIVO);
        verificarEncabezado();
        System.out.println("[DB] Base de datos en: " + archivoCSV.getAbsolutePath());
    }

    public static synchronized BaseDatos getInstance() {
        if (instancia == null) instancia = new BaseDatos();
        return instancia;
    }

    // ── API publica ───────────────────────────────────────────────────────────

    /** Registra una nueva visita al recibir el ticket. */
    public synchronized void registrarVisita(RegistroVisita visita) {
        registros.insertar(visita.getDpi(), visita);
        // No escribimos todavia — esperamos datos completos
    }

    /** Marca la hora en que el pasajero fue llamado a ventanilla. */
    public synchronized void marcarLlamada(String dpi) {
        RegistroVisita r = registros.buscar(dpi);
        if (r != null) r.marcarLlamada();
    }

    /** Marca la finalizacion de la atencion y persiste el registro completo. */
    public synchronized void marcarFin(String dpi, String vuelo,
                                        String observaciones, long duracionSeg) {
        RegistroVisita r = registros.buscar(dpi);
        if (r == null) {
            System.out.println("[DB] Advertencia: no se encontro registro para DPI " + dpi);
            return;
        }
        r.marcarFin(vuelo, observaciones, duracionSeg);
        registros.eliminar(dpi);
        escribirLinea(r.toCSV());
    }

    /** Devuelve todos los registros completados (en disco). */
    public synchronized String[][] leerTodos() {
        if (!archivoCSV.exists()) return new String[0][];
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(archivoCSV), StandardCharsets.UTF_8))) {
            String linea;
            java.util.List<String[]> filas = new java.util.ArrayList<>();
            boolean primera = true;
            while ((linea = br.readLine()) != null) {
                if (primera) { primera = false; continue; } // saltar cabecera
                if (!linea.isBlank()) filas.add(linea.split(";", -1));
            }
            return filas.toArray(new String[0][]);
        } catch (IOException e) {
            System.out.println("[DB] Error leyendo registros: " + e.getMessage());
            return new String[0][];
        }
    }

    // ── Internos ──────────────────────────────────────────────────────────────

    private void verificarEncabezado() {
        encabezadoEscrito = archivoCSV.exists() && archivoCSV.length() > 0;
        if (!encabezadoEscrito) {
            escribirLinea(RegistroVisita.csvHeader());
            encabezadoEscrito = true;
        }
    }

    private void escribirLinea(String linea) {
        try (PrintWriter pw = new PrintWriter(
                new OutputStreamWriter(
                    new FileOutputStream(archivoCSV, true), StandardCharsets.UTF_8))) {
            pw.println(linea);
        } catch (IOException e) {
            System.out.println("[DB] Error escribiendo en disco: " + e.getMessage());
        }
    }
}