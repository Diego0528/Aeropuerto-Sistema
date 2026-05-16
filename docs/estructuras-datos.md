# Estructuras de Datos

Ambas estructuras están implementadas desde cero en el módulo `common`, sin usar colecciones de Java (`ArrayList`, `LinkedList`, `HashMap`, etc.).

---

## Cola\<T\> — Lista enlazada simple

**Archivo:** `common/src/main/java/com/aeropuerto/common/Cola.java`

Implementa el comportamiento FIFO (First In, First Out) usando nodos con referencias manuales.

### Estructura interna

```
frente                                    fin
  │                                        │
  ▼                                        ▼
[Nodo: dato=p1, siguiente=►] → [Nodo: dato=p2, siguiente=►] → [Nodo: dato=p3, siguiente=null]
```

### Operaciones

| Método | Descripción | Complejidad |
|---|---|---|
| `encolar(T elemento)` | Agrega al final. Lanza `IllegalArgumentException` si el elemento es null | O(1) |
| `desencolar()` | Retira y retorna el frente. Retorna `null` si está vacía | O(1) |
| `peek()` | Consulta el frente sin retirarlo. Retorna `null` si está vacía | O(1) |
| `isEmpty()` | Retorna `true` si no hay elementos | O(1) |
| `size()` | Retorna la cantidad de elementos (mantiene contador) | O(1) |

### Uso en el servidor

```java
Cola<Pasajero> colaGeneral = new Cola<>();

// Registrar pasajero
colaGeneral.encolar(pasajero);

// Operador llama siguiente
Pasajero siguiente = colaGeneral.desencolar();
if (siguiente == null) {
    // cola vacía
}

// Consultar siguiente sin sacarlo
Pasajero cabeza = colaGeneral.peek();
```

---

## TablaHash\<K, V\> — Tabla hash con encadenamiento

**Archivo:** `common/src/main/java/com/aeropuerto/common/TablaHash.java`

Implementa un mapa clave→valor usando un arreglo de listas enlazadas (encadenamiento). Las colisiones se resuelven agregando el nuevo nodo al frente de la cadena en el mismo bucket.

### Estructura interna

```
tabla[0]: null
tabla[1]: [Nodo: "1234"|p1] → [Nodo: "9876"|p3] → null   ← colisión en bucket 1
tabla[2]: [Nodo: "5555"|p2] → null
tabla[3]: null
...
tabla[15]: null
```

### Función de hash

```java
private int indice(K clave) {
    return Math.abs(clave.hashCode() % tabla.length);
}
```

La capacidad por defecto es 16. Se puede especificar otra capacidad en el constructor (útil para pruebas de colisión con capacidad 1).

### Operaciones

| Método | Descripción | Complejidad |
|---|---|---|
| `insertar(K, V)` | Inserta o actualiza si la clave ya existe | O(1) promedio |
| `buscar(K)` | Retorna el valor o `null` si no existe | O(1) promedio |
| `eliminar(K)` | Elimina la entrada. Retorna `true` si existía | O(1) promedio |
| `contiene(K)` | Retorna `true` si la clave existe | O(1) promedio |
| `isEmpty()` | Retorna `true` si no hay entradas | O(1) |
| `size()` | Retorna el número de entradas | O(1) |

### Uso en el servidor

```java
TablaHash<String, Pasajero> indice = new TablaHash<>();

// Al registrar
indice.insertar(pasajero.getDpi(), pasajero);

// Verificar duplicado
if (indice.contiene(dpi)) { ... }

// Buscar pasajero
Pasajero p = indice.buscar(dpi);

// Al finalizar atención
indice.eliminar(dpi);
```

