# Validación local — 22 de octubre de 2026

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

## Pendiente en hardware

No se cargó el firmware en la placa ni se instaló/ejecutó la app en un teléfono. `adb devices` no mostró dispositivos conectados. No se validaron por radio el advertising, alcance, reconexión, CCCD, ritmo efectivo de notificaciones ni comportamiento de permisos/ciclo de vida en versiones físicas de Android. Tampoco se verificaron visualmente las pantallas ejecutándose en un dispositivo ni la calibración/precisión de sensores. Sigue [TESTING.md](TESTING.md) para esas comprobaciones con HiGrow y nRF Connect.
