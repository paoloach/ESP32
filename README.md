# ESP32 plugin for CLION

Plugin for CLION allowing developing a ESP32 firmware, downloading it and debugging it using the integrated debugger by jtag.

## SETUP
In the setting menu, under *Build,Execution, Deployment*, you can find the *ESP32* entry consisting in several voices

![setup](/docs/images/setup.png)

- [ESP32 espressif SDK path](#esp32-espressif-sdk-path)
- crosscompiler path
- Default serial port
- Default serial flashing baud rate
- ESP32 Openocd path

### ESP32 espressif SDK path

This is the path where you installed the ESP32 esk, that is the path where you clone the repository
https://github.com/espressif/esp-idf.git