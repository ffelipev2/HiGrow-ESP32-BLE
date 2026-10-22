# Paneles nativos — HiGrow Monitor 1.1.0

La interfaz sigue la referencia visual suministrada por el usuario: cabecera verde bosque, marca de hoja, superficie blanca con esquinas superiores redondeadas, tarjetas suaves en rojo/azul/verde/amarillo y navegación con indicador verde claro. El diseño también adapta colores al modo oscuro y usa una sola columna con fuentes grandes o pantallas pequeñas.

| Panel | Comportamiento |
| --- | --- |
| Inicio | Estado BLE, nombre/dirección del dispositivo, antigüedad/hora de recepción y cuatro tarjetas con minigráficas de datos recibidos |
| Dispositivos BLE | Panel inferior con escaneo, señal RSSI del último resultado, dirección, selección manual y acciones para detener/cancelar/desconectar |
| Gráficos | Cuatro gráficas con unidades, ejes y tiempos; selector de últimas 15, 30 o 60 mediciones |
| Detalle del sensor | Acceso tocando tarjetas/gráficas; sensor elegido primero, mínima/máxima/promedio de lecturas válidas y escala de sustrato con indicador ADC |
| Historial | Hasta 60 paquetes de la conexión actual, ordenados del más reciente al más antiguo |
| Ajustes | Conexión, interruptor real de reconexión automática, edición validada de umbrales de luz y versión de la aplicación |

## Datos y compatibilidad

No se usan las cifras de la imagen como mediciones reales. Sin conexión, las tarjetas muestran `—` y las minigráficas permanecen vacías. Un error DHT no se transforma en cero; las estadísticas omiten datos ausentes y las líneas no se unen a través de muestras inválidas. Los avisos de datos desactualizados y paquetes rechazados siguen visibles.

Se conserva el protocolo BLE de 12 bytes, las UUIDs, las lecturas reales y todos los umbrales. El sustrato sigue siendo Húmedo hasta 1700, Semi Húmedo entre 1701 y 2099, y Seco desde 2100, incluidos los límites exactos. La luminosidad sigue mostrando ADC real 0–4095 y se clasifica en la escala mapeada 0–1023; ambas escalas se explican en Ajustes. No se presenta ADC como lux ni sustrato como porcentaje.

La reconexión automática se guarda en preferencias. Desactivarla cancela una espera de reconexión en curso; si hay conexión activa, se mantiene. Los fallos posteriores no generan nuevos reintentos. Activarla conserva el máximo de tres intentos existente. Una desconexión manual o salir al segundo plano sigue cerrando la conexión.

El historial se conserva en memoria durante la conexión y se vacía al desconectar. Las estadísticas corresponden a esas muestras, no a un historial permanente. El RSSI mostrado en la lista BLE proviene del escaneo y se etiqueta como tal; no se muestra una intensidad inventada como señal en vivo.

## Revisión y ejecución

- Compilar: `gradlew.bat testDebugUnitTest assembleDebug lintDebug`.
- APK: `app/build/outputs/apk/debug/app-debug.apk`.
- Versión: 1.1.0, código 2; puede actualizar la instalación debug anterior si conserva la misma firma.
- Vistas del editor: abrir `app/src/debug/java/cl/higrow/monitor/ui/DesignPreviews.kt` en Android Studio y elegir Design/Split. Incluye inicio vacío/conectado, gráficos, detalle, ajustes, historial, modo oscuro y fuente grande a 320 dp.

Las vistas previas contienen muestras exclusivas del editor y muestran la etiqueta **VISTA PREVIA · DATOS DE DISEÑO**. `MainActivity` no accede a esos datos. Las vistas previas no sustituyen la prueba de permisos y radio BLE en un teléfono real.
