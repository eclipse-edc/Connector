package org.eclipse.dataspaceconnector.ids.daps;

import java.time.Instant;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DynamicAttributeTokenTest {

    @Test
    public void testNullPointerExceptionOnTokenArg() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            new DynamicAttributeToken(null, Instant.now());
        });
    }

    @Test
    public void testNullPointerExceptionOnExpirationArg() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            new DynamicAttributeToken("kjd", null);
        });
    }

}
