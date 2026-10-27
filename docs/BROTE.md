# Brote — compañero interactivo de HiGrow 1.3.0

Brote es un personaje vectorial de Compose Canvas. La app lo dibuja localmente, sin imágenes descargadas, servicios externos ni datos simulados en `MainActivity`. Sus colores, ojos y boca responden a las últimas lecturas válidas de la conexión actual. El movimiento de respiración, hojas y parpadeo se limita a la composición visible y las animaciones usan el sistema de animación de Compose.

## Interacción

- **Tocar la carita:** hace un pequeño salto/saludo y alterna el mensaje entre resumen, saludo y consejo. El saludo no cambia el estado calculado ni sustituye las lecturas.
- **Ver condiciones:** abre un diálogo con los cuatro sensores, sus valores actuales y los criterios de la expresión. Se puede desplazar el contenido con texto grande.
- **Cambiar los umbrales de luminosidad en Ajustes:** actualiza también la expresión de Brote.
- La cara es un botón accesible con descripción de su estado; la expresión se acompaña siempre de texto.

## Estados y orden de prioridad

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
| Me faltan datos | DHT ausente/no finito o paquete rechazado, sin otra alerta anterior |
| Feliz | Muestra actual con las cuatro lecturas presentes y sin las alertas anteriores |

Los avisos de sustrato y luz usan la misma clasificación que el resto de la app. Las referencias ambientales de temperatura/humedad son reglas generales de la interfaz, visibles en el diálogo: no identifican una especie ni diagnostican la salud de la planta. «Húmedo» no se interpreta como exceso de riego, y «Luz muy alta» no se interpreta automáticamente como daño por sol. Los mensajes invitan a comprobar la planta antes de actuar.

Si coinciden varias condiciones, la expresión sigue el orden de la tabla. El diálogo conserva los cuatro indicadores, incluidos los desconocidos, para que una sola expresión no oculte las otras condiciones. El sustrato y la luminosidad siguen disponibles cuando falla el DHT11. Un fallo DHT o un paquete rechazado no puede producir el estado Feliz.

## Distribución y vistas de diseño

En ventanas amplias Brote se integra en la columna lateral desplazable de Inicio. A su lado quedan las cuatro tarjetas de sensores, con sus minigráficas reales. En teléfono aparece un panel superior con la carita junto al resumen; con poco ancho o texto grande se apilan. Se mantiene el modo claro/oscuro.

`app/src/debug/java/cl/higrow/monitor/ui/DesignPreviews.kt` incluye el grupo **Brote**:

- `BroteThirstyPreview`, `BroteOfflinePreview` y `BroteLargeTextPreview`.
- `BroteExpressionsPreview`: galería de las once expresiones.
- `BroteInteractivePreview`: selector de escenarios para usar con el modo interactivo de las vistas previas de Android Studio.

Estos escenarios están en el origen `debug` y llevan la etiqueta **VISTA PREVIA · DATOS DE DISEÑO**. La app normal no presenta ese selector ni utiliza sus muestras.

## Verificación

`PlantAssessmentTest` cubre desconexión, espera, datos antiguos, límites exactos, umbrales de luz personalizados, DHT ausente/no finito, paquetes rechazados y prioridad de alertas simultáneas. Ejecutar junto a las pruebas existentes:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```
