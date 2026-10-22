#pragma once
#include <Arduino.h>

#ifndef HIGROW_DIAGNOSTIC_ONLY
#define HIGROW_DIAGNOSTIC_ONLY 0
#endif

// HiGrow original de https://www.hobbyelectronica.nl/en/product/esp32-plant-sensor-higrow/
// Referencia: mrdevrobot/HiGrow-Arduino-Esp, HiGrowEsp32/HiGrowEsp32.ino.
// No confundir con LilyGO T-HiGrow: DHT GPIO16, POWER_CTRL GPIO4 HIGH.
namespace board {
constexpr uint8_t DHT_PIN = 22;
constexpr uint8_t SOIL_PIN = 32;
constexpr uint8_t LIGHT_PIN = 35;  // LDR externo del proyecto, no el sensor de luz integrado.
constexpr int8_t SENSOR_POWER_PIN = 16;  // -1 si los sensores tienen alimentación permanente.
constexpr uint8_t SENSOR_POWER_ON = LOW;
constexpr uint8_t DHT_PULL_TIME_US = 55;  // Valor predeterminado de Adafruit DHT 1.4.6.
// true: mismos pines/lecturas por serie, radio BLE sin iniciar. Para aislar fallos DHT.
constexpr bool DIAGNOSTIC_ONLY = HIGROW_DIAGNOSTIC_ONLY != 0;

inline void enableSensorPower() {
  if (SENSOR_POWER_PIN >= 0) {
    pinMode(SENSOR_POWER_PIN, OUTPUT);
    digitalWrite(SENSOR_POWER_PIN, SENSOR_POWER_ON);
  }
}
}  // namespace board
