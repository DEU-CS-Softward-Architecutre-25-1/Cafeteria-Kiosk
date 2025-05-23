package common.network.encoding;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;

import java.nio.charset.StandardCharsets;

public class StringEncodings {
    public static String decode(ByteBuf buf, int maxLength) {
        int i = ByteBufUtil.utf8MaxBytes(maxLength);
        int j = VariableInts.read(buf);

        if (j > i) {
            throw new DecoderException("The received encoded string buffer length is longer than allowed ( %s > %s )".formatted(j, i));
        } else if (j < 0) {
            throw new DecoderException("ok ok. but why length is lower than zero?");
        } else {
            int k = buf.readableBytes();
            if (j > k) {
                throw new DecoderException("Not enough byte in buffer. expected " + j + ", but got " + k + " bytes");
            } else {
                String string = buf.toString(buf.readerIndex(), j, StandardCharsets.UTF_8);
                buf.readerIndex(buf.readerIndex() + j);
                if (string.length() > maxLength) {
                    throw new DecoderException("The received string length is longer than maximum alowwed (" + string.length() + " > " + maxLength + ")");
                } else {
                    return string;
                }
            }
        }
    }

    public static void encode(ByteBuf buf, CharSequence string, int maxLength) {
        if (string.length() > maxLength) {
            throw new EncoderException("String is too big. was " + string.length() + " > " + maxLength + ")");
        } else {
            int i = ByteBufUtil.utf8MaxBytes(string);
            ByteBuf byteBuf = buf.alloc().buffer(i);
            try {
                int j = ByteBufUtil.writeUtf8(byteBuf, string);
                int k = ByteBufUtil.utf8MaxBytes(maxLength);

                if (j > k) {
                    throw new EncoderException("String is too big. (was" + j + " bytes encoded, max " + k + " bytes)");
                }

                VariableInts.write(buf, j); // size
                buf.writeBytes(byteBuf); // string
            } finally {
                byteBuf.release();
            }
        }
    }
}
