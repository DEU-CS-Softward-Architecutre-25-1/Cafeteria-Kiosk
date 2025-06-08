package common.network.handler.listener;

import common.network.packet.*;

public interface ServerPacketListener extends PacketListener {
    void onHandShake(HandShakeC2SInfo packet);
    void onRequestData(UpdateDataPacket.RequestDataC2SPacket packet);
    void onKeyReceived(KeyC2SPacket packet);
    void onUpdateReceived(DataAddedC2SPacket packet);
    void onRequestVerify(VerifyPurchasePackets.VerifyPurchasePacketC2S packet);
}
