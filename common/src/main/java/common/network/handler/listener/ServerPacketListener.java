package common.network.handler.listener;

import common.network.packet.DataAddedC2SPacket;
import common.network.packet.HandShakeC2SInfo;
import common.network.packet.KeyC2SPacket;
import common.network.packet.UpdateDataPacket;

public interface ServerPacketListener extends PacketListener {
    void onHandShake(HandShakeC2SInfo packet);
    void onRequestData(UpdateDataPacket.RequestDataC2SPacket packet);
    void onKeyReceived(KeyC2SPacket packet);
    void onUpdateReceived(DataAddedC2SPacket packet);
}
