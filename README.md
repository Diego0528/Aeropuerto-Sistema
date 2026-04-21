# ✈️ AeroQueue - Sistema de Gestión de Colas de Aeropuerto

Proyecto universitario para el curso de Estructuras de Datos.  
Sistema distribuido de gestión de colas de atención a pasajeros, simulando un entorno aeroportuario real.

## 📋 Descripción

Simula la atención en múltiples ventanillas de un aeropuerto mediante colas diferenciadas por tipo:
- **General** — pasajeros estándar
- **Prioritaria** — adultos mayores, personas con discapacidad
- **Especial** — primera clase, casos especiales

La comunicación entre interfaces se realiza mediante **sockets TCP**, con todas las estructuras de datos implementadas desde cero.

## 🏗️ Arquitectura

Proyecto Maven multi-módulo compuesto por:

| Módulo | Descripción |
|---|---|
| `common` | Clases compartidas: modelos, enums, protocolo de mensajes |
| `server` | Servidor central, lógica de colas, gestión de conexiones |
| `client-registro` | Interfaz de registro de pasajeros |
| `client-general` | Ventanilla de atención general |
| `client-prioritaria` | Ventanilla de atención prioritaria |
| `client-especial` | Ventanilla de atención especial |

## 🛠️ Tecnologías

- Java 21
- JavaFX (OpenJFX 21.0.2)
- Maven (multi-módulo)
- Sockets TCP (implementación propia)
- Estructuras de datos propias (sin librerías externas)

## 👥 Equipo

| Integrante | Rol |
|---|---|
| Diego | Arquitectura de servidor, sockets, integración |
| Pablo | Estructuras de datos, interfaz de registro |
| Antonio | Documentación, manual de usuario, presentación |

## 🚀 Cómo ejecutar

> Instrucciones de ejecución pendientes conforme avance el desarrollo.

## 📁 Ramas

| Rama | Propósito |
|---|---|
| `main` | Versión estable y entregable |
| `develop` | Integración continua del equipo |
| `feature/diego-*` | Trabajo activo de Diego |
| `feature/pablo-*` | Trabajo activo de Pablo |
| `docs/antonio-*` | Documentación de Antonio |