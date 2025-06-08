package common.util;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.bytes.ByteArrays;
import org.apache.logging.log4j.core.config.plugins.convert.TypeConverters;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class JavaCodecs {
    public static final Codec<Path> PATH = Codec.STRING.xmap(Path::of, Path::toString);
    public static final Codec<Byte[]> BYTE_ARRAY = Codec.BYTE.listOf().xmap(bytes -> bytes.toArray(new Byte[0]), Arrays::asList);
    public static byte[] asByteArray(Byte[] bytes) {
        byte[] byteArray = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            byteArray[i] = bytes[i];
        }

        return byteArray;
    }
    public static Byte[] asBoxingByteArray(byte[] bytes) {
        Byte[] byteArray = new Byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            byteArray[i] = bytes[i];
        }

        return byteArray;
    }
}

