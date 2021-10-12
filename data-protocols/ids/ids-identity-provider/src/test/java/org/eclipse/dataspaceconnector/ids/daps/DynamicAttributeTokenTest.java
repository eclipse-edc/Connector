package org.eclipse.dataspaceconnector.ids.daps;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;

class DynamicAttributeTokenTest {

    @Test
    void testNullPointerExceptionOnTokenArg() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            new DynamicAttributeToken(null, Instant.now());
        });
    }

    @Test
    void testNullPointerExceptionOnExpirationArg() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            new DynamicAttributeToken("kjd", null);
        });
    }

}
