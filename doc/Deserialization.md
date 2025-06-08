# Serialization / Deserialization Logic

서버와 클라이언트에서 패킷을 핸들링 하기 위해서는 ByteBuffer를 Java Class로 변경하는 과정이 필요하다.  해당 문서에서는 이를 어떻게 진행하는지에 대해 상세히 서술하려고 한다.

## Encoder
Packet을 어떻게  ByteBuf로 변환하는지에 대한 내용이다.

정확한 로직은 [SerializableEncoder](/common/src/main/java/common/network/handler/SerializableEncoder.java)를 참조하라.

# Encode Step 1 : toJson

Serializable 인터페이스의 내용은 다음과 같다.

```java
public interface Serializable<T> {
    String PACKET_ID_PROPERTY = "packetId";
    String DATA_PROPERTY = "data";

    String getPacketId();
    <A extends T> A getValue();
    default JsonElement toJson() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("packetId", getPacketId());
        JsonElement dataValue = getCodec().encodeStart(JsonOps.INSTANCE, getValue()).getOrThrow();
        jsonObject.add("data", dataValue);

        return jsonObject;
    }
    @NotNull
    Codec<? extends T> getCodec();
}
```

해당 메소드의 toJson()을 확인하라. 해당 메소드를 통해 어떻게 Json으로 변환 될지에 대해 서술된다.

즉 Serializable 은 다음과 같은 Json 포맷의 형식으로 변환된다.

```json
{
    "packetId" : "foo",
    "data" : {
        "_comment_" : "해당 데이터는 각 클래스에 따라 다르다."
    }
}
```

## Step 1-2 : Codec

Mojang에서 제공하는 DataFixerUpper 의 Codec은 인스턴스를 원하는 형태로 변환이 가능한 Codec을 지원한다.

해당 코드는 [HandShakeC2SPacket](/common/src/main/java/common/network/packet/HandShakeC2SInfo.java)에서 발췌하였다.
```java
// in HandShakeC2SPacket
    public static final Codec<HandShakeC2SInfo> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.STRING.fieldOf("id").forGetter(HandShakeC2SInfo::id)
            ).apply(instance, HandShakeC2SInfo::new)
    );
```

이러한 코덱은 인스턴스의 필수 요소들 중 어떠한 것을 저장해야하는지, 그리고 해당 데이터를 어떻게 get 할지에 대해 서술되어있다.

예시로, "id" 필드는 STRING 타입으로 저장되며, `HandShakeC2SPacket.id()` 메소드를 통해 데이터를 가져올 수 있다는 뜻이다.

또한 이러한 데이터들을 lambda 식으로 생성할 수 있다. apply 부분을 풀어쓰면 `.apply(instance, (id) -> {return new Option(id)})`가 된다.

즉 만약 id 가 `test`일 경우, json 형태는
```json
{
    "id" : "test"
}
```

이 된다. 이를 위와 연계하면

```json
{
    "packetId": "하단에 서술한다.",
    "data" : {
        "id" : "test"
    }
}
```

의 형식으로 된다.

## Step 1-3 : 패킷 ID 설정

Serializable의 메소드 중 `getPacketId()`를 주목하라. 해당 PacketId는 각 구현체 패킷들의 id이다.
이러한 PacketId가 필요한 이유는 각 데이터에 맞는 직렬화 로직을 사용해야 하기 때문이다. 전달받는 입장에서 패킷중 어떤 코덱을 사용해야 하는지를 지시한다. 이는 후술할 역직렬화 로직에서 자세히 설명한다.

즉 이 과정을 통합하면 결과 JSON 양식은 다음과 같다.

```json
{
    "packetId" : "handshake_c2s_packet",
    "data" : {
        "id" : "test"
    }
}
```

이제 이를 Byte로 만들 시간이다.

## Step 2 : ) Json to Byte

Json은 당연히 String 으로 변환할 수 있다. 이를 통하여 우리는 이를 byte array로 변환할 것이다.

```java
// in SerializableEncoder.java
    @Override
    protected void encode(ChannelHandlerContext ctx, Serializable<?> msg, ByteBuf out) {
            StringEncodings.encode(out, msg.toJson().toString(), 32267);
            int i = out.readableBytes();
            LOGGER.info("OUT : [{}] -> {} bytes", msg.getPacketId(), i);
    }
```

해당 코드에서는 StringEncodings 를 살펴봐야 한다. `StringEncodings.encode()`의 메소드는 다음과 같다.

```java
// in StringEncodings.java
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
```

해당 코드를 분석해본다.

```java
    if (string.length() > maxLength)
```
원하는 최대 문자열보다 클 경우를 체크한다. 사실 이정도로 큰 데이터를 보낼 일이 없으니 문제는 없다.

```java
int i = ByteBufUtil.utf8MaxBytes(string);
```

해당 코드는 string의 바이트 수를 읽어오는 클래스다. 이를 통해 버퍼를 얼마나 형성해야하는지를 결정한다.

```java
Bytebuf byteBuf = buf.alloc().buffer(i);
```
버퍼의 사이즈를 설정한다.

```java
int j = ByteBufUtil.writeUtf8(byteBuf, string);
int k = ByteBufUtil.utfMaxByte(maxLength);

if (j > k) {
    throw new EncoderException("String is too big. (was" + j + " bytes encoded, max " + k + " bytes)");
}
```

인코딩된 bytebuf이 최대 길이보다 초과하는지 확인한다. 만약 j가 더 크다는 뜻은, 요청한 크기보다 더 큰 바이트가 생성됐다는 뜻이다. 왜 32767 바이트보다 크면 안되는지는 다음 내용에서 작성한다.


```java
VariableInt.write(buf, j);
```

VariableInt는 사이즈가 가변형인 INT 타입이다. VariableInt 는 다음과 같은 형태로 1바이트를 가진다.

`1XXXXXXX / 0XXXXXXX`

만약 MSB가 1이라면, 해당 데이터는 7개의 가변 비트에 저장할 수 없을 정도로 크다는 것을 의미한다. 즉 1바이트를 추가로 읽어야한다.

만약 MSB가 0이라면, 해당 데이터는 7개의 가변 비트에 저장할 수 있다는 것을 의미한다. 즉 더 이상 바이트를 읽지 않아도 된다.

즉 만약 4를 저장한다고 했을 때, INT는 `0000 0000 0000 0004(16)`을 저장하지만, 해당 방식은 `04(16)`을 저장하게 된다. 이를 통해 4바이트를 1바이트로 줄일 수 있다. 대신, 정수형의 최대 값에서는 5바이트를 저장해야한다. (7bit * 5bit = 35bit, 32bit 를 저장하기 위해서 필요.)

즉 해당 ByteBuffer의 1-5바이트는 가변형 INT 타입이 들어가고, 이는 변환된 String 의 바이트 수를 나타낸다.

이후 String 을 인코딩하여 ByteBuffer 에 넣음으로서 ByteBuf의 생성이 완료되고, 전송된다.


## Step 3 : 패킷 디코딩

이제 ByteBuf를 자바 인스턴스로 바꿀 차례다. 조립은 분해의 역순인 것 처럼, 다시 반대로 하면 된다.

```java
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        int i = in.readableBytes();
        if (i != 0) {
            String msg;
            try {
                msg = StringEncodings.decode(in, 32767);
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
                return;
            }
            JsonObject jsonObject = gson.fromJson(msg, JsonObject.class);
            String type = JsonHelper.getString(jsonObject, Serializable.PACKET_ID_PROPERTY);

            if (in.readableBytes() > 0) {
                throw new IOException(
                        "Packet " +
                        type +
                        " was larger than expected; found " +
                        in.readableBytes() +
                        " extra bytes while reading the packet."
                );
            } else {
                Codec<?> codec = SerializableManager.getCodec(type).orElseThrow(() -> new IllegalArgumentException("Unknown packet type: " + type));
                out.add(codec.decode(JsonOps.INSTANCE, jsonObject.get(Serializable.DATA_PROPERTY)).getOrThrow().getFirst());


                LOGGER.info(SerializableManager.SERIALIZABLE_RECEIVED_MARKER, "IN [{}] -> {} bytes", type, i);
            }
        }
    }
```

해당 코드는 SerializableDeserializer의 전신이다. 해당 클래스를 하나씩 확인해보려고 한다.

```java
int i = in.readableBytes();
```
받은 패킷의 바이트 수이다. 해당 byte 가 0이라면, NOP 로 종료한다.

```java
msg = StringEncodings.decode(in, 32767)
```

해당 코드는 상술한 인코딩의 방식대로 String 타입으로 디코딩한다. 자세한 내용은 Encoding 에서 이야기 했음으로 생략한다.

```java
JsonObject jsonObject = gson.fromJson(msg, JsonObject.class);
String type = JsonHelper.getString(jsonObject, Serializable.PACKET_ID_PROPERTY);
```

모든 패킷의 양식은 Json 형식임으로 일반적으로 해당 상황에서 에러가 나지는 않는다. 또한, 상술했듯이 packet_id 가 존재하기 때문에 packetType 에 대한 필드도 일반적으로는 존재한다.

readableByte를 추가로 확인하는 이유는 이러한 패킷 뒤에 추가적인 바이트가 더 있는지 확인하기 위함이다.만약 존재한다면, 패킷이 Corruption 된 것으로 간주하고 Exception을 생성한다.

이후 packetId에 따른 Packet을 가져온다.

```java
    private static final Map<String, Codec<?>> SERIALIZABLE_MAP;

    public static Optional<Codec<? extends SidedPacket>> getCodec(String key) {
        synchronized (SERIALIZABLE_MAP) {
           return Optional.ofNullable(SERIALIZABLE_MAP.get(key));
        }
    }

    static {
        register("handshake_c2s_info", HandShakeC2SInfo.CODEC);
        register("request_data_c2s_packet", UpdateDataPacket.RequestDataC2SPacket.CODEC);
        register("response_data_s2c_packet", UpdateDataPacket.ResponseDataS2CPacket.CODEC);
        register("hello_s2c_packet", HelloS2CPacket.CODEC);
        register("key_c2s_packet", KeyC2SPacket.CODEC);
        register("encrypt_complete_s2c", EncryptCompleteS2CPacket.CODEC);
        register("data_added_s2c_packet", DataAddedC2SPacket.CODEC);
        register("verify_purchase_c2s", VerifyPurchasePackets.VerifyPurchasePacketC2S.CODEC);
    }
```

해당 코드는 SerializationManager 코드의 일부이다.

해당 Codec 을 얻은 다음, 이를 통해 Deserialize 한다.