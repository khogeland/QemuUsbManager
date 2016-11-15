package hoge.land.qemuusb.qmp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.anarres.qemu.qapi.common.QApiCommand;
import org.anarres.qemu.qapi.common.QApiResponse;

import javax.annotation.Nonnull;

/**
 * Mostly copied from {@link org.anarres.qemu.qapi.api.DeviceDelCommand} because there is a
 * device_del class, but no device_add class (?). Specific to USB devices here.
 */

// QApiCommandDescriptor{name=device_del, returns=null, data={id=str}}
public class UsbDeviceAddCommand extends QApiCommand<UsbDeviceAddCommand.Arguments, UsbDeviceAddCommand.Response> {
    /**
     * Compound arguments to a UsbDeviceAddCommand.
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class Arguments {

        @JsonProperty("driver")
        @Nonnull
        public String driver;

        @JsonProperty("vendorid")
        @Nonnull
        public int vendorId;

        @JsonProperty("productid")
        @Nonnull
        public int productId;

        @JsonProperty("bus")
        @Nonnull
        public String bus;

        public Arguments() {
        }

        public Arguments(String driver, short vendorId, short productId, String bus) {
            this.driver = driver;
            // QMP accepts unsigned 32-bit integers
            this.vendorId = vendorId & 0xffff;
            this.productId = productId & 0xffff;
            this.bus = bus;
        }
    }

    /**
     * Response to a UsbDeviceAddCommand.
     */
    public static class Response extends QApiResponse<Void> {
        // I don't think this returns anything...?
        // TODO: Check QAPI source and see if this returns anything.
    }

    /**
     * Constructs a new UsbDeviceAddCommand.
     */
    public UsbDeviceAddCommand(@Nonnull UsbDeviceAddCommand.Arguments argument) {
        super("device_add", Response.class, argument);
    }

    /**
     * Constructs a new UsbDeviceAddCommand.
     */
    public UsbDeviceAddCommand(String driver, short vendorId, short productId, String bus) {
        this(new Arguments(driver, vendorId, productId, bus));
    }
}
