# Brote — compañero interactivo de HiGrow 1.3.2

Brote es un personaje vectorial de Compose Canvas. La app lo dibuja localmente, sin imágenes descargadas, servicios externos ni datos simulados en `MainActivity`. Sus colores, ojos y boca responden a las últimas lecturas válidas de la conexión actual. El movimiento de respiración, hojas y parpadeo se limita a la composición visible y las animaciones usan el sistema de animación de Compose.

## Interacción

- **Tocar la carita:** hace un pequeño salto/saludo, sin recuadro ni efecto de pulsación rectangular. En Inicio compacto alterna el saludo de la cabecera; en el panel completo alterna resumen, saludo y consejo. Las explicaciones y consejos completos siempre están en **Ver condiciones**. Se conserva el botón accesible para teclado y lector de pantalla. El saludo no cambia el estado calculado ni sustituye las lecturas.
- **Ver condiciones:** abre un diálogo con los cuatro sensores, sus valores actuales y los criterios de la expresión. Se puede desplazar el contenido con texto grande.
- **Cambiar los umbrales de luminosidad en Ajustes:** actualiza también la expresión de Brote.
- La cara es un botón accesible con descripción de su estado; la expresión se acompaña siempre de texto.

## Estados y combinaciones

| Estado | Condición |
| --- | --- |
| En descanso | BLE no está conectado; se ignoran lecturas retenidas de otra sesión |
| ¿Sigues ahí? | Datos desactualizados según el aviso existente de 6 segundos; no se evalúan datos antiguos |
| Despertando | Hay conexión, pero aún no llegó una muestra |
| Tengo sed | Sustrato seco: ADC desde 2100, conforme al umbral existente |
| Tengo calor | Temperatura válida desde 32 °C |
| Tengo frío | Temperatura válida de 12 °C o menos |
| Con sueño | Luminosidad clasificada como Baja o Muy Baja con los umbrales actuales; el mensaje recuerda que puede ser de noche |
| Aire seco | Humedad ambiental válida por debajo de 30 % |
| Mucha humedad | Humedad ambiental válida por encima de 85 % |
| Me faltan datos | DHT ausente/no finito o paquete rechazado, también cuando coexiste con otras alertas |
| Feliz | Muestra actual con las cuatro lecturas presentes y sin las alertas anteriores |

Los avisos de sustrato y luz usan la misma clasificación que el resto de la app. Las referencias ambientales de temperatura/humedad son reglas generales de la interfaz, visibles en el diálogo: no identifican una especie ni diagnostican la salud de la planta. «Húmedo» no se interpreta como exceso de riego, y «Luz muy alta» no se interpreta automáticamente como daño por sol. Los mensajes invitan a comprobar la planta antes de actuar.

Con datos actuales se recopilan **todas** las condiciones activas. El título, el resumen y el consejo combinan sus mensajes, sin suprimir alertas por el orden de prioridad. Ejemplos: **Sed y poca luz**, **Poca luz y aire seco**, o **Sed · Poca luz · Datos incompletos**. Cuando desaparece una condición se elimina solo esa parte del estado combinado.

La carita combina rasgos: la poca luz produce ojos somnolientos incluso con sustrato seco; el gesto de sed y su gota siguen presentes. El frío modifica el color y el temblor aunque coincida con otra alerta. El gesto principal de la boca mantiene el orden de prioridad de la tabla, mientras los demás rasgos y el texto representan el conjunto. El diálogo conserva los cuatro indicadores, incluidos los desconocidos.

Los estados de desconexión, espera o datos antiguos son exclusivos: no se mezclan con alertas retenidas de otra sesión. El sustrato y la luminosidad siguen disponibles cuando falla el DHT11. Un fallo DHT o un paquete rechazado no puede producir el estado Feliz.

## Distribución y vistas de diseño

En ventanas amplias Brote se integra en la columna lateral desplazable de Inicio. A su lado quedan las cuatro tarjetas de sensores, con sus minigráficas reales. En teléfono aparece un panel superior compacto con el título del estado combinado y una carita de 92 dp. Los párrafos de explicación quedan en **Ver condiciones**, para dejar espacio a las cuatro mediciones. Con poco ancho o texto grande se usa la presentación completa y desplazable. Se mantiene el modo claro/oscuro.

`app/src/debug/java/cl/higrow/monitor/ui/DesignPreviews.kt` incluye el grupo **Brote**:

- `BroteThirstyPreview`, `BroteOfflinePreview` y `BroteLargeTextPreview`.
- `BroteExpressionsPreview`: galería de las once expresiones.
- `BroteCombinedPreview`: sustrato seco y poca luz en una misma lectura.
- `BroteInteractivePreview`: selector de escenarios simples y combinados para usar con el modo interactivo de las vistas previas de Android Studio.
- El grupo **Inicio en una pantalla** incluye teléfonos de 393 × 850 y 360 × 740 dp, condiciones combinadas, fuente al 125 % y una ventana corta de 320 × 640 dp.

Estos escenarios están en el origen `debug` y llevan la etiqueta **VISTA PREVIA · DATOS DE DISEÑO**. La app normal no presenta ese selector ni utiliza sus muestras.

## Verificación

`PlantAssessmentTest` cubre desconexión, espera, datos antiguos, límites exactos, umbrales de luz personalizados, DHT ausente/no finito, paquetes rechazados, combinaciones y recuperación parcial de alertas simultáneas. Ejecutar junto a las pruebas existentes:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```
