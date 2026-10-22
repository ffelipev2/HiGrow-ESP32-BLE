# Validación local — 22 de octubre de 2026

## Adaptación a tablet Android 1.2.0

- `gradlew.bat testDebugUnitTest assembleDebug lintDebug`: **BUILD SUCCESSFUL**; **13 pruebas JUnit correctas**, Lint con **0 errores y 10 advertencias**.
- Versión **1.2.0**, código **3**, instalada mediante actualización ADB en una **Lenovo TB-J606F**, pantalla física 1200 × 2000 px, densidad 240 dpi. Se abrió `MainActivity` y se comprobó Inicio sin conexión y el selector BLE en horizontal.
- Se revisaron las pantallas con contenido ejecutando las composiciones de `DesignPreviews.kt` mediante `androidx.compose.ui.tooling.PreviewActivity`, incluida únicamente en debug. Estas pantallas muestran **VISTA PREVIA · DATOS DE DISEÑO**. Las capturas de tablet del README proceden de esta comprobación y no se presentan como mediciones de sensores.
- Inspección visual en horizontal: Inicio usa cuatro tarjetas, Gráficos muestra los cuatro trazados grandes y sus ejes en dos filas, detalle muestra los cuatro sensores en dos columnas, Historial pasa a tabla y Ajustes distribuye conexión/umbrales en columnas.
- En vertical se comprobó la cuadrícula de Gráficos; el selector 15/30/60 permanece accesible. El diseño responde al ancho y a la escala de fuente, con desplazamiento vertical para el contenido que no cabe.
- Se inspeccionó Gráficos con fuente al **150 %** y se comprobó la distribución compacta equivalente a **393 dp** cambiando temporalmente la densidad a 488 dpi: barra inferior, Inicio con dos tarjetas por fila y Gráficos en una columna. Esto verifica una ventana de tamaño teléfono en la tablet; no es una segunda prueba en un teléfono físico.
- Al terminar se restauraron densidad física **240 dpi**, escala de fuente **1.0** y rotación bloqueada horizontal (**1**), como estaban inicialmente. Se cerró la actividad de vista previa y se dejó abierta la app normal **1.2.0**.
- No se cambió firmware ni protocolo BLE. No se detectó HiGrow durante el escaneo de esta tablet; esta revisión no valida de nuevo el enlace físico ni la reconexión.

## Rediseño Android 1.1.0

Se implementaron los paneles de la referencia del usuario: Inicio, selector BLE inferior, Gráficos, detalle de sensores, Historial y Ajustes. También se añadió una preferencia funcional de reconexión automática y estadísticas que omiten lecturas DHT inválidas. No se cambió el firmware, el protocolo ni los umbrales durante este rediseño.

- `gradlew.bat testDebugUnitTest assembleDebug lintDebug`: **BUILD SUCCESSFUL**.
- **13 pruebas JUnit, 0 fallos y 0 errores**: 11 de protocolo/umbrales y 2 de estadísticas.
- Lint: **0 errores, 10 advertencias** de configuración/dependencias existentes.
- APK comprobado con `aapt`: versión **1.1.0**, código **2**, minSdk **26**, targetSdk **35**, sin permiso Internet.
- Vistas Compose renderizadas e inspeccionadas en Android Studio; se revisaron tarjetas vacías/conectadas, gráficos, ajustes y variantes de accesibilidad. Las muestras del editor están identificadas como datos de diseño y separadas de `MainActivity`.
- Comprobación física posterior: ADB identificó un **Xiaomi 25069PTEBG** con la app **1.1.0 / código 2** instalada. Se abrió la app, se detectó `HiGrow-ESP32` en el selector y se estableció la conexión BLE. Se observaron temperatura y humedad DHT válidas, ADC de sustrato/luz y paquetes sucesivos separados aproximadamente 2 s en Historial. Se revisaron Inicio, Gráficos, detalle, Historial y Ajustes en modo oscuro. Las [capturas del README](../README.md#paneles-de-la-app--versión-110) proceden de esta ejecución real, sin datos simulados.

Detalles de pantallas y comportamiento: [DESIGN.md](DESIGN.md).

## Corrección DHT11 posterior a la primera entrega

Se confirmó que Arduino IDE usaba una biblioteca DHT antigua que imprimía `Read fail`. Se respaldó fuera de `libraries` y se instaló Adafruit DHT 1.4.6. Se añadió alimentación del HiGrow original mediante GPIO16 LOW, configuración de placa en `BoardConfig.h`, comprobación de biblioteca y diagnóstico por serie/modo sin BLE.

La revisión corregida compila con el core **3.3.2 instalado en Arduino IDE** (1 093 483 bytes de flash; 39 504 bytes de RAM) y con el core **3.3.12 fijado por el proyecto** (1 105 319 bytes de flash; 41 452 bytes de RAM). Durante el diagnóstico también se compiló la variante sin BLE y con depuración Adafruit (858 404 bytes de flash; 33 356 bytes de RAM).

Se leyó la salida física anterior a la corrección en COM6: fallos DHT continuos y ADC reales. El cargador no entró automáticamente en modo programación durante el intento asistido. **Posteriormente el usuario confirmó que el DHT ya funciona.** Esa confirmación es del usuario; no se repitió una captura serie válida durante el rediseño Android. Detalles en [DHT_TROUBLESHOOTING.md](DHT_TROUBLESHOOTING.md).

## Primera entrega

Ejecutada en Windows con JDK JetBrains 21.0.10 y Android SDK Platform 35 / Build Tools 36.0.0.

| Comprobación | Resultado |
| --- | --- |
| Wrapper Gradle 8.13 incluido | Ejecutado correctamente; distribución verificada con SHA-256 |
| `gradlew.bat testDebugUnitTest assembleDebug lintDebug` | **BUILD SUCCESSFUL** |
| Pruebas JUnit | **11 pruebas, 0 fallos, 0 errores, 0 omitidas** |
| Lint Android | **0 errores, 11 advertencias** |
| Compilación firmware con Arduino-ESP32 3.3.12 | **Correcta**, ESP32 Dev Module, partición Huge APP |
| Flash firmware | 1 104 747 bytes de 3 145 728 (35 %) |
| RAM estática firmware | 41 444 bytes de 327 680 (12 %) |
| Prueba C++ del codificador y umbrales | **Correcta**, compilada y ejecutada con MSVC x64, `/EHsc /W4` |
| UUIDs/nombre entre firmware y Android | Coinciden |
| Expresión original del filtro | Conservada literalmente |
| Permisos del APK mediante `aapt dump badging` | minSdk 26, targetSdk 35; sin permiso Internet; ubicación limitada a API 30 |
| XML y espacios finales en fuentes | Correctos |

Las advertencias Lint corresponden a disponibilidad de versiones más nuevas de SDK/dependencias, un atributo de permiso de API 31 ignorado en sistemas anteriores, reglas modernas de respaldo y una sugerencia de estilo KTX para preferencias. No se deshabilitó Lint ni se creó un baseline para ocultar errores. Las versiones se mantienen fijadas en la combinación compilada; compile/target SDK 35 no son los más recientes.

APK debug generado: `app/build/outputs/apk/debug/app-debug.apk` (17 582 661 bytes). Reportes: `app/build/reports/tests/testDebugUnitTest/index.html` y `app/build/reports/lint-results-debug.html`. Los archivos `build` son artefactos locales ignorados por Git y se regeneran con Gradle.

El firmware se compiló usando una instalación aislada del core y las bibliotecas DHT 1.4.6 / Unified Sensor 1.1.15 en `.tools`, sin reemplazar el core 3.3.2 ya instalado en Arduino IDE. Los binarios de validación están en `.tools/firmware`; esa carpeta contiene herramientas y cachés locales y está ignorada por Git.

## Alcance y comprobaciones pendientes en hardware

La primera entrega y la compilación del rediseño se validaron sin teléfono conectado. La comprobación física posterior descrita arriba confirma el descubrimiento, la conexión y la recepción continua de datos en un dispositivo, además de la visualización de los paneles en modo oscuro. No constituye una prueba de alcance, reconexión ante fallos, permisos en todas las versiones Android, ciclo de vida completo ni calibración/precisión de sensores. No se volvió a cargar firmware durante esta comprobación. Sigue [TESTING.md](TESTING.md) para completar esas pruebas con HiGrow y nRF Connect.
