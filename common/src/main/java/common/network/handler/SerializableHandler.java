package common.network.handler;

import common.util.KioskLoggerFactory;
import common.network.Connection;
import common.network.Serializable;
import common.network.encryption.PacketDecryptor;
import common.network.encryption.PacketEncryptor;
import common.network.handler.factory.PacketListenerFactory;
import common.network.handler.listener.PacketListener;
import common.network.packet.SidedPacket;
import common.util.Container;
import io.netty.channel.*;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import javax.crypto.Cipher;

/**
 * Client, Server 에 모두 사용되는 Handler 클래스이고, 싱글톤으로 작동하는 클래스가 아닌지 생각이 들 수도 있곘지만 아님을 유의하라. <br>
 * 해당 핸들러는 소켓과 바인드 될 때 생성된다. 이 뜻은 해당 시스템의 클라이언트에서는 한개의 핸드러만 존재할 수 있으나, 서버는 아니다. <br>
 * 서버는 여러 클라이언트와 연결될 수 있음을 유의하라. 이 경우 서버는 각 클라이언트 마다 handler 를 가지게 된다. <br>
 * 이러한 이유로 {@link PacketListener} 도 싱글톤이 아니다.<br>
 * 또한 모든 핸들링은 비동기로 진핼됨을 유의하라.
 */
public class SerializableHandler extends SimpleChannelInboundHandler<SidedPacket> {
    public final Connection connection = Container.get(Connection.class);
    private static final Logger LOGGER = KioskLoggerFactory.getLogger();
    @Nullable
    private volatile PacketListener packetListener;
    public SerializableHandler(SidedPacket.Side side) {
        this.side = side;
    }
    private boolean encrypted = false;
    private final SidedPacket.Side side;
    @Nullable
    public volatile Channel channel;
    private volatile String id;

    public boolean isEncrypted() {
        return encrypted;
    }

    public boolean isOpened() {
        return channel != null && channel.isOpen();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SidedPacket s) throws Exception {
        LOGGER.info(s.toString());
        if (s.getSide() == this.side) { // 이게 그렇게 쓸모 있는 코드가 아닌 것 같지만 아무튼.
            s.apply(packetListener);
        } else {
            LOGGER.warn("received a packet that is not a side packet : {}", s);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error(cause.getMessage(), cause);
        ctx.close();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        this.channel = ctx.channel();
        this.packetListener = Container.get(PacketListenerFactory.class).getPacketListener(this);
        this.connection.onEstablishedChannel(ctx, this);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        this.connection.handleDisconnect(ctx, this);
    }

    public void encrypt(Cipher encryptionCipher, Cipher decryptionCipher) {
        this.encrypted = true;
        this.channel.pipeline().addBefore("splitter", "decrypt", new PacketDecryptor(decryptionCipher));
        this.channel.pipeline().addBefore("prepender", "encrypt", new PacketEncryptor(encryptionCipher));
    }

    /**
     *
     * @param packet
     * @return
     */
    @Nullable
    public ChannelFuture send(Serializable<?> packet) {
        if (this.channel != null && this.channel.isOpen()) {
            this.channel.writeAndFlush(packet);
        }
        return null;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
