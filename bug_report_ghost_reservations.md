# Reporte de Bug: "Reservas Fantasma" en Algoritmo de Ruteo Core

Durante el proceso de refactorización del método `ruteoCoreMultipleAgendado` en `Algorithms.java`, se identificó un error crítico en la lógica de asignación de recursos que afectaba significativamente la precisión de la simulación.

## Descripción del Bug

En la versión original, el algoritmo intentaba detener la búsqueda de núcleos (cores) una vez que encontraba uno válido para un enlace. Sin embargo, el mecanismo utilizado para "romper" el bucle era incorrecto para la estructura de control empleada.

### Código Original (Simplificado)
```java
// Bucle que recorre una lista de núcleos candidatos
for (int core : coresByFreeFS) {
    // ... validaciones de capacidad y crosstalk ...
    if (validCoreFound) {
        enlacesLibres.add(link);
        kspCores.add(core);
        
        // INTENTO DE ROMPER EL BUCLE
        core = cores; 
    }
}
```

### El Error Técnico
En Java, cuando se utiliza un bucle **`for-each`**, modificar la variable de iteración local (`core`) **no detiene el bucle**. El iterador interno sigue avanzando silenciosamente hacia el siguiente elemento de la lista `coresByFreeFS`.

## Impacto en la Simulación

Este error generaba una cadena de fallos lógicos que distorsionaban los resultados:

1.  **Validaciones Redundantes**: Si un enlace tenía varios núcleos libres, el algoritmo agregaba el **mismo enlace** múltiples veces a la lista `enlacesLibres`.
2.  **Éxito Prematuro**: La condición de éxito se basaba en el tamaño de la lista:
    ```java
    if (enlacesLibres.size() == ksp.getEdgeList().size())
    ```
    Si una ruta tenía 3 enlaces (A, B, C) y el enlace A tenía 3 núcleos libres, el algoritmo creía haber completado la ruta entera solo con el primer enlace, ignorando los enlaces B y C.
3.  **Reservas "Ciegas" (Ghost Reservations)**: Al reportar éxito prematuramente, el simulador procedía a instalar la demanda en los enlaces B y C sin haber verificado si estaban libres o si cumplían con los requisitos de crosstalk.
4.  **Colisiones de Espectro**: El simulador terminaba sobrescribiendo ranuras de frecuencia que ya estaban ocupadas por otras demandas, resultando en una red con colisiones físicas no detectadas.

## Por qué el ruteo parecía "mejor" antes
El usuario notó que el bloqueo era menor con el bug. Esto se debe a que el algoritmo permitía instalar demandas que **deberían haber sido bloqueadas** por falta de capacidad o exceso de crosstalk en los enlaces intermedios. "Aprobaba" rutas que físicamente estaban rotas o colisionadas.

## Corrección Aplicada en la Refactorización
En la propuesta refactorizada, se reemplazó esta lógica por un control estricto:
- Uso de `break` explícito para asegurar un solo núcleo por enlace.
- Verificación secuencial obligatoria de **todos** los enlaces de la ruta antes de declarar éxito.
- Validación de colisiones en `Utils.assignFs` para alertar si se intenta ocupar un recurso ya reservado.

---
> [!NOTE]
> Este hallazgo demuestra que la refactorización no solo mejoró la legibilidad, sino que restauró la integridad física de la simulación.
