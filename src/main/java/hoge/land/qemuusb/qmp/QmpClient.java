package hoge.land.qemuusb.qmp;

import com.google.common.base.Throwables;
import org.anarres.qemu.qapi.api.DeviceDelCommand;
import org.anarres.qemu.qapi.api.ObjectPropertyInfo;
import org.anarres.qemu.qapi.api.QomGetCommand;
import org.anarres.qemu.qapi.api.QomListCommand;
import org.anarres.qemu.qapi.common.QApiConnection;
import org.anarres.qemu.qapi.common.QApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// TODO: Tests.

public class QmpClient {

    private QApiConnection connection;
    private Logger log = LoggerFactory.getLogger(this.getClass());

    public QmpClient(String host, int port) throws IOException {
        log.info("Connecting to QMP server on {}:{}", host, port);
        connection = new QApiConnection(host, port);
    }

    public void addDevice(short vendorId, short productId, boolean usb3) throws IOException {
        String deviceLabel = String.format("%04X:%04X", vendorId, productId);
        log.info("Attempting to attach device {} (USB 3.x: {})", deviceLabel, usb3);
        List<UsbDevice> devices = listXhciEhciBus0Devices();
        if (!devices.stream().anyMatch(usbDevice -> usbDevice.getVendorId() == vendorId && usbDevice.getProductId() == productId)) {
            UsbDeviceAddCommand command = new UsbDeviceAddCommand("usb-host", vendorId, productId, usb3 ? "xhci.0" : "ehci.0");
            connection.invoke(command);
            log.info("Attached device {}.", deviceLabel);
        } else {
            log.info("Device {} already attached.", deviceLabel);
        }
    }

    public void delDevice(short vendorId, short productId) throws IOException {
        String deviceLabel = String.format("%04X:%04X", vendorId, productId);
        log.info("Attempting to delete device {}", deviceLabel);
        List<UsbDevice> devices = listXhciEhciBus0Devices();
        devices.stream().filter( d -> d.getVendorId() == vendorId && d.getProductId() == productId).forEach(usbDevice -> {
            DeviceDelCommand command = new DeviceDelCommand(usbDevice.getQomPath());
            try {
                connection.invoke(command).getResult();
            } catch (QApiException | IOException e) {
                log.warn("Exception while deleting device {}. Device may still be attached.", deviceLabel, e);
            }
        });
        log.info("Deleted device {}.", deviceLabel);
    }

    public List<UsbDevice> listXhciEhciBus0Devices() throws IOException {
        String xhciRoot = "/machine/peripheral/xhci/xhci.0";
        String ehciRoot = "/machine/peripheral/ehci/ehci.0";
        List<UsbDevice> devices = listUsbDevicesForRoot(xhciRoot);
        devices.addAll(listUsbDevicesForRoot(ehciRoot));
        return devices;
    }

    private List<UsbDevice> listUsbDevicesForRoot(String root) throws IOException {
        try {
            List<UsbDevice> devices = new ArrayList<>();
            QomListCommand listChildrenCommand = new QomListCommand(root);
            List<ObjectPropertyInfo> childNames = connection.invoke(listChildrenCommand).getResult();
            for (ObjectPropertyInfo childName : childNames) {
                try {
                    String qomPath = root + "/" + childName.name;
                    QomGetCommand getVendorId = new QomGetCommand(qomPath, "vendorid");
                    QomGetCommand getProductId = new QomGetCommand(qomPath, "productid");
                    // Even though these are shorts, for some reason we get them back as Integer objects instead of Short
                    int vendorid = (int) connection.invoke(getVendorId).getResult();
                    int productid = (int) connection.invoke(getProductId).getResult();
                    devices.add(new UsbDevice((short) vendorid, (short) productid, qomPath));
                } catch (QApiException ignored) {
                    // Not a valid USB device. Whatever, ignore. There shouldn't be non-USB
                    // QOM objects in this list but apparently there are.
                }
            }
            return devices;
        } catch (QApiException e) {
            throw Throwables.propagate(e);
        }
    }

    public class UsbDevice {
        private short vendorid;
        private short productid;
        private String qomPath;
        public UsbDevice(short vendorid, short productid, String qomPath) {
            this.vendorid = vendorid;
            this.productid = productid;
            this.qomPath = qomPath;
        }

        public short getVendorId() {
            return vendorid;
        }

        public short getProductId() {
            return productid;
        }

        public String getQomPath() {
            return qomPath;
        }
    }

}
