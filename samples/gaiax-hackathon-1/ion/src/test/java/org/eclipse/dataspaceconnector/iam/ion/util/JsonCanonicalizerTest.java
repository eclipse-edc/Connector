package org.eclipse.dataspaceconnector.iam.ion.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JsonCanonicalizerTest {

    @Test
    void canonicalizeAsBytes() {
        var map = Map.of("key1", "value1",
                "key2", 3.14159,
                "key3", Arrays.asList("one", "two", "three"),
                "key4", "",
                "key5", Collections.emptyList());

        var canon = JsonCanonicalizer.canonicalizeAsBytes(map);
        var json = new String(canon);

        assertThat(json).contains("key1");
        assertThat(json).doesNotContain("key4");
        assertThat(json).doesNotContain("key5");
    }

    @Test
    void verifyCanonicalForm() {
        var map1 = Map.of("key1", "value1",
                "key2", 3.14159,
                "key3", Arrays.asList("one", "two", "three"),
                "key4", "",
                "key5", Collections.emptyList());

        // the same as map1, but in a different sequence
        var map2 = Map.of("key1", "value1",
                "key4", "",
                "key3", Arrays.asList("one", "two", "three"),
                "key2", 3.14159,
                "key5", Collections.emptyList());


        assertThat(JsonCanonicalizer.canonicalizeAsBytes(map1)).isEqualTo(JsonCanonicalizer.canonicalizeAsBytes(map2));
    }
}
