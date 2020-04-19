# ESP32 plugin for CLION

Plugin for CLION allowing developing a ESP32 firmware, downloading it and debugging it using the integrated debugger by jtag.

After installed this plugin, you will be able to create a new c project: ![ESP32](/docs/images/ESP32-project.png).  
 After create this type of project, you will see a set of predefined configurations to compile, flash and debug (by jtag) your ESP32 project and a new ESP32 serial console to see the ESP32 logs   


 
It assumes that you have already installed the ESP32 environment as described into  [Get Started](https://docs.espressif.com/projects/esp-idf/en/latest/esp32/get-started/index.html#get-started)

## SETUP
In the setting menu, under *Build,Execution, Deployment*, you can find the *ESP32* entry consisting in several voices

![setup](/docs/images/setup.png)

- [ESP32 espressif SDK path](#esp32-espressif-sdk-path)
- [Crosscompiler path](#crosscompiler-path)
- [Default Serial Port](#default-serial-port)
- [Default Serial Flashing Baud Rate](#default-serial-flashing-baud-rate)
- [ESP32 Openocd Path](#esp32-openocd-path)

### ESP32 espressif SDK path

It is the path where you installed the ESP32 esk, that is the path where you clone the repository
https://github.com/espressif/esp-idf.git
It should contain the file ***Kconfig***, by which it builds the windows containing the different options.

### Crosscompiler Path

It is the path where you installed the crosscompiler executable. It should contain the xtensa-esp32-elf-gcc compiler and his friends.

Please refer the espressif page [Standard Setup of Toolchain for Linux](https://docs.espressif.com/projects/esp-idf/en/latest/esp32/get-started-legacy/linux-setup.html#standard-setup-of-toolchain-for-linux-legacy-gnu-make)

### Default Serial Port
The default serial port used to flash and communicate with ESP32 chip.  
Anyway, you can customize this value on every different configuration 

### Default Serial Flashing Baud Rate 

The default baud rate used to flash ESP32 chip. 
Notice that the communication baud rate used is defined by *Serial flash config/Monitor Baud Rate* menu 

### ESP32 Openocd Path
In order to communicate with jtag, you need to install a version of OpenOCD customized to work with ESP32: [openocd-esp32](https://github.com/espressif/openocd-esp32).  
This path refers to the installation directory of this program.