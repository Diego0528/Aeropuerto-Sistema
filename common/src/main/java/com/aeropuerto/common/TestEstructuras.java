package com.aeropuerto.common;

/**
 * Pruebas manuales de Cola<T> y TablaHash.
 * Ejecutar con: javac *.java && java com.aeropuerto.common.TestEstructuras
 *
 * No usa JUnit para cumplir la restricción de "sin librerías externas".
 *
 * @author Pablo Acan
 */
public class TestEstructuras {

    static int pruebas  = 0;
    static int exitosas = 0;

    // ── Aserción simple ───────────────────────────────────────────────────────
    static void afirmar(String descripcion, boolean condicion) {
        pruebas++;
        if (condicion) {
            exitosas++;
            System.out.println("  ✓ " + descripcion);
        } else {
            System.out.println("  ✗ FALLO: " + descripcion);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  TESTS DE Cola<T>
    // ════════════════════════════════════════════════════════════════════════
    static void testCola() {
        System.out.println("\n── Cola<String> ────────────────────────────");
        Cola<String> cola = new Cola<>();

        afirmar("Nueva cola está vacía",        cola.isEmpty());
        afirmar("Tamaño inicial es 0",           cola.size() == 0);
        afirmar("peek() en vacía retorna null",  cola.peek() == null);
        afirmar("desencolar() en vacía → null",  cola.desencolar() == null);

        cola.encolar("Alpha");
        cola.encolar("Beta");
        cola.encolar("Gamma");

        afirmar("Después de 3 encolados, size=3",  cola.size() == 3);
        afirmar("peek() es 'Alpha' (FIFO)",         "Alpha".equals(cola.peek()));

        String primero = cola.desencolar();
        afirmar("Desencolar retorna 'Alpha'",        "Alpha".equals(primero));
        afirmar("Size baja a 2",                     cola.size() == 2);
        afirmar("Nuevo frente es 'Beta'",            "Beta".equals(cola.peek()));

        cola.desencolar(); // saca Beta
        cola.desencolar(); // saca Gamma
        afirmar("Cola vacía tras sacar todos",       cola.isEmpty());
        afirmar("peek() en vacía vuelve a ser null", cola.peek() == null);
    }

    static void testColaPasajeros() {
        System.out.println("\n── Cola<Pasajero> ──────────────────────────");
        Cola<Pasajero> cola = new Cola<>();

        Pasajero p1 = new Pasajero("1234567890101", "Ana García",   TipoAtencion.GENERAL,    1);
        Pasajero p2 = new Pasajero("9876543210101", "Juan Pérez",   TipoAtencion.PRIORITARIA, 2);
        Pasajero p3 = new Pasajero("1111111111111", "María López",  TipoAtencion.ESPECIAL,    3);

        cola.encolar(p1);
        cola.encolar(p2);
        cola.encolar(p3);

        afirmar("3 pasajeros encolados",                   cola.size() == 3);
        afirmar("Primero es Ana (FIFO)",                   "Ana García".equals(cola.desencolar().getNombre()));
        afirmar("Segundo es Juan",                         "Juan Pérez".equals(cola.desencolar().getNombre()));
        afirmar("Tercero es María",                        "María López".equals(cola.desencolar().getNombre()));
        afirmar("Cola vacía al terminar",                  cola.isEmpty());
    }

    // ════════════════════════════════════════════════════════════════════════
    //  TESTS DE TablaHash
    // ════════════════════════════════════════════════════════════════════════
    static void testTablaHash() {
        System.out.println("\n── TablaHash ───────────────────────────────");
        TablaHash tabla = new TablaHash();

        Pasajero p1 = new Pasajero("1234567890101", "Ana García",   TipoAtencion.GENERAL,    1);
        Pasajero p2 = new Pasajero("9876543210101", "Juan Pérez",   TipoAtencion.PRIORITARIA, 2);
        Pasajero p3 = new Pasajero("5555555555555", "María López",  TipoAtencion.ESPECIAL,    3);

        afirmar("Tabla nueva está vacía",          tabla.isEmpty());
        afirmar("Size inicial es 0",               tabla.size() == 0);
        afirmar("buscar en vacía → null",          tabla.buscar("0000000000000") == null);

        tabla.insertar(p1.getDpi(), p1);
        tabla.insertar(p2.getDpi(), p2);
        tabla.insertar(p3.getDpi(), p3);

        afirmar("Size es 3 tras insertar 3",       tabla.size() == 3);
        afirmar("buscar p1 retorna a Ana",         "Ana García".equals(tabla.buscar(p1.getDpi()).getNombre()));
        afirmar("buscar p2 retorna a Juan",        "Juan Pérez".equals(tabla.buscar(p2.getDpi()).getNombre()));
        afirmar("contiene(p3) → true",             tabla.contiene(p3.getDpi()));
        afirmar("contiene('0000') → false",        !tabla.contiene("0000000000000"));

        // Actualizar (reinsertar misma clave)
        Pasajero p1Actualizado = new Pasajero(p1.getDpi(), "Ana García ACTUALIZADA", TipoAtencion.GENERAL, 1);
        tabla.insertar(p1.getDpi(), p1Actualizado);
        afirmar("Reinsertar clave existente actualiza (no duplica)", tabla.size() == 3);
        afirmar("buscar muestra nombre actualizado",
                "Ana García ACTUALIZADA".equals(tabla.buscar(p1.getDpi()).getNombre()));

        // Eliminar
        boolean eliminado = tabla.eliminar(p2.getDpi());
        afirmar("eliminar p2 retorna true",        eliminado);
        afirmar("Size baja a 2",                   tabla.size() == 2);
        afirmar("buscar p2 tras eliminar → null",  tabla.buscar(p2.getDpi()) == null);

        boolean noExistia = tabla.eliminar("9999999999999");
        afirmar("eliminar DPI inexistente → false", !noExistia);
    }

    static void testTablaHashColisiones() {
        System.out.println("\n── TablaHash con colisiones forzadas ───────");
        // Usar capacidad 1 fuerza que TODO vaya al mismo bucket → máximas colisiones
        TablaHash tabla = new TablaHash(1);

        Pasajero[] pasajeros = {
                new Pasajero("1111111111111", "Pasajero A", TipoAtencion.GENERAL,    1),
                new Pasajero("2222222222222", "Pasajero B", TipoAtencion.PRIORITARIA, 2),
                new Pasajero("3333333333333", "Pasajero C", TipoAtencion.ESPECIAL,    3),
        };

        for (Pasajero p : pasajeros) tabla.insertar(p.getDpi(), p);

        afirmar("Size correcto con colisiones totales", tabla.size() == 3);
        afirmar("buscar A funciona con colisiones",     tabla.buscar("1111111111111") != null);
        afirmar("buscar B funciona con colisiones",     tabla.buscar("2222222222222") != null);
        afirmar("buscar C funciona con colisiones",     tabla.buscar("3333333333333") != null);

        tabla.eliminar("2222222222222");
        afirmar("eliminar en medio de cadena",          tabla.buscar("2222222222222") == null);
        afirmar("A sigue accesible",                    tabla.buscar("1111111111111") != null);
        afirmar("C sigue accesible",                    tabla.buscar("3333333333333") != null);
    }

    static void testEstadoPasajero() {
        System.out.println("\n── Estado del Pasajero ─────────────────────");
        Pasajero p = new Pasajero("1234567890101", "Carlos", TipoAtencion.GENERAL, 5);

        afirmar("Estado inicial es EN_ESPERA",    p.getEstado() == EstadoPasajero.EN_ESPERA);
        p.setEstado(EstadoPasajero.EN_ATENCION);
        afirmar("Estado cambia a EN_ATENCION",    p.getEstado() == EstadoPasajero.EN_ATENCION);
        p.setEstado(EstadoPasajero.ATENDIDO);
        afirmar("Estado cambia a ATENDIDO",       p.getEstado() == EstadoPasajero.ATENDIDO);

        // Serialización / deserialización
        String serial = p.serializar();
        Pasajero reconstruido = Pasajero.deserializar(serial);
        afirmar("DPI preservado tras serializar",    p.getDpi().equals(reconstruido.getDpi()));
        afirmar("Nombre preservado",                 p.getNombre().equals(reconstruido.getNombre()));
        afirmar("Estado preservado",                 p.getEstado() == reconstruido.getEstado());
        afirmar("NumeroCola preservado",             p.getNumeroCola() == reconstruido.getNumeroCola());
    }

    // ════════════════════════════════════════════════════════════════════════
    //  MAIN
    // ════════════════════════════════════════════════════════════════════════
    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║  Tests de Estructuras de Datos - Aeropuerto  ║");
        System.out.println("╚══════════════════════════════════════════════╝");

        testCola();
        testColaPasajeros();
        testTablaHash();
        testTablaHashColisiones();
        testEstadoPasajero();

        System.out.println("\n══════════════════════════════════════════════");
        System.out.printf("  Resultado: %d/%d pruebas exitosas%n", exitosas, pruebas);
        if (exitosas == pruebas) {
            System.out.println("  ✓ TODAS LAS PRUEBAS PASARON");
        } else {
            System.out.println("  ✗ HAY FALLOS — revisar arriba");
        }
        System.out.println("══════════════════════════════════════════════");
    }
}