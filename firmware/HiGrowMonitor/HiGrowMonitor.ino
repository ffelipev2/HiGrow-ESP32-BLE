// ESP32 original (HiGrow), Arduino-ESP32 3.3.12, Bluedroid incluido.
#include <Arduino.h>
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include <DHT.h>
#ifdef DHT11
#error "Biblioteca DHT antigua detectada. Usa DHT sensor library by Adafruit 1.4.6; consulta docs/DHT_TROUBLESHOOTING.md."
#endif
#include <atomic>
#include <math.h>
#include "SensorProtocol.h"
#include "BoardConfig.h"

constexpr uint32_t SAMPLE_INTERVAL_MS = 2000;
constexpr uint32_t FILTER_INTERVAL_MS = 100;
DHT dht(board::DHT_PIN, DHT11);
BLECharacteristic* measurement = nullptr;
BLE2902* cccd = nullptr;
std::atomic<bool> connected{false};
std::atomic<bool> restartAdvertising{false};
double asoilmoist = 0;
uint32_t lastSample = 0;
uint32_t lastFilter = 0;
bool advertisingPending = false;
uint32_t advertisingRequestedAt = 0;
uint32_t consecutiveDhtErrors = 0;

class ServerCallbacks : public BLEServerCallbacks {
  void onConnect(BLEServer*) override { connected.store(true); }
  void onDisconnect(BLEServer*) override {
    connected.store(false);
    restartAdvertising.store(true);
  }
};

void publish(bool readDht) {
  const int idleLevel = digitalRead(board::DHT_PIN);
  const float temperature = readDht ? dht.readTemperature() : NAN;
  const float humidity = readDht ? dht.readHumidity() : NAN;
  const bool valid = isfinite(temperature) && isfinite(humidity) &&
      temperature >= -40 && temperature <= 80 && humidity >= 0 && humidity <= 100;
  const uint16_t soilAdc = static_cast<uint16_t>(lround(asoilmoist));
  const int luzRaw = analogRead(35);
  // Se reutiliza la misma muestra: equivalente a map(analogRead(35), ...).
  const int luz = map(luzRaw, 0, 4095, 0, 1023);
  uint8_t packet[higrow::PACKET_SIZE];
  higrow::encode(packet, valid ? static_cast<int16_t>(lroundf(temperature * 10)) : 0,
      valid ? static_cast<uint16_t>(lroundf(humidity * 10)) : 0,
      soilAdc, static_cast<uint16_t>(luzRaw), valid);
  if (measurement != nullptr) {
    measurement->setValue(packet, sizeof(packet));
    if (connected.load() && cccd->getNotifications()) measurement->notify();
  }
  const char* dhtStatus = !readDht ? "ESPERANDO" : (valid ? "OK" : "ERROR");
  Serial.printf("Sustrato=%u estado=%u luzRaw=%d luz=%d DHT=%s\n",
      soilAdc, packet[9], luzRaw, luz, dhtStatus);
  if (readDht) {
    consecutiveDhtErrors = valid ? 0 : consecutiveDhtErrors + 1;
    Serial.printf("DHT11 GPIO%u: T=%.1f C HR=%.1f %% reposo=%s fallosConsecutivos=%lu\n",
        board::DHT_PIN, temperature, humidity, idleLevel == HIGH ? "HIGH" : "LOW",
        static_cast<unsigned long>(consecutiveDhtErrors));
    if (!valid && consecutiveDhtErrors == 3) {
      Serial.println("DHT: revisa alimentacion/pin/modelo. Prueba HIGROW_DIAGNOSTIC_ONLY=1 en BoardConfig.h.");
    }
  }
}

void setup() {
  Serial.begin(115200);
  board::enableSensorPower();
  Serial.printf("HiGrow original: DHT11 GPIO%u, alimentacion GPIO%d=%s, modo=%s\n",
      board::DHT_PIN, board::SENSOR_POWER_PIN, board::SENSOR_POWER_ON == LOW ? "LOW" : "HIGH",
      board::DIAGNOSTIC_ONLY ? "DIAGNOSTICO SIN BLE" : "BLE");
  pinMode(board::SOIL_PIN, INPUT);
  pinMode(board::LIGHT_PIN, INPUT);
  analogReadResolution(12);
  analogSetPinAttenuation(board::SOIL_PIN, ADC_11db);
  analogSetPinAttenuation(board::LIGHT_PIN, ADC_11db);
  asoilmoist = analogRead(32);  // Primera lectura real, sin partir de cero.
  dht.begin(board::DHT_PULL_TIME_US);
  if (board::DIAGNOSTIC_ONLY) {
    // Primer intento tras 2 s de alimentación, programado con millis().
    lastSample = lastFilter = millis();
    return;
  }
  BLEDevice::init(higrow::DEVICE_NAME);
  BLEServer* server = BLEDevice::createServer();
  server->setCallbacks(new ServerCallbacks());
  BLEService* service = server->createService(higrow::SERVICE_UUID);
  measurement = service->createCharacteristic(higrow::SENSOR_UUID,
      BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_NOTIFY);
  cccd = new BLE2902();
  measurement->addDescriptor(cccd);
  publish(false);  // READ inicial: ADC reales, DHT marcado inválido hasta los 2 s.
  service->start();
  BLEAdvertising* advertising = BLEDevice::getAdvertising();
  advertising->addServiceUUID(higrow::SERVICE_UUID);
  advertising->setScanResponse(true);  // Nombre en scan response (adv cabe en 31 B).
  advertising->start();
  lastSample = lastFilter = millis();
}

void loop() {
  const uint32_t now = millis();
  if (restartAdvertising.exchange(false)) {
    advertisingPending = true;
    advertisingRequestedAt = now;
  }
  // Dejar terminar el evento de desconexión sin bloquear el muestreo.
  if (advertisingPending && static_cast<uint32_t>(now - advertisingRequestedAt) >= 200) {
    advertisingPending = false;
    if (!connected.load()) BLEDevice::startAdvertising();
  }
  if (static_cast<uint32_t>(now - lastFilter) >= FILTER_INTERVAL_MS) {
    lastFilter = now;
    asoilmoist = 0.95 * asoilmoist + 0.05 * analogRead(32);
  }
  if (static_cast<uint32_t>(now - lastSample) >= SAMPLE_INTERVAL_MS) {
    lastSample = now;
    publish(true);
  }
  yield();
}
