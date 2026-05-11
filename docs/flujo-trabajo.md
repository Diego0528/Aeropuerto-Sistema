# Flujo de Trabajo del Sistema

## Flujo completo — paso a paso

```
PASAJERO en kiosko                    SERVIDOR                    OPERADOR en ventanilla
─────────────────                    ─────────                    ──────────────────────

1. Ingresa DPI
   │
   ├─ Consulta CatalogoRENAP (local)
   │  └─ Autocompleta nombre y datos
   │
2. Selecciona tipo de cola
   (General / Prioritaria / VIP)
   │
3. Presiona "Registrar"
   │
   ├──── REGISTRO|dpi|nombre|TIPO ──────────────────────────────────►
   │                                 4. GestorColas.registrarPasajero()
   │                                    - Verifica duplicado en TablaHash
   │                                    - Crea Pasajero
   │                                    - Asigna número de turno
   │                                    - encolar() en Cola<Pasajero>
   │                                    - insertar() en TablaHash
   │
   ◄─── CONFIRMACION|dpi|turno ─────────────────────────────────────
   │
4. Muestra: "Turno #7 — Cola GENERAL"
                                                         5. Presiona "Llamar Siguiente"
                                                            │
                                        ◄─── LLAMAR_SIGUIENTE|TIPO ─
                                        6. GestorColas.llamarSiguiente()
                                           - desencolar() de Cola<Pasajero>
                                           - Estado → EN_ATENCION
                                        ─── PASAJERO_LLAMADO|dpi|nombre|turno ──►
                                                         7. Muestra datos del pasajero
                                                            Habilita formulario:
                                                            - Vuelo (ComboBox)
                                                            - Datos específicos por cola

                                                         8. Presiona "Finalizar Atención"
                                                            │
                                        ◄─── FIN_ATENCION|dpi ──────
                                        9. GestorColas.finalizarAtencion()
                                           - Estado → ATENDIDO
                                           - eliminar() de TablaHash
                                        ─── CONFIRMACION|dpi|turno ─────────────►
                                                         10. Limpia pantalla,
                                                             listo para el siguiente
```

## Tipos de cola y sus formularios

### Cola General
El operador ve y registra:
- **Vuelo confirmado** — selección de lista estática (10 vuelos)
- **Observaciones** — texto libre

### Cola Prioritaria
El operador ve y marca los servicios requeridos:
- Silla de ruedas
- Asistencia para caminar
- Oxígeno a bordo
- Acompañante autorizado
- Embarazada (más de 32 semanas)
- Adulto mayor (más de 65 años)

### Cola Especial / VIP
El operador ve y activa:
- **Vuelo confirmado**
- **Clase** — Primera Clase / Clase Ejecutiva
- **Número de asiento** — campo de texto (ej: 1A, 2B)
- **Beneficios activables:**
  - Acceso Sala VIP
  - Boarding prioritario
  - Menú especial a bordo
  - Equipaje adicional (hasta 2 maletas)
  - Fast Track en seguridad

## Estados del pasajero

```
EN_ESPERA  ──(llamarSiguiente)──►  EN_ATENCION  ──(finalizarAtencion)──►  ATENDIDO
   │                                                                           │
   └── en Cola<Pasajero>                                                       └── eliminado de TablaHash
       y TablaHash                     en TablaHash (ya no en cola)
```

## CatalogoRENAP (datos ficticios)

El sistema incluye 15 personas predefinidas para demostración. Algunos DPI de prueba:

| DPI | Nombre |
|---|---|
| `1234567890101` | Carlos García López |
| `2345678901201` | María José Pérez Morales |
| `3456789012301` | Juan Pablo Méndez Castillo |
| `4567890123401` | Ana Lucía Ramírez Solís |
| `5678901234501` | Pedro Antonio Santos Cruz |
| `1111111111111` | Diego Andrino González |
| `2222222222222` | Andrea Soto Castellanos |
| `3333333333333` | José Luis Girón Méndez |

Ver `CatalogoRENAP.java` para la lista completa.
