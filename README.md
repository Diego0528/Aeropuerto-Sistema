# Sistema de Colas — Aeropuerto Internacional La Aurora

Sistema de gestión de turnos para aeropuerto, implementado con arquitectura cliente-servidor sobre sockets TCP/IP. Desarrollado en Java 21 con interfaz gráfica JavaFX.

**Equipo:** Diego Andrino (servidor / integración) · Pablo Acan (estructuras de datos / client-registro)  
**Entrega:** 10 de mayo

---

## Descripción general

Los pasajeros se registran en un kiosko ingresando su DPI (el sistema consulta el catálogo RENAP ficticio y autocompleta el nombre). Reciben un número de turno según el tipo de cola elegido. Los operadores en ventanilla atienden cada cola de forma ordenada y registran la información específica de cada tipo de pasajero.

### Tres tipos de cola

| Cola | Pasajeros | Datos que registra el operador |
|---|---|---|
| **General** | Público en general | Vuelo + observaciones |
| **Prioritaria** | Adultos mayores, embarazadas, discapacidad | Vuelo + checklist de asistencia requerida |
| **Especial / VIP** | Pasajeros VIP | Vuelo + clase + numero de asiento + beneficios activables |

---

## Flujo de trabajo

```
1. Pasajero ingresa DPI en kiosko
2. Sistema consulta CatalogoRENAP → autocompleta nombre
3. Pasajero selecciona tipo de cola → obtiene numero de turno
4. Operador presiona "Llamar Siguiente" en su ventanilla
5. Sistema muestra nombre y turno del pasajero
6. Operador completa formulario (vuelo, asistencia, beneficios)
7. Operador presiona "Finalizar Atencion" → turno cerrado
```

---

## Tecnologias

- **Java 21**
- **JavaFX 21.0.2** — interfaz grafica de clientes
- **Maven 3** — build multi-modulo
- **TCP/IP sockets** — comunicacion cliente-servidor (puerto 5000 por defecto)
- Sin librerias externas en `common` — estructuras de datos implementadas desde cero

---

## Estructura del proyecto

```
Aeropuerto-Sistema/
├── pom.xml                          # POM padre
├── common/                          # Codigo compartido
│   └── .../com/aeropuerto/common/
│       ├── Cola.java                # Cola generica (lista enlazada simple)
│       ├── TablaHash.java           # Tabla hash con encadenamiento
│       ├── Pasajero.java            # DTO del pasajero
│       ├── Mensaje.java             # Protocolo de comunicacion
│       ├── TipoMensaje.java         # Enum tipos de mensaje
│       ├── TipoAtencion.java        # Enum GENERAL / PRIORITARIA / ESPECIAL
│       ├── EstadoPasajero.java      # Enum EN_ESPERA / EN_ATENCION / ATENDIDO
│       ├── CatalogoRENAP.java       # 15 personas ficticias para demos
│       └── TestEstructuras.java     # Pruebas manuales sin JUnit
├── server/                          # Servidor central
│   └── .../com/aeropuerto/server/
│       ├── ServerMain.java          # Acepta conexiones TCP en puerto 5000
│       ├── ClientHandler.java       # Un hilo por cliente conectado
│       └── GestorColas.java         # Singleton thread-safe con las tres colas
├── client-registro/                 # Kiosko de pasajeros
├── client-general/                  # Ventanilla cola General
├── client-prioritaria/              # Ventanilla cola Prioritaria
├── client-especial/                 # Ventanilla cola Especial/VIP
└── docs/                            # Documentacion tecnica
    ├── arquitectura.md
    ├── protocolo.md
    ├── flujo-trabajo.md
    ├── estructuras-datos.md
    └── guia-ejecucion.md
```

---

## Protocolo de mensajes (TCP, texto plano)

Formato: `TIPO|campo1|campo2\n`

```
# Registro
C→S:  REGISTRO|1234567890101|Carlos Garcia|GENERAL
S→C:  CONFIRMACION|1234567890101|7

# Operador llama siguiente
C→S:  LLAMAR_SIGUIENTE|GENERAL
S→C:  PASAJERO_LLAMADO|1234567890101|Carlos Garcia|7

# Finalizar atencion
C→S:  FIN_ATENCION|1234567890101
S→C:  CONFIRMACION|1234567890101|7
```

Ver [`docs/protocolo.md`](docs/protocolo.md) para la especificacion completa.

---

## Compilar y ejecutar

### Compilar todo

```bash
mvn clean package
```

### Orden de inicio

```bash
# 1. Primero el servidor
java -jar server/target/server-1.0-SNAPSHOT.jar

# 2. Kiosko de pasajeros
mvn javafx:run -pl client-registro

# 3. Una o mas ventanillas de operador
mvn javafx:run -pl client-general
mvn javafx:run -pl client-prioritaria
mvn javafx:run -pl client-especial
```

Ver [`docs/guia-ejecucion.md`](docs/guia-ejecucion.md) para instrucciones detalladas.

---

## DPIs de prueba (RENAP ficticio)

| DPI | Nombre |
|---|---|
| `1234567890101` | Carlos Garcia Lopez |
| `2345678901201` | Maria Jose Perez Morales |
| `4567890123401` | Ana Lucia Ramirez Solis |
| `1111111111111` | Diego Andrino Gonzalez |
| `2222222222222` | Andrea Soto Castellanos |
| `3333333333333` | Jose Luis Giron Mendez |
| `4444444444444` | Carmen Alicia Lopez (adulta mayor) |

---

## Documentacion tecnica

| Documento | Contenido |
|---|---|
| [`docs/arquitectura.md`](docs/arquitectura.md) | Diagrama de modulos, capas y thread model |
| [`docs/protocolo.md`](docs/protocolo.md) | Especificacion del protocolo TCP/IP |
| [`docs/flujo-trabajo.md`](docs/flujo-trabajo.md) | Flujo completo paso a paso con diagramas |
| [`docs/estructuras-datos.md`](docs/estructuras-datos.md) | Cola y TablaHash — API, complejidad, ejemplos |
| [`docs/guia-ejecucion.md`](docs/guia-ejecucion.md) | Compilar, configurar IP y ejecutar |

---

## Restriccion del proyecto

> Los modulos `Cola<T>` y `TablaHash<K,V>` deben implementarse desde cero en `common`.  
> No se permite usar `ArrayList`, `LinkedList`, `HashMap` ni ninguna coleccion de Java.

---

## Paquetes Java

| Modulo | Paquete base |
|---|---|
| common | `com.aeropuerto.common` |
| server | `com.aeropuerto.server` |
| client-registro | `com.aeropuerto.registro` |
| client-general | `com.aeropuerto.general` |
| client-prioritaria | `com.aeropuerto.prioritaria` |
| client-especial | `com.aeropuerto.especial` |

---

## Workflow de Git

```
main     ← versiones estables (merge desde develop)
develop  ← integracion continua
```
