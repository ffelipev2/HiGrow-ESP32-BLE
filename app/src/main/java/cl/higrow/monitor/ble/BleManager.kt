@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package cl.higrow.monitor.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.*
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.os.SystemClock
import android.provider.Settings
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/** Todas las operaciones y callbacks se serializan en el hilo principal. Una operación GATT
 * pendiente a la vez: discover -> CCCD -> READ. No depende de MTU, bonding ni Internet. */
@SuppressLint("MissingPermission")
class BleManager(private val context: Context) {
    private val handler = Handler(Looper.getMainLooper())
    private val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
    private val mutableConnection = MutableStateFlow(ConnectionState())
    val connection = mutableConnection.asStateFlow()
    private val mutableDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val devices = mutableDevices.asStateFlow()
    private val mutablePackets = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val packets = mutablePackets.asSharedFlow()
    private var gatt: BluetoothGatt? = null
    private var scanner: BluetoothLeScanner? = null
    private var scanCallback: ScanCallback? = null
    private var timeout: Runnable? = null
    private var reconnect: Runnable? = null
    private var target: BluetoothDevice? = null
    private var retryCount = 0
    private var manualDisconnect = true
    private var ready = false
    private var autoReconnect = true
    private var closed = false
    private var session = 0

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_OFF) {
                disconnect()
                reportError("Bluetooth apagado. Actívalo para volver a conectar.")
            }
        }
    }
    init {
        ContextCompat.registerReceiver(context, receiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED), ContextCompat.RECEIVER_EXPORTED)
    }

    fun requiredPermissions(): Array<String> = if (Build.VERSION.SDK_INT >= 31) {
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    fun hasPermissions() = requiredPermissions().all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
    fun bluetoothAvailable() = adapter != null
    fun bluetoothEnabled() = hasPermissions() && runCatching { adapter?.isEnabled == true }.getOrDefault(false)
    private fun prerequisites(forScan: Boolean = false): Boolean {
        val error = when {
            closed -> "La sesión BLE está cerrada"
            adapter == null -> "Este equipo no dispone de Bluetooth BLE"
            !hasPermissions() -> "Faltan permisos para buscar o conectar dispositivos"
            !bluetoothEnabled() -> "Bluetooth apagado"
            forScan && Build.VERSION.SDK_INT <= 30 && !locationEnabled() ->
                "Android 8–11 requiere activar Ubicación para buscar dispositivos BLE"
            else -> null
        }
        if (error != null) reportError(error)
        return error == null
    }

    private fun locationEnabled(): Boolean = if (Build.VERSION.SDK_INT >= 28) {
        context.getSystemService(LocationManager::class.java)?.isLocationEnabled == true
    } else Settings.Secure.getInt(context.contentResolver, Settings.Secure.LOCATION_MODE,
        Settings.Secure.LOCATION_MODE_OFF) != Settings.Secure.LOCATION_MODE_OFF

    fun startScan() {
        if (!prerequisites(forScan = true)) return
        if (gatt != null || reconnect != null) return
        stopScan()
        mutableDevices.value = emptyList()
        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                handler.post {
                    if (scanCallback !== this) return@post
                    runCatching {
                        val name = result.scanRecord?.deviceName ?: result.device.name ?: BleContract.DEVICE_NAME
                        val item = DiscoveredDevice(result.device.address, name, result.rssi)
                        mutableDevices.value = (mutableDevices.value.filterNot { it.address == item.address } + item)
                            .sortedByDescending { it.rssi }
                    }.onFailure { reportError("Permiso de Bluetooth revocado") }
                }
            }
            override fun onScanFailed(errorCode: Int) {
                handler.post {
                    if (scanCallback === this) {
                        stopScan()
                        reportError("No se pudo buscar (código $errorCode). Espera unos segundos y reintenta.")
                    }
                }
            }
        }
        try {
            scanner = adapter?.bluetoothLeScanner ?: error("Escáner BLE no disponible")
            scanCallback = callback
            update(ConnectionStatus.SCANNING, "Buscando HiGrow durante 15 segundos…")
            // Filtros OR: UUID de servicio o nombre exacto. Se deduplica por dirección.
            scanner?.startScan(listOf(
                ScanFilter.Builder().setServiceUuid(ParcelUuid(BleContract.SERVICE)).build(),
                ScanFilter.Builder().setDeviceName(BleContract.DEVICE_NAME).build(),
            ), ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), callback)
            setTimeout(15_000) {
                stopScan()
                update(ConnectionStatus.DISCONNECTED, if (mutableDevices.value.isEmpty())
                    "No se encontró HiGrow. Acerca la placa y vuelve a buscar."
                else "Selecciona un dispositivo para conectar")
            }
        } catch (e: Exception) {
            stopScan()
            reportError("No se pudo iniciar el escaneo: ${e.message}")
        }
    }

    fun stopScan() {
        scanCallback?.let { callback -> runCatching { scanner?.stopScan(callback) } }
        scanCallback = null
        scanner = null
        if (connection.value.status == ConnectionStatus.SCANNING) {
            clearTimeout()
            update(ConnectionStatus.DISCONNECTED, "Búsqueda detenida")
        }
    }

    fun connect(address: String) {
        if (!prerequisites()) return
        disconnect()
        retryCount = 0
        manualDisconnect = false
        try {
            target = adapter?.getRemoteDevice(address) ?: error("Dispositivo no disponible")
            connectTarget()
        } catch (e: Exception) { fail("No se pudo conectar: ${e.message}", retry = false) }
    }

    private fun connectTarget() {
        if (!prerequisites()) { manualDisconnect = true; return }
        val device = target ?: return
        ready = false
        session++
        update(ConnectionStatus.CONNECTING, "Conectando y habilitando notificaciones…")
        try {
            gatt = device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE,
                BluetoothDevice.PHY_LE_1M_MASK, handler)
            if (gatt == null) { fail("Android no pudo abrir la conexión GATT"); return }
            setTimeout(12_000) { fail("Se agotó el tiempo para conectar o configurar GATT") }
        } catch (e: Exception) { fail("Error al abrir GATT: ${e.message}") }
    }

    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(client: BluetoothGatt, status: Int, newState: Int) {
            if (client !== gatt) { runCatching { client.close() }; return }
            if (status != BluetoothGatt.GATT_SUCCESS || newState == BluetoothProfile.STATE_DISCONNECTED) {
                fail("Se perdió la conexión GATT (código $status)")
            } else if (newState == BluetoothProfile.STATE_CONNECTED) {
                try {
                    if (!client.discoverServices()) fail("No se pudo descubrir el servicio")
                } catch (e: Exception) { fail("Error descubriendo servicios: ${e.message}") }
            }
        }

        override fun onServicesDiscovered(client: BluetoothGatt, status: Int) {
            if (client !== gatt) return
            if (status != BluetoothGatt.GATT_SUCCESS) { fail("Error de descubrimiento GATT: $status"); return }
            val characteristic = client.getService(BleContract.SERVICE)?.getCharacteristic(BleContract.SENSOR)
            if (characteristic == null) { fail("Servicio o característica HiGrow no encontrados", false); return }
            val required = BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY
            if (characteristic.properties and required != required) {
                fail("La característica no admite READ y NOTIFY", false); return
            }
            val descriptor = characteristic.getDescriptor(BleContract.CCCD)
            if (descriptor == null) { fail("No se encontró el descriptor CCCD", false); return }
            try {
                if (!client.setCharacteristicNotification(characteristic, true)) {
                    fail("No se pudo activar la recepción de notificaciones", false); return
                }
                val started = if (Build.VERSION.SDK_INT >= 33) {
                    client.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) == BluetoothStatusCodes.SUCCESS
                } else {
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    client.writeDescriptor(descriptor)
                }
                if (!started) fail("No se pudo escribir el CCCD")
            } catch (e: Exception) { fail("Error configurando notificaciones: ${e.message}") }
        }

        override fun onDescriptorWrite(client: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            if (client !== gatt || descriptor.uuid != BleContract.CCCD) return
            if (status != BluetoothGatt.GATT_SUCCESS) { fail("El ESP32 rechazó el CCCD: $status"); return }
            clearTimeout()
            ready = true
            update(ConnectionStatus.CONNECTED, "Notificaciones habilitadas · cada 2 s")
            try {
                if (!client.readCharacteristic(descriptor.characteristic)) {
                    update(ConnectionStatus.CONNECTED, "READ no disponible; esperando la próxima notificación")
                }
            } catch (e: Exception) { fail("Error leyendo la medición: ${e.message}") }
        }

        override fun onCharacteristicRead(client: BluetoothGatt, characteristic: BluetoothGattCharacteristic,
            value: ByteArray, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) receive(client, characteristic, value)
        }
        override fun onCharacteristicRead(client: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (Build.VERSION.SDK_INT < 33 && status == BluetoothGatt.GATT_SUCCESS)
                receive(client, characteristic, characteristic.value ?: byteArrayOf())
        }
        override fun onCharacteristicChanged(client: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            receive(client, characteristic, value)
        }
        override fun onCharacteristicChanged(client: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (Build.VERSION.SDK_INT < 33) receive(client, characteristic, characteristic.value ?: byteArrayOf())
        }
    }

    private fun receive(client: BluetoothGatt, characteristic: BluetoothGattCharacteristic, bytes: ByteArray) {
        if (client === gatt && ready && characteristic.uuid == BleContract.SENSOR) mutablePackets.tryEmit(bytes.copyOf())
    }
    private fun fail(message: String, retry: Boolean = true) {
        clearTimeout()
        releaseGatt()
        if (retry && autoReconnect && !manualDisconnect && retryCount < 3 && bluetoothEnabled()) {
            val delay = longArrayOf(2000, 4000, 8000)[retryCount++]
            update(ConnectionStatus.RECONNECTING, "$message. Reintento $retryCount/3 en ${delay / 1000} s")
            val task = Runnable { reconnect = null; if (!manualDisconnect) connectTarget() }
            reconnect = task
            handler.postDelayed(task, delay)
        } else {
            manualDisconnect = true
            reportError(if (retryCount >= 3) "$message. Se agotaron los 3 reintentos; conecta manualmente." else message)
        }
    }
    private fun releaseGatt() {
        ready = false
        val previous = gatt
        gatt = null // Los callbacks tardíos ya no pertenecen a la sesión actual.
        runCatching { previous?.disconnect() }
        runCatching { previous?.close() }
    }
    fun disconnect() {
        manualDisconnect = true
        reconnect?.let(handler::removeCallbacks)
        reconnect = null
        stopScan()
        clearTimeout()
        releaseGatt()
        target = null
        update(ConnectionStatus.DISCONNECTED, "Desconectado. Puedes volver a buscar.")
    }
    fun setAutoReconnect(enabled: Boolean) {
        autoReconnect = enabled
        if (!enabled && connection.value.status == ConnectionStatus.RECONNECTING) disconnect()
    }
    fun reportError(message: String) { update(ConnectionStatus.ERROR, message) }
    private fun update(status: ConnectionStatus, message: String) {
        val connectedAt = if (status == ConnectionStatus.CONNECTED) {
            connection.value.connectedAtElapsed ?: SystemClock.elapsedRealtime()
        } else null
        mutableConnection.value = ConnectionState(status,
            if (target != null) BleContract.DEVICE_NAME else null, message, session, connectedAt,
            runCatching { target?.address }.getOrNull())
    }
    private fun setTimeout(delay: Long, action: () -> Unit) {
        clearTimeout()
        timeout = Runnable(action).also { handler.postDelayed(it, delay) }
    }
    private fun clearTimeout() { timeout?.let(handler::removeCallbacks); timeout = null }
    fun close() {
        if (closed) return
        disconnect()
        closed = true
        runCatching { context.unregisterReceiver(receiver) }
    }
}
