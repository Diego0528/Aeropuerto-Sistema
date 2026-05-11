package com.aeropuerto.common;

/**
 * Pruebas manuales de Cola<T> y TablaHash.
 * Sin JUnit — cumple restricción de "sin librerías externas".
 */
public class TestEstructuras {

    static int pruebas  = 0;
    static int exitosas = 0;

    static void afirmar(String descripcion, boolean condicion) {
        pruebas++;
        if (condicion) {
            exitosas++;
            System.out.println("  OK  " + descripcion);
        } else {
            System.out.println("  FALLO: " + descripcion);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  TESTS DE Cola<T>
    // ══════════════════════════════════════════════════════════════════════════
    static void testCola() {
        System.out.println("\n-- Cola<String> ------------------------------------");
        Cola<String> cola = new Cola<>();

        afirmar("Nueva cola esta vacia",           cola.isEmpty());
        afirmar("Tamano inicial es 0",              cola.size() == 0);
        afirmar("peek() en vacia retorna null",    cola.peek() == null);
        afirmar("desencolar() en vacia retorna null", cola.desencolar() == null);

        cola.encolar("Alpha");
        cola.encolar("Beta");
        cola.encolar("Gamma");

        afirmar("Despues de 3 encolados, size=3",  cola.size() == 3);
        afirmar("peek() es 'Alpha' (FIFO)",        "Alpha".equals(cola.peek()));

        String primero = cola.desencolar();
        afirmar("Desencolar retorna 'Alpha'",      "Alpha".equals(primero));
        afirmar("Size baja a 2",                   cola.size() == 2);
        afirmar("Nuevo frente es 'Beta'",          "Beta".equals(cola.peek()));

        cola.desencolar();
        cola.desencolar();
        afirmar("Cola vacia tras sacar todos",     cola.isEmpty());
        afirmar("peek() en vacia vuelve a null",   cola.peek() == null);
    }

    static void testColaPasajeros() {
        System.out.println("\n-- Cola<Pasajero> ----------------------------------");
        Cola<Pasajero> cola = new Cola<>();

        Pasajero p1 = new Pasajero("1234567890101", "Ana Garcia",   TipoAtencion.GENERAL,    1);
        Pasajero p2 = new Pasajero("9876543210101", "Juan Perez",   TipoAtencion.PRIORITARIA, 2);
        Pasajero p3 = new Pasajero("1111111111111", "Maria Lopez",  TipoAtencion.ESPECIAL,    3);

        cola.encolar(p1);
        cola.encolar(p2);
        cola.encolar(p3);

        afirmar("3 pasajeros encolados",          cola.size() == 3);
        afirmar("Primero es Ana (FIFO)",          "Ana Garcia".equals(cola.desencolar().getNombre()));
        afirmar("Segundo es Juan",                "Juan Perez".equals(cola.desencolar().getNombre()));
        afirmar("Tercero es Maria",               "Maria Lopez".equals(cola.desencolar().getNombre()));
        afirmar("Cola vacia al terminar",         cola.isEmpty());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  TESTS DE TablaHash
    // ══════════════════════════════════════════════════════════════════════════
    static void testTablaHash() {
        System.out.println("\n-- TablaHash<String, Pasajero> --------------------");
        TablaHash<String, Pasajero> tabla = new TablaHash<>();

        Pasajero p1 = new Pasajero("1234567890101", "Ana Garcia",   TipoAtencion.GENERAL,    1);
        Pasajero p2 = new Pasajero("9876543210101", "Juan Perez",   TipoAtencion.PRIORITARIA, 2);
        Pasajero p3 = new Pasajero("5555555555555", "Maria Lopez",  TipoAtencion.ESPECIAL,    3);

        afirmar("Tabla nueva esta vacia",         tabla.isEmpty());
        afirmar("Size inicial es 0",              tabla.size() == 0);
        afirmar("buscar en vacia -> null",        tabla.buscar("0000000000000") == null);

        tabla.insertar(p1.getDpi(), p1);
        tabla.insertar(p2.getDpi(), p2);
        tabla.insertar(p3.getDpi(), p3);

        afirmar("Size es 3 tras insertar 3",      tabla.size() == 3);
        afirmar("buscar p1 retorna a Ana",        "Ana Garcia".equals(tabla.buscar(p1.getDpi()).getNombre()));
        afirmar("buscar p2 retorna a Juan",       "Juan Perez".equals(tabla.buscar(p2.getDpi()).getNombre()));
        afirmar("contiene(p3) -> true",           tabla.contiene(p3.getDpi()));
        afirmar("contiene('0000') -> false",      !tabla.contiene("0000000000000"));

        Pasajero p1Act = new Pasajero(p1.getDpi(), "Ana Garcia ACTUALIZADA", TipoAtencion.GENERAL, 1);
        tabla.insertar(p1.getDpi(), p1Act);
        afirmar("Reinsertar misma clave actualiza (no duplica)", tabla.size() == 3);
        afirmar("buscar muestra nombre actualizado",
                "Ana Garcia ACTUALIZADA".equals(tabla.buscar(p1.getDpi()).getNombre()));

        boolean eliminado = tabla.eliminar(p2.getDpi());
        afirmar("eliminar p2 retorna true",       eliminado);
        afirmar("Size baja a 2",                  tabla.size() == 2);
        afirmar("buscar p2 tras eliminar -> null", tabla.buscar(p2.getDpi()) == null);

        boolean noExistia = tabla.eliminar("9999999999999");
        afirmar("eliminar DPI inexistente -> false", !noExistia);
    }

    static void testTablaHashColisiones() {
        System.out.println("\n-- TablaHash con colisiones forzadas (capacidad=1) -");
        TablaHash<String, Pasajero> tabla = new TablaHash<>(1);

        Pasajero[] pasajeros = {
            new Pasajero("1111111111111", "Pasajero A", TipoAtencion.GENERAL,    1),
            new Pasajero("2222222222222", "Pasajero B", TipoAtencion.PRIORITARIA, 2),
            new Pasajero("3333333333333", "Pasajero C", TipoAtencion.ESPECIAL,    3),
        };

        for (Pasajero p : pasajeros) tabla.insertar(p.getDpi(), p);

        afirmar("Size correcto con colisiones totales", tabla.size() == 3);
        afirmar("buscar A funciona con colisiones",    tabla.buscar("1111111111111") != null);
        afirmar("buscar B funciona con colisiones",    tabla.buscar("2222222222222") != null);
        afirmar("buscar C funciona con colisiones",    tabla.buscar("3333333333333") != null);

        tabla.eliminar("2222222222222");
        afirmar("eliminar en medio de cadena",         tabla.buscar("2222222222222") == null);
        afirmar("A sigue accesible",                   tabla.buscar("1111111111111") != null);
        afirmar("C sigue accesible",                   tabla.buscar("3333333333333") != null);
    }

    static void testEstadoPasajero() {
        System.out.println("\n-- EstadoPasajero ----------------------------------");
        Pasajero p = new Pasajero("1234567890101", "Carlos", TipoAtencion.GENERAL, 5);

        afirmar("Estado inicial es EN_ESPERA",    p.getEstado() == EstadoPasajero.EN_ESPERA);
        afirmar("NumeroCola asignado es 5",       p.getNumeroCola() == 5);

        p.setEstado(EstadoPasajero.EN_ATENCION);
        afirmar("Estado cambia a EN_ATENCION",    p.getEstado() == EstadoPasajero.EN_ATENCION);

        p.setEstado(EstadoPasajero.ATENDIDO);
        afirmar("Estado cambia a ATENDIDO",       p.getEstado() == EstadoPasajero.ATENDIDO);

        afirmar("DPI correcto",                   "1234567890101".equals(p.getDpi()));
        afirmar("Nombre correcto",                "Carlos".equals(p.getNombre()));
        afirmar("Tipo correcto",                  p.getTipo() == TipoAtencion.GENERAL);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  MAIN
    // ══════════════════════════════════════════════════════════════════════════
    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("  Tests Estructuras de Datos - Aeropuerto Sistema ");
        System.out.println("=================================================");

        testCola();
        testColaPasajeros();
        testTablaHash();
        testTablaHashColisiones();
        testEstadoPasajero();

        System.out.println("\n=================================================");
        System.out.printf("  Resultado: %d/%d pruebas exitosas%n", exitosas, pruebas);
        if (exitosas == pruebas) {
            System.out.println("  TODAS LAS PRUEBAS PASARON");
        } else {
            System.out.println("  HAY FALLOS -- revisar arriba");
        }
        System.out.println("=================================================");
    }
}
