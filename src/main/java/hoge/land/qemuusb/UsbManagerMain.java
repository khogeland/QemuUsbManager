package hoge.land.qemuusb;

import hoge.land.qemuusb.qmp.QmpClient;

import java.io.IOException;

public class UsbManagerMain {

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
        QmpClient client = new QmpClient(host, port);
        UsbManagerGui gui = new UsbManagerGui(client);
        gui.setup();
        new UsbPollingService(gui).startAsync();
        gui.start();
    }

}
