import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Optional;

public class CodecTest {
    @Test
    public void codecTests() {
        int i = 10;
        DataResult<JsonElement> jsonResult = Codec.INT.encodeStart(JsonOps.INSTANCE, i);
        Optional<JsonElement> jsonElement = jsonResult.resultOrPartial(System.err::println);
        JsonElement element = jsonElement.orElseThrow();

        System.out.println(element);
        DataResult<Pair<Integer, JsonElement>> intResult = Codec.INT.decode(JsonOps.INSTANCE, element);
        Optional<Pair<Integer, JsonElement>> optionalPair = intResult.resultOrPartial(System.err::println);
        Pair<Integer, JsonElement> pair = optionalPair.orElseThrow();
        int j = pair.getFirst();

        Assertions.assertEquals(i, j);
    }

}