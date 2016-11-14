package hoge.land.qemuusb.usb;


import javax.usb.UsbDevice;
import javax.usb.UsbException;
import javax.usb.UsbHostManager;
import javax.usb.UsbHub;
import java.io.UnsupportedEncodingException;
import java.util.List;

public class UsbManager {

    public static UsbDeviceNode getDevices() throws UsbException, UnsupportedEncodingException {
        return createDevice(UsbHostManager.getUsbServices().getRootUsbHub());
    }

    private static UsbDeviceNode createDevice(UsbDevice device) throws UnsupportedEncodingException, UsbException {
        UsbDeviceNode node = new UsbDeviceNode(device);
        if (UsbHub.class.isAssignableFrom(device.getClass())) {
            UsbHub hub = (UsbHub) device;
            for (UsbDevice usbDevice : ((List<UsbDevice>) hub.getAttachedUsbDevices())) {
                node.getChildren().add(createDevice(usbDevice));
            }
        }
        return node;
    }

}
