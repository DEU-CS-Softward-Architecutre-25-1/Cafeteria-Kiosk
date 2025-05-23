package common.network.packet;

import common.network.Serializable;

public interface SidedPacket extends Serializable<SidedPacket> {
    Side getSide();
    void apply();
    enum Side {
        SERVER, CLIENT
    }

    @Override
    default SidedPacket getValue() {
        return this;
    }
}
