# DHT11: diagnóstico en HiGrow original

## Modelo y alimentación

El modelo indicado por el usuario es [ESP32 Plant sensor HiGrow de HobbyElectronica](https://www.hobbyelectronica.nl/en/product/esp32-plant-sensor-higrow/). La foto muestra el diseño HiGrow original con DHT11 en el extremo. La ficha comercial no especifica el pinout; la correspondencia con esta revisión se basa en esa foto y debe contrastarse con la placa si hay diferencias.

El [firmware de referencia del HiGrow original](https://github.com/mrdevrobot/HiGrow-Arduino-Esp/blob/master/HiGrowEsp32/HiGrowEsp32.ino) usa **GPIO22 para el DHT** y configura **GPIO16 como salida LOW** para encender los sensores. Esta inicialización estaba ausente en la primera versión del proyecto. Se añadió en `firmware/HiGrowMonitor/BoardConfig.h` antes de iniciar el DHT/BLE.

No se debe confundir con [LilyGO T-HiGrow](https://github.com/Xinyuan-LilyGO/LilyGo-HiGrow/blob/master/src/configuration.h), cuyo firmware usa **DHT GPIO16 y alimentación GPIO4 HIGH**. No se prueban pines automáticamente: GPIO16 cumple funciones distintas en ambas revisiones. Para una placa con sensores alimentados permanentemente, `SENSOR_POWER_PIN = -1` desactiva el control. El LDR externo sigue en GPIO35 y no se cambia por BH1750.

## Biblioteca antigua encontrada y corregida en este equipo

La caché de compilación de Arduino IDE confirmó que HiGrow estaba usando `Documents/Arduino/libraries/DHT/DHT.cpp`. Esa biblioteca antigua imprimía `Read fail`, tenía temporización fija y no era la biblioteca de la compilación validada del proyecto. Corregirla elimina una causa de fallos; no confirma por sí solo que el sensor físico funciona.

- Se instaló **DHT sensor library by Adafruit 1.4.6** en `Documents/Arduino/libraries/DHT_sensor_library`.
- Se conservó **Adafruit Unified Sensor 1.1.15**.
- La biblioteca anterior se movió, sin borrarla, a `Documents/Arduino/library_backups/DHT-legacy-20261022-123628`.
- Se añadió una comprobación que detiene la compilación con un mensaje claro si vuelve a seleccionarse la biblioteca antigua.

El cambio de biblioteca también afecta a otros sketches que incluyan `DHT.h` en este equipo. Para restaurarla, conserva primero la biblioteca moderna fuera de `libraries` y devuelve el respaldo a `libraries/DHT`; HiGrow Monitor requiere la biblioteca moderna. No mantengas ambas activas si Arduino selecciona la antigua.

## Probar el firmware corregido

1. Cierra y vuelve a abrir Arduino IDE para actualizar la lista de bibliotecas. Abre `firmware/HiGrowMonitor/HiGrowMonitor.ino`.
2. Usa ESP32 Dev Module, partición Huge APP, puerto de la placa (**COM6 en este equipo**, CP210x). Compila y carga. La aplicación Android no necesita reinstalarse para este cambio.
3. Abre Monitor Serie a **115200 baud**, pulsa reset y espera al menos 10 segundos. Debe aparecer:

```text
HiGrow original: DHT11 GPIO22, alimentacion GPIO16=LOW, modo=BLE
Sustrato=... estado=... luzRaw=... luz=... DHT=ESPERANDO
... DHT=OK
DHT11 GPIO22: T=... C HR=... % reposo=HIGH fallosConsecutivos=0
```

Los puntos suspensivos representan lecturas físicas variables. `ESPERANDO` es el paquete inicial antes de la primera lectura DHT. Si aparece `ERROR`, los campos DHT permanecen inválidos para Android; no se presenta un cero como medición real.

## Aislar BLE

Si persiste el fallo, en `BoardConfig.h` cambia `#define HIGROW_DIAGNOSTIC_ONLY 0` por `1`, vuelve a cargar y revisa el mismo Monitor Serie. Este modo conserva pines/alimentación y el muestreo de 2 segundos, pero **no inicializa BLE**. No aparecerá en la app mientras esté activado. Al terminar devuelve el valor a `0` y vuelve a cargar.

- **Funciona sin BLE y falla con BLE:** investigar temporización/carga del controlador; el pin/modelo/alimentación tienen una validación física inicial.
- **Falla en ambos modos:** confirmar revisión de placa, continuidad del GPIO22, alimentación real del DHT y resistencia pull-up según el esquema de esa placa. Un reposo LOW persistente es una pista para revisar la línea; HIGH no demuestra que el sensor esté conectado o alimentado.
- **Fallos ocasionales:** registrar varias líneas completas con `T`, `HR`, `reposo` y `fallosConsecutivos`. No reducir el intervalo por debajo de 2 segundos ni repetir inmediatamente una lectura fallida.

Para identificar timeout o checksum usando la depuración incluida en la biblioteca Adafruit, compila con `--build-property 'compiler.cpp.extra_flags=-DDHT_DEBUG -DHIGROW_DIAGNOSTIC_ONLY=1'` en Arduino CLI. Definir `DHT_DEBUG` solo en el `.ino` no activa la depuración del archivo `DHT.cpp`, que se compila por separado.

El puerto serie solo admite un proceso a la vez. Si otro programa necesita leerlo o cargar firmware, cierra primero el Monitor Serie. Durante esta investigación se cerró el Monitor Serie y se capturaron varios ciclos reales del firmware anterior en COM6: `Read fail ... DHT=ERROR`.

## Si la carga indica Wrong boot mode (0x13)

Con el Monitor Serie cerrado, mantén BOOT/IO0 pulsado, pulsa y suelta RESET/EN, espera un segundo y suelta BOOT. Inicia la carga. Si la placa sale de modo programación al iniciar la carga, mantén BOOT durante `Connecting...` y suéltalo cuando empiece a escribir. No modifiques cableado ni puentees pines sin identificar la revisión de placa.

En este equipo el intento de acceso al cargador falló primero con `Wrong boot mode (0x13)` y después con `No serial data received`. Eso no confirma una avería del DHT; impide cargar y verificar la corrección. El primer fallo ocurrió antes de escribir en la memoria de la placa.
