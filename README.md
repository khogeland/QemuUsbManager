# QEMU USB Manager

![Example screenshot](/screenshot.png)

This is a graphical USB manager for QEMU, because copy-pasting lsusb output is a pain. The UI uses the Lanterna library and is intended to be run on the command line, but should work acceptably if run as a desktop application.

## Build
```bash
mvn clean package
```
This will spit out two files into a `bin` directory: the JAR, and an executable (`qemu-usb-manager`) to stick on your `PATH`.

## Requirements
QEMU >= 2.5.0

## Usage
Start QEMU with a QMP server, an xHCI bus named `xhci`, and an eHCI bus named `ehci`:
```
-qmp tcp:localhost:4565,server,nowait \
-device nec-usb-xhci,id=xhci \
-device usb-ehci,id=ehci
```

Now you can start the USB manager by running `qemu-usb-manager localhost 4565`

Navigate with the arrow keys. Use space to toggle attached USB devices.

To avoid speed mismatches, USB 3.0 devices will use the xHCI bus, and USB 1.x/2.0 will use eHCI.
