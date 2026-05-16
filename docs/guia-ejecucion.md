# Guía de Ejecución

## Requisitos previos

- Java 21 (el proyecto incluye el JDK bundled de IntelliJ IDEA)
- Maven (incluido en IntelliJ IDEA)
- IntelliJ IDEA 2024.x (recomendado para abrir el proyecto)

> **Nota:** En este equipo, `java` y `mvn` no están en el PATH del sistema.  
> Rutas absolutas disponibles:
> - Java: `C:\Program Files\JetBrains\IntelliJ IDEA 2024.3.5\jbr\bin\java.exe`
> - Maven: `C:\Program Files\JetBrains\IntelliJ IDEA 2024.3.5\plugins\maven\lib\maven3\bin\mvn.cmd`

---

## Compilar el proyecto

Desde la raíz del proyecto (`Aeropuerto-Sistema/`):

```powershell
# PowerShell — configurar variables
$env:JAVA_HOME = "C:\Program Files\JetBrains\IntelliJ IDEA 2024.3.5\jbr"
$mvn = "C:\Program Files\JetBrains\IntelliJ IDEA 2024.3.5\plugins\maven\lib\maven3\bin\mvn.cmd"

# Compilar todo
& $mvn clean package

# Compilar en modo silencioso (sin output de Maven)
& $mvn clean package -q
```

El build genera JARs ejecutables (fat JARs con dependencias incluidas) en cada `target/`.

---

## Orden de inicio

El servidor **debe iniciarse antes** que cualquier cliente.

### 1. Iniciar el servidor

**Desde IntelliJ:** Ejecutar `server/src/main/java/com/aeropuerto/server/ServerMain.java`

**Desde terminal (PowerShell):**
```powershell
$java = "C:\Program Files\JetBrains\IntelliJ IDEA 2024.3.5\jbr\bin\java.exe"
& $java -jar server\target\server-1.0-SNAPSHOT.jar
# Puerto personalizado (por defecto 5000):
& $java -jar server\target\server-1.0-SNAPSHOT.jar 6000
```

**Desde IntelliJ con Maven:**
```powershell
& $mvn exec:java -pl server
```

El servidor muestra:
```
[SERVER] Servidor escuchando en puerto 5000
[SERVER] Esperando conexiones...
```

### 2. Iniciar el kiosko de registro

**Desde IntelliJ:** Ejecutar `client-registro/src/main/java/com/aeropuerto/registro/Launcher.java`

```powershell
& $mvn javafx:run -pl client-registro
```

### 3. Iniciar ventanillas de operadores

Se pueden iniciar una o más ventanillas simultáneamente (cada una en su propia ventana):

```powershell
# Cola General
& $mvn javafx:run -pl client-general

# Cola Prioritaria
& $mvn javafx:run -pl client-prioritaria

# Cola Especial / VIP
& $mvn javafx:run -pl client-especial
```

---

## Demo rápida (flujo completo)

1. Inicia el servidor
2. Abre el kiosko de registro
3. Ingresa un DPI de prueba (ej: `1234567890101`) y presiona Enter
4. El nombre se autocompleta desde RENAP: "Carlos García López"
5. Selecciona "General" y presiona "Registrar en Cola"
6. El sistema confirma: "Registrado. Turno #1 — Cola GENERAL"
7. Abre la ventanilla General (`client-general`)
8. Presiona "Llamar Siguiente" — aparece el turno y el nombre del pasajero
9. Selecciona el vuelo y escribe observaciones
10. Presiona "Finalizar Atención" — el turno se cierra

---

## Configuración de red

Por defecto todos los clientes se conectan a `localhost:5000`.

Para cambiar la IP/puerto (demo en red local), editar la constante en cada App:

```java
// En RegistroApp.java, GeneralApp.java, PrioritariaApp.java, EspecialApp.java
private static final String HOST   = "192.168.1.X"; // IP del servidor
private static final int    PUERTO = 5000;
```

---

## Estructura de JARs generados

```
server/target/server-1.0-SNAPSHOT.jar              ← ejecutable sin JavaFX
client-registro/target/client-registro-*.jar       ← fat JAR con JavaFX incluido
client-general/target/client-general-*.jar
client-prioritaria/target/client-prioritaria-*.jar
client-especial/target/client-especial-*.jar
```
