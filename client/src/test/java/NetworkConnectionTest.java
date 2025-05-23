import common.network.packet.HandShakeC2SInfo;
import dev.qf.client.Main;
import org.junit.jupiter.api.Assertions;
import org.testng.annotations.Test;

public class NetworkConnectionTest {
    @Test
    public void testNetworkConnection() throws InterruptedException {
        Main.INSTANCE.run();
        var future = Main.INSTANCE.sendSerializable(new HandShakeC2SInfo("test"));

        while(!future.isDone()) {
            Thread.sleep(100);
        }
        Assertions.assertTrue(future.isSuccess());

        Main.INSTANCE.shutdown();
    }
}
