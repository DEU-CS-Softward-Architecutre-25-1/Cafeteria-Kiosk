package common.network.handler.listener;

import common.network.packet.EncryptCompleteS2CPacket;
import common.network.packet.HelloS2CPacket;
import common.network.packet.UpdateDataPacket;
import common.network.packet.VerifyPurchasePackets;

/**
 * <code>Client</code>에서 핸들링되는 패킷들을 정의한 인터페이스이다. 해당 인터페이스의 실 구현부는 <code>client</code> 모듈에 있음을
 * 유의하라. 즉 서버와 common에서는 이 실 구현부를 확인할 수 없다.
 */
public interface ClientPacketListener extends PacketListener {
    void onHello(HelloS2CPacket packet);
    void onReceivedData(UpdateDataPacket.ResponseDataS2CPacket packet);
    void onEncryptCompleted(EncryptCompleteS2CPacket packet);
    void onVerifyPurchaseResult(VerifyPurchasePackets.VerifyPurchaseResultS2CPacket packet);
}
