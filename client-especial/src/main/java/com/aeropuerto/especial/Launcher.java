package com.aeropuerto.especial;

/**
 * Clase de arranque necesaria cuando JavaFX está en el classpath (sin módulos).
 * Si el main() estuviera directamente en EspecialApp (que extiende Application),
 * la JVM fallaría al intentar cargar la clase antes de inicializar JavaFX.
 * Este Launcher intermedio evita ese problema.
 *
 * NO modificar esta clase.
 */
public class Launcher {
    public static void main(String[] args) {
        EspecialApp.main(args);
    }
}
