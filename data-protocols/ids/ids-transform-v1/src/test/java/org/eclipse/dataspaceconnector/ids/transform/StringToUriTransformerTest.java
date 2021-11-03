package org.eclipse.dataspaceconnector.ids.transform;

import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class StringToUriTransformerTest {
    private static final String STRING = "https//example.com";

    // subject
    StringToUriTransformer stringToUriTransformer;

    // mocks
    private TransformerContext context;

    @BeforeEach
    public void setup() {
        stringToUriTransformer = new StringToUriTransformer();
        context = EasyMock.createMock(TransformerContext.class);
    }

    @Test
    void testThrowsNullPointerExceptionForAll() {
        EasyMock.replay(context);

        Assertions.assertThrows(NullPointerException.class, () -> {
            stringToUriTransformer.transform(null, null);
        });
    }

    @Test
    void testThrowsNullPointerExceptionForContext() {
        EasyMock.replay(context);

        Assertions.assertThrows(NullPointerException.class, () -> {
            stringToUriTransformer.transform(STRING, null);
        });
    }

    @Test
    void testReturnsNull() {
        EasyMock.replay(context);

        var result = stringToUriTransformer.transform(null, context);

        Assertions.assertNull(result);
    }

    @Test
    void testSuccessfulSimple() {
        // record
        EasyMock.replay(context);

        // invoke
        var result = stringToUriTransformer.transform(STRING, context);

        // verify
        Assertions.assertNotNull(result);
        Assertions.assertEquals(STRING, result.toString());
    }
}
