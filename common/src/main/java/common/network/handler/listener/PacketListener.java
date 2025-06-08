package common.network.handler.listener;

import common.network.handler.SerializableHandler;
import common.network.packet.SidedPacket;

/**
 * {@link PacketListener}는 각 사이드가 각 패킷 마다 어떻게 핸들링 할지를 정의한다. 즉 상속된 2가지 클래스, {@link ServerPacketListener}와
 * {@link ClientPacketListener}는 각 고유 모듈에서 구현된다. 다만 이러한 인터페이서가 <code>common</code> 에 있는 이유는 Packet에 알맞은 메소드로
 * 리다이렉션 시키기 위함이다.
 */
public interface PacketListener {
    SidedPacket.Side getSide();
    SerializableHandler getHandler();
}
