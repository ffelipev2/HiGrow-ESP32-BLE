# Verificación

## nRF Connect para Android

1. Carga el firmware y abre monitor serie a 115200. Debe aparecer una línea de ADC/DHT aproximadamente cada 2 s, además del paquete inicial con DHT ERROR (todavía no se ha leído).
2. Cierra/desconecta HiGrow Monitor para liberar el periférico. Abre **nRF Connect for Mobile**, concede sus permisos y pulsa Scan.
3. Busca `HiGrow-ESP32`; comprueba el UUID de servicio `7b183224-9168-443e-a927-7aeea07e8101` en el advertising. El nombre puede aparecer al recibir la respuesta al escaneo.
4. Pulsa Connect. Localiza ese servicio y su característica `7b183224-9168-443e-a927-7aeea07e8102`; confirma propiedades **READ** y **NOTIFY** y descriptor **Client Characteristic Configuration (0x2902)**.
5. Pulsa READ. En la vista hexadecimal deben aparecer exactamente **12 bytes**, comenzando con `01`. Decodifica con [PROTOCOL.md](PROTOCOL.md). El vector de ejemplo del documento es una prueba fija; los bytes de la placa dependerán de sus sensores reales.
6. Habilita las notificaciones con el botón de suscripción. El log debe mostrar escritura CCCD **01 00** y eventos de 12 bytes cada ~2 s. No hace falta cambiar el MTU.
7. Cubre/ilumina el LDR y comprueba que el ADC cambia; en el módulo indicado, valores menores corresponden a mayor luz. Prueba los umbrales contra la señal real antes de ajustarlos.
8. Desconecta. Vuelve a Scan y conecta de nuevo sin reiniciar la placa. Debe reaparecer y continuar entregando mediciones. Después desconecta nRF Connect para probar la app.

## App + placa física

| Caso | Resultado esperado |
| --- | --- |
| Primera ejecución sin conexión | Cuatro tarjetas `—`, gráficos vacíos, sin valores simulados |
| Buscar con Bluetooth apagado | Solicitud del sistema para activarlo; si se cancela, mensaje recuperable |
| Rechazar permisos | Mensaje e indicación de Ajustes, sin cierre inesperado |
| Android 8–11 con Ubicación apagada | Mensaje para activarla antes del escaneo |
| Android 12+ | Solo permisos de dispositivos cercanos, sin ubicación |
| Buscar sin placa | Finaliza a los 15 s y permite buscar otra vez |
| Conectar | Primero Conectando; Conectado solo tras confirmar escritura CCCD |
| DHT11 válido | Temperatura y HR reales en tarjetas y gráficos |
| DHT11 con falla de señal | `—` en DHT y mensaje, bit 0 apagado; sustrato/luz continúan |
| Paquete malformado o versión desconocida desde periférico de prueba | Paquete rechazado; no actualiza la hora del dato válido |
| Silenciar NOTIFY desde periférico de prueba manteniendo enlace | Aviso de datos desactualizados tras 6 s |
| Alejar/apagar la placa | Cierre GATT; hasta 3 reintentos a 2/4/8 s; luego error y conexión manual |
| Desconectar/cancelar manualmente | No se producen reintentos automáticos |
| Apagar Bluetooth durante conexión | Mensaje de Bluetooth apagado y cierre de recursos |
| ESP32 con servicio/característica/CCCD diferente | Error descriptivo; no queda conectando indefinidamente |
| Rotar teléfono | Conserva conexión y muestras |
| Enviar app al segundo plano | Cierra conexión; al regresar permite conectar manualmente |
| Más de 60 muestras | Solo las últimas 60 permanecen en memoria |
| Cambiar umbrales LDR y reiniciar app | Conserva los nuevos límites; restaura 1000/400/120 con el botón correspondiente |
| Modo oscuro, pantalla pequeña y tamaño de fuente grande | Tarjetas legibles, disposición en una columna cuando corresponde |

No desconectes señales en caliente si la revisión de tu placa no permite hacerlo; la prueba de DHT inválido también puede hacerse con un firmware de diagnóstico que ponga el bit 0 en cero. Para probar paquetes o falta de notificaciones sin cambiar el producto, usa un periférico BLE de prueba con las mismas UUID. Estos escenarios requieren pruebas físicas: la compilación y las pruebas de parser no validan el enlace de radio.
