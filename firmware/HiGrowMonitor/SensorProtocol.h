#pragma once
#include <stdint.h>

namespace higrow {
constexpr char DEVICE_NAME[] = "HiGrow-ESP32";
constexpr char SERVICE_UUID[] = "7b183224-9168-443e-a927-7aeea07e8101";
constexpr char SENSOR_UUID[] = "7b183224-9168-443e-a927-7aeea07e8102";
constexpr uint8_t PACKET_SIZE = 12;
constexpr uint8_t VERSION = 1;
constexpr uint16_t SOIL_WET_MAX = 1700;
constexpr uint16_t SOIL_DRY_MIN = 2100;

inline uint8_t soilState(uint16_t adc) {
  return adc >= SOIL_DRY_MIN ? 2 : (adc > SOIL_WET_MAX ? 1 : 0);
}
inline void write16(uint8_t* dst, uint16_t value) {
  dst[0] = static_cast<uint8_t>(value & 0xff);
  dst[1] = static_cast<uint8_t>(value >> 8);
}
inline void encode(uint8_t* packet, int16_t temperatureTenths,
                   uint16_t humidityTenths, uint16_t soilAdc,
                   uint16_t lightAdc, bool dhtValid) {
  packet[0] = VERSION;
  write16(packet + 1, dhtValid ? static_cast<uint16_t>(temperatureTenths) : 0);
  write16(packet + 3, dhtValid ? humidityTenths : 0);
  write16(packet + 5, soilAdc);
  write16(packet + 7, lightAdc);
  packet[9] = soilState(soilAdc);
  packet[10] = dhtValid ? 1 : 0;
  packet[11] = 0;
}
}  // namespace higrow
