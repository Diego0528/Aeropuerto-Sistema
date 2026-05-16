package com.aeropuerto.common;

import java.io.*;
import java.util.Properties;

/**
 * Configuración del servidor leída desde config.txt.
 *
 * Busca config.txt en este orden:
 *   1. Directorio de trabajo (donde se lanzó la JVM)
 *   2. Directorio donde está el JAR
 *
 * Formato de config.txt:
 *   ip=192.168.1.100
 *   puerto=5000
 *   puerto_chat=5001
 */
public class ConfigServidor {

    private static ConfigServidor instancia;

    private String host       = "localhost";
    private int    puerto     = 5000;
    private int    puertoChat = 5001;

    private ConfigServidor() {
        cargarDesdeArchivo();
    }

    public static synchronized ConfigServidor getInstance() {
        if (instancia == null) instancia = new ConfigServidor();
        return instancia;
    }

    private void cargarDesdeArchivo() {
        String[] rutas = {
            "config.txt",
            System.getProperty("user.dir") + File.separator + "config.txt",
            directorioJar() + File.separator + "config.txt"
        };

        for (String ruta : rutas) {
            File f = new File(ruta);
            if (f.exists() && f.isFile()) {
                try (InputStream is = new FileInputStream(f)) {
                    Properties p = new Properties();
                    p.load(is);

                    if (p.containsKey("ip"))
                        host = p.getProperty("ip").trim();
                    if (p.containsKey("puerto"))
                        try { puerto = Integer.parseInt(p.getProperty("puerto").trim()); }
                        catch (NumberFormatException ignored) {}
                    if (p.containsKey("puerto_chat"))
                        try { puertoChat = Integer.parseInt(p.getProperty("puerto_chat").trim()); }
                        catch (NumberFormatException ignored) {}

                    System.out.println("[CONFIG] ip=" + host
                        + "  puerto=" + puerto + "  chat=" + puertoChat
                        + "  ← " + f.getAbsolutePath());
                    return;
                } catch (IOException e) {
                    System.out.println("[CONFIG] Error leyendo " + ruta + ": " + e.getMessage());
                }
            }
        }
        System.out.println("[CONFIG] config.txt no encontrado — usando ip="
            + host + " puerto=" + puerto);
    }

    private static String directorioJar() {
        try {
            return new File(
                ConfigServidor.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()
            ).getParentFile().getAbsolutePath();
        } catch (Exception e) {
            return ".";
        }
    }

    public String getHost()       { return host; }
    public int    getPuerto()     { return puerto; }
    public int    getPuertoChat() { return puertoChat; }

    /** Escribe un config.txt de plantilla en el directorio actual si no existe. */
    public static void crearPlantillaSiNoExiste() {
        File f = new File("config.txt");
        if (f.exists()) return;
        try (PrintWriter w = new PrintWriter(f, "UTF-8")) {
            w.println("# ── Configuracion AeroQueue ──────────────────────────");
            w.println("# Editar con cualquier editor de texto (Bloc de notas, etc.)");
            w.println();
            w.println("# IP de la PC donde corre el servidor");
            w.println("ip=192.168.1.100");
            w.println();
            w.println("# Puerto principal del sistema de colas");
            w.println("puerto=5000");
            w.println();
            w.println("# Puerto del chat interno entre modulos");
            w.println("puerto_chat=5001");
            System.out.println("[CONFIG] config.txt creado en: " + f.getAbsolutePath());
        } catch (IOException e) {
            System.out.println("[CONFIG] No se pudo crear config.txt: " + e.getMessage());
        }
    }
}