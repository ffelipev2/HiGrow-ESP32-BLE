#include <cassert>
#include <cstring>
#include "../firmware/HiGrowMonitor/SensorProtocol.h"
int main() {
  const uint8_t expected[] = {1, 0xeb, 0, 0x37, 2, 0x6c, 7, 0, 8, 1, 1, 0};
  uint8_t packet[12];
  higrow::encode(packet, 235, 567, 1900, 2048, true);
  assert(std::memcmp(packet, expected, 12) == 0);
  higrow::encode(packet, -53, 567, 1700, 4095, true);
  assert(packet[1] == 0xcb && packet[2] == 0xff);
  assert(packet[9] == 0 && packet[7] == 0xff && packet[8] == 0x0f);
  higrow::encode(packet, 235, 567, 2100, 0, false);
  assert(packet[1] == 0 && packet[2] == 0 && packet[3] == 0 && packet[4] == 0 && packet[10] == 0);
  assert(higrow::soilState(1700) == 0);
  assert(higrow::soilState(1701) == 1);
  assert(higrow::soilState(2099) == 1);
  assert(higrow::soilState(2100) == 2);
}
