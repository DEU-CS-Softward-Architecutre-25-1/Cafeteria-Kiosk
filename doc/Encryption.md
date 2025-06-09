# Encryption (암호화)

암호화는 TLS 공개 키 암호화 방식을 수정한 방식을 사용한다. 기존 TCP-TLS 암호화 방식은 다음과 같다.

<div style="text-align: center"><img src="https://cf-assets.www.cloudflare.com/slt3lc6tev37/5aYOr5erfyNBq20X5djTco/3c859532c91f25d961b2884bf521c1eb/tls-ssl-handshake.png" width = "384" height="294"></div>

이러한 방식은 키오스크-서버 연결간 인증서와 같은 요소까지는 필요 없다 판단되어 일부 수정하였다. 이에 따른 방식은 다음과 같다.

<div style="text-align: center"><img src="kiosk_cafeteria_encryption.png" width = "384" height="294"></div>

## 1. Client HandShake Start
클라이언트에서는 첫 연결 후 HandShake 패킷을 보낸다. 해당 패킷은 서버와 연결 프로토콜이 준비되었으며, 이제부터 암호화 로직을 시작하겠다는 일련의 신호이다.

```java
// In HandShakeC2SInfo.java
public record HandShakeC2SInfo(String id) implements SidedPacket<ServerPacketListener> {
    public static final Codec<HandShakeC2SInfo> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.STRING.fieldOf("id").forGetter(HandShakeC2SInfo::id)
            ).apply(instance, HandShakeC2SInfo::new)
    );
    //...
}
```

해당 코드는 클라이언트가 서버로 전달하는 HandShake 패킷의 일부분이다. 해당 클래스에는 1개의 String 타입 필드가 존재한다. 이를 통해 서버는 해당 클라이언트의 식별자를 확인한다.

## 2. Server Hello Packet

`HandShakeC2SPacket`을 받은 서버는 서버에 있는 `KeyPair`를 가져온다. 이 `KeyPair`는 서버 실행 중 전역적으로 존재하며, 서버가 재시작이 될 경우 새로운 `KeyPair`가 할당된다. `KeyPair`에는 `PrivateKey` 와 `PublicKey`가 한 쌍으로 존재한다.

서버는 이 중 `PublicKey`와 `Nonce`를 래핑해서 클라이언트에 전송한다.

공개키 암호화 방식은 [여기](https://namu.wiki/w/%EA%B3%B5%EA%B0%9C%ED%82%A4%20%EC%95%94%ED%98%B8%ED%99%94%20%EB%B0%A9%EC%8B%9D)를 참고하라.

다음은 `KeyPair`를 생성하는 코드이다. `RSA-1024`를 사용한다.

```java
    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(1024);
            return keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
```

### 2.1. PublicKey
`PublicKey`는 서버가 클라이언트에게 배포하는 암호키이다. 이를 통해 클라이언트는 데이터를 암호화 할 수 있으며, 암호화된 데이터는 `PublicKey`의 쌍인 `PrivateKey`를 통해 복호화 할 수 있다.

### 2-2. PrivateKey
`PrivateKey`는 서버에서 계속 가지고 있는 암호화 키이다. 서버는 **절대로** 클라이언트나 외부로 이 키를 유츌하면 안된다.

`PrivateKey`는 상술 한 것 처럼 PublicKey로 암호화된 데이터를 복호화 하는데 사용된다. 다시 말하지만 **이를 외부로 절대 유출하면 안되며, 이에 대한 가능성이 있는 모든 작업에 대해서 신중히 진행해야 한다.**

### 2-3. Nonce
[Nonce](https://en.wikipedia.org/wiki/Cryptographic_nonce)는 서버와 클라이언트에서 랜덤으로 생성되는 Integer 타입의 데이터다. 이러한 값을 전달함을 통해 서버는 해당 데이터가 한번만 사용되었다는 것을 보증한다.

이를 통해 [리플레이 공격](https://en.wikipedia.org/wiki/Cryptographic_nonce)을 예방할 수 있다.

### 2-4. Wrapping 및 전송

```java
// In HelloS2CPacket
public final class HelloS2CPacket implements SidedPacket<ClientPacketListener> {
    //...
    private final byte[] publicKey;
    private final byte[] nonce;
    //...
}

```

서버는 `HelloS2CPacket`에 Nonce와 PublicKey를 전달받아 전송한다.

## 4. SecretKey 생성

```java
    /**
     * Client가 처음으로 Server 와 연결 된 이후, Client 가 Server에게 HandShakeC2SPacket 을 보낼 떄 서버가 callback 으로 전달하는 패킷이다.<br>
     * 해당 패킷에는 서버의 public 키가 암호화 된 상태로 들어있다. <br>
     * 또한 nonce 역시 존재한다. Client 에서는 nonce를 사용할 일이 딱히 없으나, 클라이언트가 서버로 같은 nonce 값을 보내지 않으면 연결이 거절된다.
     * @param packet
     */
    @Override
    public void onHello(HelloS2CPacket packet) {
        SecretKey secretKey = NetworkEncryptionUtils.generateSecretKey();
        PublicKey publicKey = packet.getPublicKey();

        Cipher encrpytionCipher = NetworkEncryptionUtils.cipherFromKey(Cipher.ENCRYPT_MODE, secretKey);
        Cipher decryptionCipher = NetworkEncryptionUtils.cipherFromKey(Cipher.DECRYPT_MODE, secretKey);

        KeyC2SPacket secretPacket = new KeyC2SPacket(secretKey, publicKey, packet.nonce());
        logger.info("Client public key sent");
        this.handler.send(secretPacket);
        this.handler.encrypt(encrpytionCipher, decryptionCipher);
    }
```

현 시점에서 클라이언트가 가지고 있는 데이터는 `PublicKey`와 `Nonce`이다.

이 프로젝트는 양방향 통신이기 때문에 클라이언트에서 데이터를 받을 경우 이를 복호화 하는 기능도 존재해야하는데, `PrivateKey`를 또 만들어서 보낼 수도 있겠으나, 그렇게 좋은 방법은 아니다.

이러한 상황에서 우리는 암호화 / 복호화가 가능한 대칭키를 만들어 암호화를 할 것이다.

대칭키를 방법은 다음과 같다.

```java
    /**
     * AES-128 알고리즘을 통해 비밀 키를 생성한다. 이러한 비밀 키는 암호화에 사용된다.
     * @return 비밀 키
     */
    public static SecretKey generateSecretKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(128);
            return keyGenerator.generateKey();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
```

이후 클라이언트는 [AES-128](https://namu.wiki/w/AES) 알고리즘 기반의 대칭키 `SecretKey` 를 생성한다. 이러한 대칭키는 서버와 클라이언트가 서로 가지고 있어야 하기 때문에 서버로 전송해야한다.

## 5. SecretKey 전송

다만 이를 평문으로 보낼 수는 없다. 공격자가 패킷을 캡쳐하여 비밀 키를 얻을 수 있기 때문이다. 이러한 이유로 클라이언트는 `Nonce`와 `SecretKey`를 암호화 한다.

`SecretKey`와 `Nonce`는 서버에서 전달받은 `PublicKey`를 통해 암호화 한다.

```java
public class KeyC2SPacket implements SidedPacket<ServerPacketListener> {
    //...
        public KeyC2SPacket(SecretKey secretKey, PublicKey publicKey, byte[] nonce) {
        this.nonce = NetworkEncryptionUtils.encrypt(publicKey, nonce);
        this.encryptedSecretKey = NetworkEncryptionUtils.encrypt(publicKey, secretKey.getEncoded());
        KioskLoggerFactory.getLogger().info("Encrypted nonce: {}", this.nonce);
    }
    //...
}
```

상술한 내역에서 `nonce`는 32비트의 정수형이라고 했다. 여기서 `nonce`가 `byte[]` 인 이유는 `integer` 는 4개의 어레이를 가진 `byte` 와 같기 때문이다.

`PublicKey`를 통해 `SecretKey`와 `Nonce`를 암호화 한다. 이렇게 암호화 된 바이트 어레이는 `PrivateKey`를 가진 서버만이 복호화 할 수 있음으로 안전성이 보장된다.

## 6. 클라이언트 핸들러 암호화

클라이언트는 SecretKey를 발급받았기 때문에 이제 암호화를 할 수 있다. 클라이언트는 SecretKey를 이용하여 암호화 / 복호화를 담당하는 Cipher를 생성한다.

```java
// on onHello(HelloS2CPacket)
//...
        Cipher encrpytionCipher = NetworkEncryptionUtils.cipherFromKey(Cipher.ENCRYPT_MODE, secretKey);
        Cipher decryptionCipher = NetworkEncryptionUtils.cipherFromKey(Cipher.DECRYPT_MODE, secretKey);
//...
```
`NetworkEncryptionUtils.cipherFromKey(int, Key)`는 다음과 같다.

```java
    public static Cipher cipherFromKey(int opMode, Key key) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CFB8/NoPadding");
            cipher.init(opMode, key, new IvParameterSpec(key.getEncoded()));
            return cipher;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

```

`Cipher.getInstance(String)` 에 들어간 인수는 [자바 보안 알고리즘 표준 명칭](https://docs.oracle.com/en/java/javase/11/docs/specs/security/standard-names.html)을 참조하라.

이렇게 만들어진 2개의 Cipher는 `PacketEncryptionManager`
래퍼 클래스를 통해 암호화 / 복호화한다.
이러한 `PacketEncryptionManager`는 [PacketEncryptor](/common/src/main/java/common/network/encryption/PacketEncryptor.java)와 [PacketDecryptor](/common/src/main/java/common/network/encryption/PacketEncryptor.java) 에서 생성되며, 다른 인스턴스에서는 생성되지 않는다.

다음은 [SerializableHandler](/common/src/main/java/common/network/handler/SerializableHandler.java)에 있는 `encryption(Cipher, Cipher)` 메소드이다. 해당 메소드를 통해 파이프라인에 암호화와 복호화 로직을 적용한다.
```java
    public void encrypt(Cipher encryptionCipher, Cipher decryptionCipher) {
        this.encrypted = true;
        this.channel.pipeline().addBefore("splitter", "decrypt", new PacketDecryptor(decryptionCipher));
        this.channel.pipeline().addBefore("prepender", "encrypt", new PacketEncryptor(encryptionCipher));
    }
```

이제부터 이러한 SecretKey로 만들어낸 Cipher를 통해 어떻게 암호화 되는지에 대한 내용을 작성한다.

```java
// in PacketEncryptionManager
    protected ByteBuf decrypt(ChannelHandlerContext ctx, ByteBuf buf) throws ShortBufferException {
        int i = buf.readableBytes();
        byte[] bs = this.toByteArray(buf);
        ByteBuf byteBuf = ctx.alloc().heapBuffer(this.cipher.getOutputSize(i));
        int bytesWritten = this.cipher.update(bs, 0, i, byteBuf.array(), byteBuf.arrayOffset());
        if (bytesWritten < 0 || bytesWritten > byteBuf.capacity()) {
            throw new IllegalStateException("Invalid number of bytes written by cipher.update: " + bytesWritten);
        }
        byteBuf.writerIndex(bytesWritten);
        return byteBuf;
    }

    protected void encrypt(ByteBuf buf, ByteBuf result) throws ShortBufferException {
        int byteSize = buf.readableBytes();
        byte[] bs = this.toByteArray(buf);
        int outputSize = this.cipher.getOutputSize(byteSize);

        if (this.encryptionBuffer.length < outputSize) {
            this.encryptionBuffer = new byte[outputSize];
        }

        result.writeBytes(this.encryptionBuffer, 0, this.cipher.update(bs, 0, byteSize, this.encryptionBuffer));
    }
```

하나씩 보겠다. 암호화 하는 과정을 먼저 살펴보자.

```java
    protected void encrypt(ByteBuf buf, ByteBuf result) throws ShortBufferException {
        int byteSize = buf.readableBytes();
        byte[] bs = this.toByteArray(buf);
        int outputSize = this.cipher.getOutputSize(byteSize);

        if (this.encryptionBuffer.length < outputSize) {
            this.encryptionBuffer = new byte[outputSize];
        }

        result.writeBytes(this.encryptionBuffer, 0, this.cipher.update(bs, 0, byteSize, this.encryptionBuffer));
    }
```

해당 암호화는 데이터를 ByteBuffer로 변환한 후 실행된다.

1. ByteBuffer의 byte 수를 얻는다.
2. ByteBuffer를 Byte Array로 변환한다.
3. Cipher를 통해 해당 byte 만큼을 암호화 하면 몇 바이트가 되는지를 얻는다.
4. 3에서 얻은 바이트 수를 통해 암호화된 byte array를 임시로 저장할 공간을 확인한다. 부족하다면 추가 할당한다.
5. 임시로 저장할 byte array에 암호화한 byteArray를 담는다.
6. 이후 이 byte array를 통해 전송할 byte buffer에 데이터를 담는다.

복호화 과정은 다음과 같다.

```java
    protected ByteBuf decrypt(ChannelHandlerContext ctx, ByteBuf buf) throws ShortBufferException {
        int i = buf.readableBytes();
        byte[] bs = this.toByteArray(buf);
        ByteBuf byteBuf = ctx.alloc().heapBuffer(this.cipher.getOutputSize(i));
        int bytesWritten = this.cipher.update(bs, 0, i, byteBuf.array(), byteBuf.arrayOffset());
        if (bytesWritten < 0 || bytesWritten > byteBuf.capacity()) {
            throw new IllegalStateException("Invalid number of bytes written by cipher.update: " + bytesWritten);
        }
        byteBuf.writerIndex(bytesWritten);
        return byteBuf;
    }
```

1. 전달받은 바이트 버퍼의 바이트 수를 얻는다.
2. ByteBuffer를 Byte array로 변환한다.
3. 변환된 Byte Array를 통해 복호화 되었을 경우의 Byte 수를 확인한다.
4. 이를 통해 새로운 ByteBuffer를 생성하고 크기를 할당한다.
5. byte buffer 에 있는 byte array에 데이터를 복호화해 넣는다. 이 때 `bytesWritten`은 이렇게 작성된 실 바이트 수를 표시한다.<br>
    5-1. 만약 예상했던 바이트 보다(할당된 바이트 보다) 더 많은 바이트가 변환됐거나, 바이트 수가 0보다 작으면(에러) Exception 을 발생시킨다.
6. ByteBuffer에 byteWriterIndex 를 설정한다.

이러한 방식으로 클라이언트 / 서버는 암호화 복호화 하게 되며, 지금 상황에서는 클라이언트만 암호화 파이프러인이 생성되어있기 때문에 서버도 이러한 방식을 통해 암호화 파이프라인을 형성해야한다.


## 7. 서버 암호화

```java
    public void onKeyReceived(KeyC2SPacket packet) {
        try {
            KioskNettyServer server = (KioskNettyServer) handler.connection;
            PrivateKey privateKey = server.getKeyPair().getPrivate(); // (1)
            if (!packet.verifySignedNonce(this.nonce, privateKey))  { //(2)
                throw new IllegalStateException("Invalid nonce");
            }

            SecretKey secretKey = packet.decryptSecretKey(privateKey);
            Cipher encryptCipher = NetworkEncryptionUtils.cipherFromKey(Cipher.ENCRYPT_MODE, secretKey);
            Cipher decryptCipher = NetworkEncryptionUtils.cipherFromKey(Cipher.DECRYPT_MODE, secretKey);

            this.handler.encrypt(encryptCipher, decryptCipher);
            this.handler.send(new EncryptCompleteS2CPacket(System.currentTimeMillis()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // in KeyC2SPacket
    // (3)
        public boolean verifySignedNonce(byte[] nonce, PrivateKey privateKey) {
        try {
            KioskLoggerFactory.getLogger().info("Verifying nonce... : {}", NetworkEncryptionUtils.decrypt(privateKey, this.nonce));
            return Arrays.equals(nonce, NetworkEncryptionUtils.decrypt(privateKey, this.nonce));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    // (4)
    public SecretKey decryptSecretKey(PrivateKey privateKey) {
        return NetworkEncryptionUtils.decryptSecretKey(privateKey, this.encryptedSecretKey);
    }
```

이제부터는 서버사이드 암호화 로직을 살펴볼 것이다. 현재 서버에서 받은 데이터는 `PublicKey`로 암호화된 인코딩된 `SecretKey`와 암호화된 `Nonce`이다.

서버는 `PublicKey`의 쌍인 `PrivateKey`를 가지고 있기 때문에 이를 복호화 할 수 있다.

서버는

1. 서버의 PrivateKey를 가져온다.
2. privateKey를 통해 nonce를 복호화 하고, 올바른지 확인한다. <br>
    2-2. 만약 nonce가 맞지 않다면 Exception을 발생시킨다. 이 Exception이 발생 시 서버와 클라이언트의 연결은 끊어진다.
3. 이후 privateKey를 통해 SecretKey를 복호화 한다.
4. 이후 내역은 위와 동일하다. 클라이언트와 같은 방식으로 암호화를 파이프라인에 추가한다.
5. 암호화가 완료되면 암호화 테스트용 암호화 완료 패킷을 전달한다.

이후 암호화 설정은 종료된다.

