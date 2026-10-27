# Paneles nativos — HiGrow Monitor 1.3.1

La interfaz sigue la referencia visual suministrada por el usuario: cabecera verde bosque, marca de hoja, superficie blanca con esquinas superiores redondeadas, tarjetas suaves en rojo/azul/verde/amarillo y navegación con indicador verde claro. El diseño también adapta colores al modo oscuro y reduce las columnas según el ancho disponible y el tamaño de texto.

La versión 1.3.0 añade **Brote**, una carita animada con expresiones ligadas a las lecturas. Incluye saludo al tocarla y explicación de condiciones; usa degradados suaves, bordes coloreados y una paleta verde más profunda en modo oscuro. En tablet se sitúa sobre los controles de la columna lateral; en teléfono encabeza Inicio. Ver [comportamiento y criterios de Brote](BROTE.md). Las capturas que siguen documentan las versiones anteriores indicadas en el README.

La corrección 1.3.1 elimina el recuadro de pulsación de la carita y combina las condiciones simultáneas en título, resumen, consejo y rasgos del personaje; por ejemplo, sustrato seco con poca luz. El saludo y la accesibilidad del botón se mantienen.

Las [capturas del README](../README.md#paneles-de-la-app--versión-110) muestran los seis paneles ejecutándose en un Xiaomi 25069PTEBG con datos reales recibidos del HiGrow por BLE, en modo oscuro. Los PNG originales están en [screenshots](screenshots/).

| Panel | Comportamiento |
| --- | --- |
| Inicio | Estado BLE, nombre/dirección del dispositivo, antigüedad/hora de recepción y cuatro tarjetas con minigráficas de datos recibidos |
| Dispositivos BLE | Panel inferior con escaneo, señal RSSI del último resultado, dirección, selección manual y acciones para detener/cancelar/desconectar |
| Gráficos | Cuatro gráficas con unidades, ejes y tiempos; selector de últimas 15, 30 o 60 mediciones |
| Detalle del sensor | Acceso tocando tarjetas/gráficas; sensor elegido primero, mínima/máxima/promedio de lecturas válidas y escala de sustrato con indicador ADC |
| Historial | Hasta 60 paquetes de la conexión actual, ordenados del más reciente al más antiguo |
| Ajustes | Conexión, interruptor real de reconexión automática, edición validada de umbrales de luz y versión de la aplicación |

## Tablet y ventanas redimensionables (1.2.0)

La adaptación depende de las dimensiones de la **ventana actual**, no de un modelo de dispositivo ni de bloquear la orientación. Se elimina el límite central de 1000 dp. Desde 600 dp de ancho, la navegación pasa a una barra lateral desplazable y el contenido usa el resto del ancho con márgenes de 24 dp; en ventanas menores se mantiene la barra inferior y el margen de 14 dp.

`AdaptiveLayout.kt` distribuye las filas según el ancho útil y la escala de fuente. Las tarjetas de una fila tienen la misma altura, se reduce el número de columnas con texto grande y se conserva el orden de lectura de izquierda a derecha y de arriba abajo.

| Pantalla | Distribución adaptable |
| --- | --- |
| Inicio | Desde 900 dp legibles y 480 dp de alto útil, conexión y controles en un panel lateral desplazable; a su lado, sensores en 2 × 2 con altura calculada a partir de la ventana. Las minigráficas crecen dentro de las tarjetas. En tablet vertical las tarjetas también aumentan de altura; en teléfono conservan la presentación compacta. Con texto grande se apilan los controles y se permite desplazar el contenido. |
| Gráficos | Dos columnas cuando cada gráfica dispone de al menos 280 dp legibles. La altura del trazado se calcula con la altura útil, reservando espacio para selector, ejes, etiquetas y avisos; en ventanas estrechas sigue siendo compacta. |
| Detalle | Hasta dos columnas de al menos 300 dp legibles; el sensor elegido sigue primero y las minigráficas aumentan de altura en tablet. |
| Historial | Tabla con hora y los cuatro sensores desde 720 dp legibles; por debajo se usan una o dos columnas de tarjetas. Mantiene el orden más reciente primero y el desplazamiento vertical. |
| Ajustes | Conexión y acerca de en una columna, umbrales en otra cuando caben dos bloques de al menos 300 dp legibles. En ventanas estrechas quedan apilados. |
| Selector BLE | Panel centrado con ancho limitado por Material 3, abierto completamente y con contenido desplazable para poder alcanzar los botones en horizontal. |

Las [capturas de tablet del README](../README.md#tablet-y-ventanas-amplias--versión-121) muestran las vistas de diseño ejecutadas en la Lenovo TB-J606F. Sus muestras siguen separadas de `MainActivity` y se identifican en pantalla. Inicio corresponde a la corrección 1.2.1; los otros paneles mantienen la distribución 1.2.0.

## Datos y compatibilidad

No se usan las cifras de la imagen como mediciones reales. Sin conexión, las tarjetas muestran `—` y las minigráficas permanecen vacías. Un error DHT no se transforma en cero; las estadísticas omiten datos ausentes y las líneas no se unen a través de muestras inválidas. Los avisos de datos desactualizados y paquetes rechazados siguen visibles.

Se conserva el protocolo BLE de 12 bytes, las UUIDs, las lecturas reales y todos los umbrales. El sustrato sigue siendo Húmedo hasta 1700, Semi Húmedo entre 1701 y 2099, y Seco desde 2100, incluidos los límites exactos. La luminosidad sigue mostrando ADC real 0–4095 y se clasifica en la escala mapeada 0–1023; ambas escalas se explican en Ajustes. No se presenta ADC como lux ni sustrato como porcentaje.

La reconexión automática se guarda en preferencias. Desactivarla cancela una espera de reconexión en curso; si hay conexión activa, se mantiene. Los fallos posteriores no generan nuevos reintentos. Activarla conserva el máximo de tres intentos existente. Una desconexión manual o salir al segundo plano sigue cerrando la conexión.

El historial se conserva en memoria durante la conexión y se vacía al desconectar. Las estadísticas corresponden a esas muestras, no a un historial permanente. El RSSI mostrado en la lista BLE proviene del escaneo y se etiqueta como tal; no se muestra una intensidad inventada como señal en vivo.

## Revisión y ejecución

- Compilar: `gradlew.bat testDebugUnitTest assembleDebug lintDebug`.
- APK: `app/build/outputs/apk/debug/app-debug.apk`.
- Versión: 1.3.1, código 6; puede actualizar la instalación debug anterior si conserva la misma firma.
- Vistas del editor: abrir `app/src/debug/java/cl/higrow/monitor/ui/DesignPreviews.kt` en Android Studio y elegir Design/Split. Incluye inicio vacío/conectado, gráficos, detalle, ajustes, historial, modo oscuro y fuente grande a 320 dp.
- El grupo **Tablet** añade vistas horizontales 1333 × 800 dp, verticales 800 × 1333 dp, una ventana de 600 dp y texto a escala 1.5. También hay una vista de teléfono horizontal de 850 × 393 dp.

Las vistas previas contienen muestras exclusivas del editor y muestran la etiqueta **VISTA PREVIA · DATOS DE DISEÑO**. `MainActivity` no accede a esos datos. Las vistas previas no sustituyen la prueba de permisos y radio BLE en un teléfono real.
