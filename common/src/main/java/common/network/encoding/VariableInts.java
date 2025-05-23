package common.network.encoding;

import io.netty.buffer.ByteBuf;

/**
 * Variable-Int 는 가변 가능한 32비트의 정수형을 핸들링 하는 클래스입니다. <br>
 * 사용하는 이유는 각 패킷에 대한 최적화고요. 실제 어떻게 작동되는지는 메소드마다 작성하겠습니다. <br>
 * 해당 프로젝트에서까지 쓸 정도의 최적화 기법은 아닌데, 그냥 써보고 싶어서 씁니다.
 */
public class VariableInts {
    /**
     * 총 몇 바이트까지 읽을지입니다. 각 바이트에서 MSB는 다음 바이트가 존재하는지에 대해 체크하는데, 그렇기 때문에 각 바이트<code>(8비트)</code>중 <code>7비트</code>만 사용 가능합니다.<br>
     * 그렇기 떄문에 <code>7*5 비트</code>를 읽을 수 있고, 대충 <code>32비트</code> 정수형을 담기에는 충분한 바이트 공간입니다.
     */
    private static final int MAX_BYTES = 5;
    /**
     * 127은 2진법으로 <code>01111111</code> 입니다. 이걸 <code>AND</code> 연산을 돌려버리면 MSB 는 버리고 나머지 진수를 얻을 수 있게 됩니다.
     */
    private static final int DATA_BITS_MASK = 127;
    /**
     * 128은 2진법으로 <code>10000000</code> 입니다. 이걸 <code>AND</code> 연산을 때려서 <code>128</code>이 나오면 다음 비트가 있다는 거겠죠.
     */
    private static final int MORE_BIT_MASK = 128;
    /**
     * 8비트 중 MSB를 뺀 유효 비트가 몇 비트인지 정의해둔 필드입니다.
     */
    private static final int DATA_BITS_PER_BYTE = 7;

    /**
     * 해당 바이트의 사이즈, 즉 해당 값의 가변 크기를 반환합니다. <br>
     * 우선 -1을 7비트 왼쪽 쉬프트를 합니다. 이 경우 32비트 정수형은 7개의 필드는 0으로, 나머지는 1이 될 겁니다. (기억 안나면 2의 보수 다시 배우고 오세요.) <br>
     * 그렇게 된 상태에서, i가 7비트에 모두 들어가는 수 (<= 127) 라면,AND 연산을 하면 0이 나올 것 입니다. ( 마지막 7비트가 <code>0000000</code> 이기때문 ) <br>
     * 이 때 0이 나오면 7비트로 표현이 가능한 수고, 0이 아니라면 상위비트가 존재하는 것 입니다. (<code>1 & 1</code> 은 <code>1</code> 임으로. )
     * @param i 대상 정수
     * @return 몇 개의 바이트로 표현할 수 있는지를 나타냅니다. 5비트보다 클 경우, 5비트로 리턴합니다.
     */
    public static int getSizeInBytes(int i) {
        for (int j = 1; j < MAX_BYTES; j++) {
            if ((i & -1 << j * DATA_BITS_PER_BYTE) == 0) {
                return j;
            }
        }

        return MAX_BYTES;
    }

    /**
     * 바이트의 MSB를 체크합니다. MSB가 1이라면 다음 비트도 값이 있는걸로 간주합니다.
     * @param b 바이트
     * @return 다음 바이트가 존재하는지 여부.
     */
    public static boolean shouldContinueRead(byte b) {
        return (b & MORE_BIT_MASK) == MORE_BIT_MASK;
    }

    /**
     * 바이트를 쭉 읽어 32비트 정수형으로 변환합니다.<br>
     *
     * @param buf
     * @return
     */
    public static int read(ByteBuf buf) {
        int i = 0;
        int j = 0;

        byte b;

        do {
            b = buf.readByte();
            i |= (b & DATA_BITS_MASK) << j++ * DATA_BITS_PER_BYTE;
            if (j > MAX_BYTES) {
                throw new RuntimeException("Too many bytes");
            }
        } while (shouldContinueRead(b));

        return i;
    }

    /**
     * 비트 밀고 적고
     * @param buf
     * @param i
     * @return
     */
    public static ByteBuf write(ByteBuf buf, int i) {
        while ((i & -MORE_BIT_MASK) != 0) {
            buf.writeByte(i & DATA_BITS_MASK | MORE_BIT_MASK);
            i >>>= DATA_BITS_PER_BYTE;
        }

        buf.writeByte(i);
        return buf;
    }
}
