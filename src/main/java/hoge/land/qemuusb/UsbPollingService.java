package hoge.land.qemuusb;

import com.google.common.base.Throwables;
import com.google.common.util.concurrent.AbstractScheduledService;
import hoge.land.qemuusb.usb.UsbDeviceNode;
import hoge.land.qemuusb.usb.UsbManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usb4java.javax.DeviceNotFoundException;

import javax.usb.UsbDisconnectedException;
import javax.usb.UsbException;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class UsbPollingService extends AbstractScheduledService {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    private UsbManagerGui gui;

    public UsbPollingService(UsbManagerGui gui) {
        this.gui = gui;
    }

    @Override
    protected void runOneIteration() throws Exception {
        try {
            List<UsbDeviceNode> devices = UsbManager.getDevices().asList();
            devices = devices.stream().distinct().collect(Collectors.toList());
            // Remove any fake devices.
            devices.removeIf(node -> node.getVendorId() == -1 && node.getProductId() == -1);
            gui.setDeviceList(devices);
        } catch (UsbException | IOException | UsbDisconnectedException | DeviceNotFoundException e) {
            // These are all bound to happen occasionally and are nothing worrying.
            log.warn("Exception while listing host USB devices. Device may have been modified during read.", e);
        } catch (Exception e) {
            log.error("Unexpected exception while listing host USB devices.", e);
            Throwables.propagate(e);
        }
    }

    @Override
    protected Scheduler scheduler() {
        return Scheduler.newFixedDelaySchedule(0, 1, TimeUnit.SECONDS);
    }
}
