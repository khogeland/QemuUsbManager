package hoge.land.qemuusb;

import hoge.land.qemuusb.qmp.QmpClient;
import hoge.land.qemuusb.usb.UsbDeviceNode;
import hoge.land.qemuusb.usb.UsbManager;
import org.usb4java.javax.DeviceNotFoundException;

import javax.usb.UsbDisconnectedException;
import javax.usb.UsbException;
import java.io.*;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class UsbManagerMain {

    private static QmpClient client;

    public static void main(String[] args) throws IOException {
        String host = null;
        int port = -1;
        try {
            host = args[0];
            port = Integer.valueOf(args[1]);
        } catch (IndexOutOfBoundsException | NumberFormatException e) {
            System.out.println("Required parameters: [host] [port]");
            System.exit(1);
        }
        client = new QmpClient(host, port);
        UsbManagerGui gui = new UsbManagerGui(client);
        gui.setup();
        new ScheduledThreadPoolExecutor(1).scheduleWithFixedDelay(() -> {
            try {
                List<UsbDeviceNode> devices = UsbManager.getDevices().asList();
                devices = devices.stream().distinct().collect(Collectors.toList());
                // Remove any fake devices.
                devices.removeIf(node -> node.getVendorId() == -1 && node.getProductId() == -1);
                gui.setDeviceList(devices);
            } catch (UsbException | IOException | UsbDisconnectedException | DeviceNotFoundException ignored) {
                // These are all bound to happen occasionally and are nothing worrying.
                // TODO: Log.
            } catch (RuntimeException e) {
                // A window update may swallow this if we only print the stack trace.
                // I also don't feel like configuring log4j or just yet. :~)
                die(e);
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
        gui.start();
    }

    public static void die(Exception e) {
        //TODO: This is shit. Delete it. Use logging framework.
        try {
            File deathLog = new File("death.log");
            FileWriter writer = new FileWriter(deathLog);
            StringWriter traceWriter = new StringWriter();
            PrintWriter pw = new PrintWriter(traceWriter);
            e.printStackTrace(pw);
            writer.write(traceWriter.toString());
            writer.flush();
            writer.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        System.exit(2);
    }

}
