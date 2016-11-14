package hoge.land.qemuusb.usb;

import javax.usb.UsbConst;
import javax.usb.UsbDevice;
import javax.usb.UsbException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// TODO: Implement entire-hub passthrough. The tree representation is already done.
// NOTE: Not enough to simply pass through every device under a parent,
// must also manage physically hot-plugging new devices.
public class UsbDeviceNode {
    // libusb frequently has problems getting strings. Cache 'em.
    private static final Map<Integer, String> knownNames = new HashMap<>();

    private List<UsbDeviceNode> children = new ArrayList<UsbDeviceNode>();
    private String name;
    private UsbDevice device;

    public UsbDeviceNode(UsbDevice device) {
        this.device = device;
        this.name = nameFor(device);
    }

    public String getName() {
        return name;
    }


    public List<UsbDeviceNode> getChildren() {
        return children;
    }

    public List<UsbDeviceNode> getChildrenRecursive() {
        List<UsbDeviceNode> nodes = new ArrayList<>(children.size());
        for (UsbDeviceNode child : children) {
            nodes.add(child);
            nodes.addAll(child.getChildrenRecursive());
        }
        return nodes;
    }

    public void setChildren(List<UsbDeviceNode> children) {
        this.children = children;
    }

    public List<UsbDeviceNode> asList() {
        List<UsbDeviceNode> returnList = new ArrayList<>();
        returnList.add(this);
        children.forEach(c -> returnList.addAll(c.asList()));
        return returnList;
    }

    @Override
    public boolean equals(Object obj) {
        if (UsbDeviceNode.class.isAssignableFrom(obj.getClass())) {
            UsbDeviceNode other = (UsbDeviceNode) obj;
            return (getVendorId() == other.getVendorId() && getProductId() == other.getProductId());
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return ((int) getVendorId() << 16) + getProductId();
    }

    @Override
    public String toString() {
        return name;
    }

    public String getManufacturer() throws UnsupportedEncodingException, UsbException {
        return device.getString(device.getUsbDeviceDescriptor().iManufacturer()).trim();
    }

    // USB 3.x should use the xHCI controller, otherwise use the eHCI controller.
    public boolean isUSB3() {
        return device.getSpeed().equals(UsbConst.DEVICE_SPEED_FULL);
    }

    public short getVendorId() {
        return device.getUsbDeviceDescriptor().idVendor();
    }

    public String getProduct() throws UnsupportedEncodingException, UsbException {
        return device.getString(device.getUsbDeviceDescriptor().iProduct()).trim();
    }

    public short getProductId() {
        return device.getUsbDeviceDescriptor().idProduct();
    }

    private String nameFor(UsbDevice device) {
        if (knownNames.containsKey(hashCode())) {
            return knownNames.get(hashCode());
        }
        String name = String.format("[%04X:%04X] ", getVendorId(), getProductId());
        try {
            name += String.format("%s %s", getManufacturer(), getProduct());
            knownNames.put(hashCode(), name);
        } catch (UsbException | UnsupportedEncodingException ignored) {
            name += device.toString();
        }
        // Removes any non-printable characters so Lanterna doesn't barf
        return name.replaceAll("\\p{C}", "");
    }
}
