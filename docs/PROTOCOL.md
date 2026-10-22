# Protocolo HiGrow BLE v1

Nombre: **HiGrow-ESP32**. Periférico: ESP32; central: Android. Servicio primario: `7b183224-9168-443e-a927-7aeea07e8101`. Característica: `7b183224-9168-443e-a927-7aeea07e8102` (**READ | NOTIFY**). CCCD: `00002902-0000-1000-8000-00805f9b34fb`.

El advertising contiene el UUID del servicio y el nombre se incluye en la respuesta al escaneo. No requiere emparejamiento. Un cliente a la vez. El acceso GATT es abierto, sin autenticación ni cifrado configurados.

## Paquete de 12 bytes

Todos los campos de 16 bits usan **little-endian**, sin alineación ni estructuras C empaquetadas. MTU ATT predeterminado 23 admite hasta 20 bytes de payload: el paquete cabe sin negociación de MTU ni fragmentación propia.

| Offset | Tamaño | Tipo | Campo |
| --- | --- | --- | --- |
| 0 | 1 | uint8 | Versión, exactamente `1` |
| 1 | 2 | int16 | Temperatura × 10, complemento a dos (−53 = −5.3 °C) |
| 3 | 2 | uint16 | Humedad ambiental × 10 (567 = 56.7 % HR) |
| 5 | 2 | uint16 | Sustrato ADC filtrado, redondeado al entero más próximo, 0…4095 |
| 7 | 2 | uint16 | Luminosidad ADC real sin mapear, 0…4095 |
| 9 | 1 | uint8 | Sustrato: 0 Húmedo, 1 Semi Húmedo, 2 Seco |
| 10 | 1 | bitmask | Bit 0 = temperatura **y** humedad DHT válidas; bits 1…7 reservados |
| 11 | 1 | uint8 | Reservado, firmware envía `0` |

Cuando el DHT falla, el firmware envía **bit 0 apagado** y ambos campos DHT en cero. El cliente ignora esos campos y muestra `—`, conservando las mediciones ADC del mismo paquete. Un cero con bit de validez encendido es una lectura real. No se envía NaN ni una lectura DHT antigua como actual. El parser admite −40…80 °C y 0…100 % HR, envolvente del formato/sensor; no implica calibración o exactitud en todo ese rango.

Android exige longitud exacta, versión conocida, ADC válidos y estado de sustrato coherente. Los bits desconocidos y el byte reservado se ignoran; cualquier cambio incompatible deberá incrementar la versión. Las pruebas Android y C++ comparten este vector:

```text
01 EB 00 37 02 6C 07 00 08 01 01 00
```

Resultado: 23.5 °C, 56.7 % HR, sustrato 1900 ADC, luz 2048 ADC, Semi Húmedo, DHT válido. Luminosidad mapeada: `2048 * 1023 / 4095 = 511` (división entera), estado Baja.

## Filtro y clasificación

Se inicializa `asoilmoist = analogRead(32)` antes del primer paquete. Cada 100 ms:

```cpp
asoilmoist = 0.95 * asoilmoist + 0.05 * analogRead(32);
```

La clasificación se calcula sobre el mismo ADC **redondeado** que se transmite, asegurando igualdad entre firmware y Android:

| ADC | Estado |
| --- | --- |
| ≤ 1700 | Húmedo (0) |
| 1701…2099 | Semi Húmedo (1) |
| ≥ 2100 | Seco (2) |

La luz usa una sola lectura de GPIO35 por muestra. Se conserva el cálculo Arduino `map(luzRaw, 0, 4095, 0, 1023)`, equivalente a `luzRaw * 1023 / 4095`. La app clasifica con umbrales iniciales ≥1000 Muy Baja, ≥400 Baja, ≥120 Media, <120 Muy Alta. Se pueden cambiar desde la app y persistir localmente; no altera el firmware ni el protocolo. La tarjeta y gráfico muestran **ADC sin mapear**, nunca lux. El firmware registra `luzRaw` y `luz` por serie para diagnóstico.

## Secuencia GATT

1. Conectar y descubrir servicio/característica.
2. Registrar recepción local con `setCharacteristicNotification`.
3. Escribir `01 00` en CCCD y esperar `onDescriptorWrite` exitoso.
4. Leer una vez la última medición (READ). Después, recibir NOTIFY cada ~2000 ms.
5. Al desconectar, Android cierra GATT. El ESP32 retoma advertising desde `loop()` y sigue midiendo.

El firmware programa filtro y medición con diferencias de `millis()` sin signo (toleran rollover). La adquisición DHT y las operaciones BLE tienen sus tiempos propios; no hay `delay()` para programar ciclos. El primer READ tras arrancar contiene ADC reales y DHT inválido hasta que pasen 2 s. Las últimas 60 muestras incluyen READ y NOTIFY; no hay identificador de muestra para deduplicar una coincidencia temporal entre ambos.
