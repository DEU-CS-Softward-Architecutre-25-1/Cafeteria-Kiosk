package common.network.packet;

import common.network.handler.listener.PacketListener;

public interface SidedPacket<T extends PacketListener> extends Serializable<SidedPacket> {
    Side getSide();

    /**
     * 해당 패킷을 받은 클라이언트 / 서버가 실행하는 메소드입니다.
     */
    void apply(T listener);
    enum Side {
        SERVER, CLIENT
    }

    @Override
    default SidedPacket getValue() {
        return this;
    }
}
