# Airport Queue System — Aeropuerto Guatemala

Sistema de gestión de colas para aeropuerto. Proyecto universitario de Estructuras de Datos.

**Equipo:** Diego Andrino (servidor/integración) · Pablo Acan (estructuras de datos) · Antonio (documentación)  
**Entrega:** 10 de mayo

---

## Requisitos previos

| Herramienta | Versión | Verificar con |
|---|---|---|
| Java JDK | 21 | `java -version` |
| Maven | 3.9+ | `mvn -version` |
| IntelliJ IDEA | Cualquier edición | — |

---

## Configurar en IntelliJ desde cero

1. **File → Open** → seleccionar la carpeta raíz `airport-queue/` (la que contiene el `pom.xml` padre)
2. IntelliJ detecta automáticamente que es un proyecto Maven multi-módulo
3. Si pregunta "Trust project?" → **Trust**
4. Esperar a que termine la indexación y descarga de dependencias (barra de progreso abajo)
5. Verificar que en **Maven** (panel derecho) aparezcan los 6 módulos

---

## Compilar todo (verificar que no hay errores)

```bash
# Desde la carpeta raíz airport-queue/
mvn clean install
```

Debe terminar con `BUILD SUCCESS` sin errores de compilación.

---

## Correr módulos individuales

```bash
# Servidor (Java puro)
mvn exec:java -pl server -Dexec.mainClass=com.aeropuerto.server.ServerMain

# Clientes JavaFX (reemplazar xxx por el módulo deseado)
mvn javafx:run -pl client-registro
mvn javafx:run -pl client-general
mvn javafx:run -pl client-prioritaria
mvn javafx:run -pl client-especial
```

> **Nota:** Para correr los clientes es necesario haber ejecutado primero
> `mvn install -pl common` (o `mvn install` completo) para que `common.jar`
> esté disponible en el repositorio local de Maven.

---

## Estructura de módulos

```
airport-queue/
├── common/             ← DTOs, protocolo, estructuras (Cola, TablaHash)
├── server/             ← Servidor central con sockets
├── client-registro/    ← UI: registro de pasajeros
├── client-general/     ← UI: cola general
├── client-prioritaria/ ← UI: cola prioritaria
└── client-especial/    ← UI: cola especial
```

### Dependencias entre módulos

```
common ←── server
common ←── client-registro
common ←── client-general
common ←── client-prioritaria
common ←── client-especial
```

---

## Workflow de Git

```
main       ← solo versiones estables (merge desde develop con PR)
develop    ← integración continua (merge desde ramas personales)
diego      ← trabajo de Diego (server, sockets, integración)
pablo      ← trabajo de Pablo (Cola.java, TablaHash.java, client-registro)
antonio    ← trabajo de Antonio (documentación, screenshots, manual)
```

### Flujo de trabajo diario

```bash
# 1. Actualizar tu rama con lo último de develop
git checkout diego          # (o pablo / antonio)
git pull origin develop

# 2. Trabajar, hacer commits frecuentes
git add .
git commit -m "feat(server): agregar ServerSocket en puerto 5000"

# 3. Subir tu rama
git push origin diego

# 4. Cuando algo está listo para integrar → Pull Request hacia develop
```

---

## Restricción crítica del rubric

> **Prohibido usar librerías externas para estructuras de datos y búsqueda.**
> `Cola<T>` y `TablaHash` deben implementarse desde cero en el módulo `common`.
> No agregar ninguna dependencia en `pom.xml` que resuelva esto automáticamente.

---

## Paquetes Java

| Módulo | Paquete base |
|---|---|
| common | `com.aeropuerto.common` |
| server | `com.aeropuerto.server` |
| client-registro | `com.aeropuerto.registro` |
| client-general | `com.aeropuerto.general` |
| client-prioritaria | `com.aeropuerto.prioritaria` |
| client-especial | `com.aeropuerto.especial` |
