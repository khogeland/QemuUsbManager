package hoge.land.qemuusb;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.SimpleTheme;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import hoge.land.qemuusb.qmp.QmpClient;
import hoge.land.qemuusb.usb.UsbDeviceNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class UsbManagerGui {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    private final QmpClient client;
    private MultiWindowTextGUI textGUI;
    private BasicWindow window;
    private Terminal terminal;
    private Screen screen;
    private CheckBoxList<UsbDeviceNode> deviceList;
    private TextBox box;

    public UsbManagerGui(QmpClient client) {
        this.client = client;
    }

    public void setup() throws IOException {
        this.terminal = new DefaultTerminalFactory().createTerminal();
        this.screen = new TerminalScreen(terminal);
        screen.startScreen();
        screen.clear();
        textGUI = new MultiWindowTextGUI(screen, new DefaultWindowManager(), new EmptySpace(TextColor.ANSI.DEFAULT));
        box = new TextBox(new TerminalSize(80, 20));
        box.setReadOnly(true);
        deviceList = new CheckBoxList<>();
        addListenerToCheckBoxList(deviceList);
        window = new BasicWindow("QEMU USB Manager");
        window.setHints(Collections.singletonList(Window.Hint.FULL_SCREEN));
        window.setTheme(new SimpleTheme(TextColor.ANSI.WHITE, TextColor.ANSI.DEFAULT));
        window.setComponent(createPanel(deviceList, box));
    }

    public void setDeviceList(List<UsbDeviceNode> devices) throws IOException {
        CheckBoxList<UsbDeviceNode> newList = new CheckBoxList<>();
        UsbDeviceNode selectedItem = deviceList.getSelectedItem();
        List<QmpClient.UsbDevice> actualDevices = client.listXhciEhciBus0Devices();
        int selectedIndex = deviceList.getSelectedIndex();
        devices.forEach(newList::addItem);
        for (UsbDeviceNode usbDeviceNode : newList.getItems()) {
            if (actualDevices.stream().anyMatch(usbDevice ->
                            usbDevice.getProductId() == usbDeviceNode.getProductId()
                            && usbDevice.getVendorId() == usbDeviceNode.getVendorId())) {
                newList.setChecked(usbDeviceNode, true);
            }
        }
        if (newList.getItems().contains(selectedItem)) {
            newList.setSelectedIndex(deviceList.indexOf(selectedItem));
        } else {
            newList.setSelectedIndex((newList.getItems().size() >= selectedIndex + 1)
                    ? selectedIndex : newList.getItems().size() - 1);
        }
        addListenerToCheckBoxList(newList);
        deviceList = newList;
        window.setComponent(createPanel(deviceList, box));
    }

    private Panel createPanel(Component top, Component bottom) {
        Panel panel = new Panel(new LinearLayout(Direction.VERTICAL));
        Panel topPanel = new Panel();
        topPanel.addComponent(top);
        panel.addComponent(topPanel);
        // TODO: Uncomment and configure log4j to append logs to this.
//        panel.addComponent(bottom.withBorder(Borders.singleLine("Logs")));
        return panel;
    }

    private void addListenerToCheckBoxList(CheckBoxList<UsbDeviceNode> list) {
        list.addListener((itemIndex, checked) -> {
            UsbDeviceNode device = list.getItemAt(itemIndex);
            try {
                if (checked) {
                    client.addDevice(device.getVendorId(), device.getProductId(), device.isUSB3());
                } else {
                    client.delDevice(device.getVendorId(), device.getProductId());
                }
            } catch (Exception e) {
                log.error("Uncaught exception while managing device {}", device.getName(), e);
            }
        });

    }

    public void start() {
        textGUI.addWindowAndWait(window);
    }

}
