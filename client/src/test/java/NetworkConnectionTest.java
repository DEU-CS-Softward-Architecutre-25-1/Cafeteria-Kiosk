import common.network.Connection;
import common.network.packet.HandShakeC2SInfo;
import common.network.packet.UpdateDataPacket;
import common.util.Container;
import dev.qf.client.CategoryManagementUI;
import dev.qf.client.Main;
import dev.qf.client.network.KioskNettyClient;
import io.netty.channel.ChannelFuture;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class NetworkConnectionTest {
    @Test
    public void testNetworkConnection() throws InterruptedException {
        Main.INSTANCE.run();
        KioskNettyClient client = (KioskNettyClient) Container.get(Connection.class);
        while(!client.isConnected()) {
            Thread.sleep(100);
        }
        var future = Main.INSTANCE.sendSerializable("server", new HandShakeC2SInfo("test"));

        while(client.isConnected() && !client.getHandlers().getFirst().isEncrypted()) {
            Thread.sleep(3000);
        }
//
//        while(true) {
//            Thread.sleep(1000);
//        }
        Thread.sleep(1000);

        ChannelFuture itemRequest = client.sendSerializable(new UpdateDataPacket.RequestDataC2SPacket("all"));
        Thread.sleep(2000);
        Assertions.assertTrue(future.isSuccess());
    }
}
