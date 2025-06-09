import common.network.Connection;
import common.network.packet.HandShakeC2SInfo;
import common.network.packet.UpdateDataPacket;
import common.registry.RegistryManager;
import common.util.Container;
import common.util.KioskLoggerFactory;
import dev.qf.client.Main;
import dev.qf.client.network.KioskNettyClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

public class NetworkConnectionTest {
    @BeforeAll
    public static void init() throws InterruptedException {
        Main.INSTANCE.run();
        KioskNettyClient client = (KioskNettyClient) Container.get(Connection.class);
        while(!client.isConnected()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        var future = Main.INSTANCE.sendSerializable("server", new HandShakeC2SInfo("test"));

        while(client.isConnected() && !client.getHandlers().getFirst().isEncrypted()) {
            Thread.sleep(3000);
        }
    }

    @Test
    public void testPurchaseSerialization() {
        AtomicBoolean isFrozen = new AtomicBoolean(true);
        UpdateDataPacket.RequestDataC2SPacket packet = new UpdateDataPacket.RequestDataC2SPacket(RegistryManager.ORDERS.getRegistryId());
        KioskNettyClient client = (KioskNettyClient) Container.get(Connection.class);
        var future = client.sendSerializable(packet);

        future.addListener(f -> {
            if (f.isSuccess()) {
                isFrozen.set(false);
            } else {
                f.cause().printStackTrace();
            }
        });
        while(isFrozen.get()) {
            Thread.yield();
        }

        Logger logger = KioskLoggerFactory.getLogger();
        logger.info("Serialized purchase data sent successfully.");
        logger.info("items : {}", RegistryManager.ORDERS.getAll());
    }
}
