# Serializable (Packet)

[Serializable](/common/src/main/java/common/network/packet/Serializable.java)은 해당 프로젝트에서 네트워크킹의 한 단위이다.
즉 송수신되는 모든 데이터는 Serializable 클래스이며 데이터 인스턴스 또한 이러한 Serializable 클래스에 래핑되어 전송된다.

## Serializable 과 Packet
Serializable 은 Json -> ByteArray -> byteBuffer 식으로 변환된다. 이러한 형태로 인해, class 를 Json 으로 변환하고, 이를 string 으로 바꿔 ByteArray로 바꿔주는 작업이 존재해야하는데, 이 중 Class2Json(Json2Class)의 역할을 하는 것이 Mojang 에서 개발한 DataFixerUpper 내에 있는 Codec 이다.

Codec은 마인크래프트에서 사용되는 Netty 라이브러리를 이용한 통신과 IO 를 위해 쓰인다.

마인크래프트에서는 Codec을 통해 클래스를 직접적으로 ByteArray 로 변환하지만, 우리는 아직 이렇게 했을 때 디버깅에 대한 지식이 적을것으로 보여 Json 으로 바꾸는 방식으로 이용하였다.

## Codec 의 선언

Mojang의 DataUpperFixer에는 POJO 에 대한 [코덱들이 존재한다.](https://kvverti.github.io/Documented-DataFixerUpper/snapshot/com/mojang/serialization/Codec)

이를 통해 코덱을 인코딩, 디코딩 할 수 있다.

예를 들어 Int 타입의 수를 Json 으로 바꾸자 할 때는

```
Logger LOGGER = LoggerFactory.getLogger();
int i = 5;
DataResult<JsonElement> result = Codec.INT.encodeStart(JsonOps.INSTANCE, i);

Optional<JsonElement> optionalJson = result.resultOrPartial(LOGGER::error);

JsonElement json = optionalJson.orElseThrow();

```

의 식으로 가능하다.

역의 경우에는

```
DataResult<Pair<Integer, JsonElement>> result = Codec.INT.decode(JsonOps.INSTANCE, json)

Optional<Pair<Integer, JsonElement>> optionalPair = result.resultOrParitial(System::err::println);

Pair<Integer, JsonElement> pair = optionalPair.orElseThrow();

int j = pair.getFirst();
```

의 방식으로 가능하다.

이러한 Codec 에 대한 Serialization에 대한 예시는 [여기](/common/src/test/java/CodecTest.java)를 참고하라.

## Custom Codec
상술한 POJO를 통해 여러 추가적인 클래스도 구현 가능하다.

### Record Codec
Record Codec 은 Java Beans 와 같은 데이터 타입을 생성할 때 매우 용이하다.

다음의 코드는 HandShakeC2SPacket의 코덱이다.

```java
    public static final Codec<HandShakeC2SInfo> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.STRING.fieldOf("id").forGetter(HandShakeC2SInfo::id)
            ).apply(instance, HandShakeC2SInfo::new)
    );
```

RecordCodecBuilder는 `Instance<T>`를 통하여 `T`를 반환하는 함수를 인자로 받는다.

또한 `instance.group(RecordCodecBuilder..)`가 존재하는데, 최대 16개의 인수를 얻을 수 있다.

이렇게 받은 인수를 통하여 `.apply(Instance<T>, Function<T1...T16, T>`를 인수로 받아 `T`를 반환한다.

해당 예시에서는 `.apply(Instance<HandShakeC2SPacket>, Function<String, HandShakeC2SPacket>)`의 형태가 된다.

`Codec.STRING.fieldOf("id").forGetter(HandShakeC2SInfo::id)`를 보자. 여기서 `Codec.STRING`은 기본 제공되는 `POJO` 타입의 코덱이라고 했다.그렇다면 `fieldOf()`는 무엇일까?

`fieldOf(String)`은 해당 코덱을 `MapCodec`으로 변환한다. 이러한 MapCodec은 `Key-Value`값을 가진 코덱이다. 이는 Json 등에서 사용되며, 이러한 부분이 필요하지 않을 경우에서는 사용을 하지 않아도 무관하다.

`forGetter()`는 특정 데이터를 Getter를 통해 제공한다.

이를 JSON 으로 변환한다면

```json
{
    "id" : "test"
}
```

식으로 변환 될 것이다.

## ExternalDataManager
[ExternalDataManager](/server/src/main/java/dev/qf/server/database/ExternalDataManager.java)는 외부 저장 수단과 이에 대한 규약을 선언한 인터페이스이다.

```java
public interface ExternalDataManager {
    default void loadAll() {
        RegistryManager.entries().forEach(this::loadSpecificRegistry);
    }
    default void saveAll() {
        RegistryManager.entries().forEach(this::saveSpecificRegistry);
    }
    default void close() {
        this.saveAll();
        internalClose();
    }
    void internalClose();
    void initialize();
    void loadSpecificRegistry(Registry<?> registry);
    void saveSpecificRegistry(Registry<?> registry);
    void removeSpecificRegistry(Registry<?> registry, String targetId);
    void saveSpecificRegistry(Registry<?> registry, SynchronizeData<?> data);
}
```

모든 IO를 이용하는 저장 수단들은 해당 인터페이스를 통해 구현되어야 한다.


### LocalJsonStorage

[LocalJsonStorage](/server/src/main/java/dev/qf/server/database/LocalJsonStorage.java)는 모든 데이터를 Json 형식으로 로컬파일에 저장하는 형태이다.

LocalJsonStorage 는 NIO를 사용하여 저장/삭제를 담당한다.

### SQLiteStorage

[SQLiteStorage](/server/src/main/java/dev/qf/server/database/SQLiteStorage.java)는 Sqlite를 이용하는 저장 스토리지이다.
SQLite 를 핸들링 하기 위해 SQLib이라는 라이브러리를 사용하며, 스키마를 구성할 필요 없이 DB에 데이터가 저장된다.


