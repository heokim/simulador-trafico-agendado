# Implementacion completa: Erlang variable en 5 franjas

Este documento unifica la implementacion de:

- Generacion de trafico con Erlang variable en 5 franjas.
- Ruteo por franja para el caso H3.
- Grafico de Erlang objetivo, Erlang cursado y bloqueo acumulado.

La idea es poder copiar esta misma implementacion a otra rama sin separar la logica en varios documentos.

## Objetivo general

El simulador conserva el modo de Erlang fijo y agrega un modo variable que divide el tiempo total de simulacion en 5 franjas iguales:

```text
BAJO -> MEDIO -> ALTO -> MEDIO -> BAJO
```

Cada franja define un Erlang objetivo. Ese Erlang objetivo se usa para generar demandas con la misma base probabilistica anterior del proyecto: llegadas por Poisson y lifetime asociado a `HT = Erlang / Lambda`.

Caso particular H3: el ruteo cambia segun el Erlang actual, es decir, la cantidad de conexiones activas en el tiempo `X`:

```text
3250 -> KSP por uso de FS + core por entropia + busqueda paralela MIN FRAG_BFR
3550 -> KSP por uso de FS + seleccion random de core + busqueda paralela MIN XT
4600 -> KSP por uso de FS + heuristicCoresOrder + busqueda paralela MIN XT
```

## Archivos involucrados

Para pasar esta implementacion a otra rama, revisar o copiar:

```text
pom.xml
src/main/java/py/una/pol/simulador/eon/SimulatorTest.java
src/main/java/py/una/pol/simulador/eon/rsa/Algorithms.java
src/main/java/py/una/pol/simulador/eon/utils/IErlangDistribution.java
src/main/java/py/una/pol/simulador/eon/utils/DynamicErlangDistribution.java
src/main/java/py/una/pol/simulador/eon/utils/GraphAnalyticsUtils.java
```

Tambien verificar si la rama destino tiene estos archivos compatibles:

```text
src/main/java/py/una/pol/simulador/eon/utils/Database.java
src/main/java/py/una/pol/simulador/eon/utils/SimulacionResumen.java
```

## Dependencia Maven

El grafico usa JFreeChart. En `pom.xml` debe existir:

```xml
<!-- JFreeChart for graphs -->
<dependency>
    <groupId>org.jfree</groupId>
    <artifactId>jfreechart</artifactId>
    <version>1.5.4</version>
</dependency>
```

Sin esta dependencia no compila `GraphAnalyticsUtils`.

## Distribucion Erlang

### IErlangDistribution

Ubicacion:

```text
src/main/java/py/una/pol/simulador/eon/utils/IErlangDistribution.java
```

Contrato:

```java
int getErlang(int time, int totalTime, int baseErlang);
String getTrafficType(int time, int totalTime);
```

`getErlang(...)` devuelve el Erlang objetivo que se debe usar en una unidad de tiempo. `getTrafficType(...)` devuelve la etiqueta de franja, util para rotulos o diagnostico.

### DynamicErlangDistribution

Ubicacion:

```text
src/main/java/py/una/pol/simulador/eon/utils/DynamicErlangDistribution.java
```

Recibe:

```java
new DynamicErlangDistribution(ERLANG_BAJO, ERLANG_MEDIO, ERLANG_ALTO);
```

Divide el tiempo total en quintos:

```java
double fraction = (double) time / totalTime;
if (fraction < 0.2) return "BAJO";
if (fraction < 0.4) return "MEDIO";
if (fraction < 0.6) return "ALTO";
if (fraction < 0.8) return "MEDIO";
return "BAJO";
```

Y asigna los Erlang:

```java
BAJO  -> erlangBajo
MEDIO -> erlangMedio
ALTO  -> erlangAlto
MEDIO -> erlangMedio
BAJO  -> erlangBajo
```

Importante: si `fraction` es exactamente `0.2`, cae en la segunda franja (`MEDIO`), porque la primera condicion es `fraction < 0.2`.

## Cambios en SimulatorTest

Ubicacion:

```text
src/main/java/py/una/pol/simulador/eon/SimulatorTest.java
```

### Configuracion

Se agregan valores de Erlang por franja:

```java
private static int ERLANG = 0;
public static int ERLANG_BAJO = 3250;
public static int ERLANG_MEDIO = 3550;
public static int ERLANG_ALTO = 4600;
```

Para `H3`, los valores de franja tambien se usan como umbrales para decidir el algoritmo segun conexiones activas:

```java
conexionesActivas <= 3250 -> MinFunction.FRAG_BFR + CoreSelectionEnum.SORTED_BY_ENTROPY;
conexionesActivas <= 3550 -> MinFunction.XT + CoreSelectionEnum.RANDOM;
conexionesActivas > 3550  -> MinFunction.XT + CoreSelectionEnum.HEURISTIC_ORDER;
```

### Modo fijo y modo variable

El modo fijo delega con `distribution = null`:

```java
public static double simular() throws IOException, SQLException {
    return ejecutarSimulacion(null);
}
```

El modo variable crea la distribucion de 5 franjas:

```java
public static double simulacionErlangVariable() throws IOException, SQLException {
    DynamicErlangDistribution distribution = new DynamicErlangDistribution(ERLANG_BAJO, ERLANG_MEDIO, ERLANG_ALTO);
    return ejecutarSimulacion(distribution);
}
```

El flujo comun es:

```java
private static double ejecutarSimulacion(IErlangDistribution distribution) throws IOException, SQLException
```

## Generacion de demandas

El simulador sigue generando demandas con la misma base anterior:

```java
Integer demandasQuantity = MathUtils.poisson(lambda);
Integer tLife = MathUtils.getLifetime(HT);
```

La diferencia es el valor de `HT`.

Antes, con Erlang fijo:

```java
input.getErlang() / input.getLambda()
```

Ahora, en modo variable:

```java
int currentErlang = distribution.getErlang(i, input.getSimulationTime(), input.getErlang());
currentErlang / input.getLambda()
```

El codigo genera demandas por cada unidad de tiempo:

```java
List<Demand> demands = Utils.generateDemands(
        input.getLambda(),
        input.getSimulationTime(),
        input.getFsRangeMin(),
        input.getFsRangeMax(),
        graph.vertexSet().size(),
        currentErlang / input.getLambda(),
        demandsQ,
        i,
        T_RANGE_MIN,
        T_RANGE_MAX
);
```

Por lo tanto:

- Poisson se evalua en cada unidad de tiempo.
- `lambda` permanece fijo.
- La duracion media de las conexiones cambia con el Erlang objetivo de la franja.
- La cantidad de conexiones activas sube o baja con cierto retraso respecto al escalon de Erlang objetivo.

## Seleccion de ruteo

En esta rama `H3`, el ruteo se selecciona por el Erlang actual. En la implementacion, ese valor es `establishedRoutes.size()`, o sea la cantidad real de conexiones activas en el tiempo `X`.

Nota importante: los metodos de ruteo deben especificarse antes de implementar o portar la logica a otra rama. Este caso `H3` usa la tabla definida para esta rama:

```text
conexionesActivas <= 3250 -> KSP por uso de FS + core por entropia + busqueda paralela MIN FRAG_BFR
conexionesActivas <= 3550 -> KSP por uso de FS + seleccion random de core + busqueda paralela MIN XT
conexionesActivas > 3550  -> KSP por uso de FS + heuristicCoresOrder + busqueda paralela MIN XT
```

La llamada base sigue siendo:

```java
EstablishedRoute establishedRoute = Algorithms.ruteoCoreMultipleAgendadoFixed(
        graph, demand, input.getCapacity(), input.getCores(),
        input.getMaxCrosstalk(), XT_Per_Unit_Length,
        routingConfig.minFunction, routingConfig.coreSelection);
```

La seleccion usada es:

```java
private static RoutingConfig seleccionarRuteoH3(int erlangActual) {
    if (erlangActual <= ERLANG_BAJO) {
        return new RoutingConfig(MinFunction.FRAG_BFR, CoreSelectionEnum.SORTED_BY_ENTROPY, ...);
    }
    if (erlangActual <= ERLANG_MEDIO) {
        return new RoutingConfig(MinFunction.XT, CoreSelectionEnum.RANDOM, ...);
    }
    return new RoutingConfig(MinFunction.XT, CoreSelectionEnum.HEURISTIC_ORDER, ...);
}
```

Para los valores `3550` y `4600`, se mantiene `MIN XT` como funcion de minimizacion porque el pedido solo especifica la estrategia de core.

## Metodos de ruteo en Algorithms

Ubicacion:

```text
src/main/java/py/una/pol/simulador/eon/rsa/Algorithms.java
```

En esta rama no se agregan metodos nuevos dentro de `Algorithms`. Se reutiliza `ruteoCoreMultipleAgendadoFixed(...)`, que ya implementa:

- KSP ordenado por uso de FS mediante `ordenarKSPPorUso(kspPaths)`.
- Busqueda paralela sobre los posibles indices FS.
- Seleccion del resultado minimo segun `MIN_FUNCTION`.
- Para H3, la funcion de minimizacion y la estrategia de core se pasan desde `seleccionarRuteoH3(...)`.

## Grafico

Ubicacion:

```text
src/main/java/py/una/pol/simulador/eon/utils/GraphAnalyticsUtils.java
```

El grafico se genera solo en modo Erlang variable:

```java
if (erlangVariable) {
    String fileName = "erlang_vs_tiempo_" + simulacionId + ".png";
    GraphAnalyticsUtils.guardarGraficoErlang(...);
}
```

Nombre de salida:

```text
erlang_vs_tiempo_<idSimulacion>.png
```

### Series capturadas

En `SimulatorTest`:

```java
double[] xTime = erlangVariable ? new double[input.getSimulationTime()] : null;
double[] yErlang = erlangVariable ? new double[input.getSimulationTime()] : null;
double[] yErlangReal = erlangVariable ? new double[input.getSimulationTime()] : null;
double[] yBloqueosAcum = new double[input.getSimulationTime()];
```

Durante la generacion previa:

```java
xTime[i] = i;
yErlang[i] = currentErlang;
```

Durante la simulacion:

```java
yErlangReal[t] = establishedRoutes.size();
```

Bloqueo acumulado:

```java
double pocentajeT = 0.0;
if (demandaNumero > 0) {
    pocentajeT = ((double) NUMERO_BLOQUEOS * 100.0) / demandaNumero;
}
yBloqueosAcum[t] = pocentajeT;
```

### Series dibujadas

El grafico actual muestra:

- Azul: `Erlang Objetivo`.
- Naranja: `Erlang Cursado`, calculado con `establishedRoutes.size()`.
- Rojo: `% Bloqueo Acumulado`.

No se dibuja la serie magenta de bloqueo por tiempo.

### Eje de bloqueo fijo

El eje derecho de bloqueo es estatico para poder comparar distintos graficos:

```java
NumberAxis axis2 = new NumberAxis("% Bloqueo");
axis2.setRange(0.0, 7.0);
```

Esto fuerza el rango de bloqueo a:

```text
0%..7%
```

### Bandas visuales

El fondo muestra las 5 franjas temporales:

```java
double slice = totalTiempo / 5.0;

addMarker(plot, 0, slice, "Trafico Bajo", colorBajo);
addMarker(plot, slice, slice * 2, "Trafico Medio", colorMedio);
addMarker(plot, slice * 2, slice * 3, "Trafico Alto", colorAlto);
addMarker(plot, slice * 3, slice * 4, "Trafico Medio", colorMedio);
addMarker(plot, slice * 4, totalTiempo, "Trafico Bajo", colorBajo);
```

Colores:

```java
Color colorBajo = new Color(0, 255, 0, 40);
Color colorMedio = new Color(255, 255, 0, 40);
Color colorAlto = new Color(255, 0, 0, 40);
```

## Relacion entre franja, Erlang cursado y bloqueo

La curva azul es el Erlang objetivo configurado para cada franja. Por eso se ve escalonada.

La curva naranja es el Erlang cursado real, o conexiones activas. Esta curva puede parecer mas suave o con forma de campana porque depende de:

- Demandas nuevas por Poisson.
- Lifetime de cada demanda.
- Bloqueos.
- Liberacion de rutas vencidas.

La curva roja es bloqueo acumulado. No necesariamente tiene forma de campana porque arrastra toda la historia desde el inicio de la simulacion.

## Configuracion actual de ejemplo

En `main`:

```java
TOPOLOGY = TopologiesEnum.USNET;
DESCRIPCION = "Dinamico, erlangs variables, ruteo adaptado a la carga";
simulacionErlangVariable();
```

Valores actuales:

```java
ERLANG_BAJO = 3250;
ERLANG_MEDIO = 3550;
ERLANG_ALTO = 4600;
VALOR_H = "h3";
XT_Per_Unit_Length = XTPerUnitLenght.H3.getValue();
```

Esta configuracion corresponde al caso particular `H3`. Si se trabaja con `H1`, `H2` u otra combinacion, se debe confirmar primero que tecnicas de ruteo usar.

## Checklist para copiar a otra rama

1. Agregar `IErlangDistribution.java`.
2. Agregar `DynamicErlangDistribution.java`.
3. Agregar `GraphAnalyticsUtils.java`.
4. Agregar `org.jfree:jfreechart:1.5.4` en `pom.xml`.
5. Agregar `ERLANG_BAJO`, `ERLANG_MEDIO`, `ERLANG_ALTO`.
6. Agregar `simulacionErlangVariable()`.
7. Refactorizar `simular()` para llamar a `ejecutarSimulacion(null)`.
8. Crear `ejecutarSimulacion(IErlangDistribution distribution)`.
9. Generar demandas con `currentErlang / input.getLambda()`.
10. Guardar `erlangPorTiempo[i] = currentErlang`.
11. Configurar `VALOR_H = "h3"` y `XT_Per_Unit_Length = XTPerUnitLenght.H3.getValue()`.
12. Agregar `seleccionarRuteoH3(...)` con la tabla de ruteo H3.
13. Usar `ruteoCoreMultipleAgendadoFixed(...)` con `routingConfig.minFunction` y `routingConfig.coreSelection`.
14. Capturar `xTime`, `yErlang`, `yErlangReal`, `yBloqueosAcum`.
15. Generar `erlang_vs_tiempo_<idSimulacion>.png` solo si `erlangVariable` es `true`.
16. Fijar el eje de bloqueo en `0..7`.
17. Compilar.
18. Ejecutar `simulacionErlangVariable()` y revisar consola + PNG.

## Validacion esperada

Al ejecutar el modo variable:

- La consola imprime el Erlang objetivo, conexiones activas y el ruteo elegido para H3.
- Se genera un PNG `erlang_vs_tiempo_<idSimulacion>.png`.
- El grafico muestra 5 bandas de fondo.
- La curva azul muestra el Erlang objetivo por franja.
- La curva naranja muestra conexiones activas.
- La curva roja muestra bloqueo acumulado con eje fijo de `0` a `7%`.

## Puntos que deben quedar iguales

Para reproducir la implementacion:

- Mantener la regla de franjas `0.2 / 0.4 / 0.6 / 0.8`.
- Mantener la secuencia `BAJO -> MEDIO -> ALTO -> MEDIO -> BAJO`.
- Mantener `MathUtils.poisson(lambda)` para demandas por unidad de tiempo.
- Mantener `currentErlang / input.getLambda()` para el lifetime.
- Mantener la tabla de ruteo H3 con umbrales 3250/3550/4600.
- Seleccionar algoritmo por conexiones activas en esta rama H3, usando `establishedRoutes.size()`.
- Mantener el grafico solo para modo variable.
- Mantener el eje de bloqueo en `0..7%` si se quieren comparar graficos.
