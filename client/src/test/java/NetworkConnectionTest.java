import common.Option;
import common.OptionGroup;
import common.OrderItem;
import common.network.Connection;
import common.network.packet.HandShakeC2SInfo;
import common.registry.RegistryManager;
import common.util.Container;
import dev.qf.client.Main;
import dev.qf.client.network.KioskNettyClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

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
        Map<OptionGroup, Option> optionMap = new HashMap<>();
        optionMap.put(RegistryManager.OPTION_GROUPS.get(0), RegistryManager.OPTIONS.get(0));
        OrderItem item = new OrderItem(RegistryManager.MENUS.get(0), optionMap, 1);


    }
}
