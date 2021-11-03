package org.eclipse.dataspaceconnector.ids.transform;

import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

public class IntegerToBigIntegerTransformerTest {
    private static final Integer INTEGER = 10;

    // subject
    IntegerToBigIntegerTransformer integerToBigIntegerTransformer;

    // mocks
    private TransformerContext context;

    @BeforeEach
    public void setup() {
        integerToBigIntegerTransformer = new IntegerToBigIntegerTransformer();
        context = EasyMock.createMock(TransformerContext.class);
    }

    @Test
    void testThrowsNullPointerExceptionForAll() {
        EasyMock.replay(context);

        Assertions.assertThrows(NullPointerException.class, () -> {
            integerToBigIntegerTransformer.transform(null, null);
        });
    }

    @Test
    void testThrowsNullPointerExceptionForContext() {
        EasyMock.replay(context);

        Assertions.assertThrows(NullPointerException.class, () -> {
            integerToBigIntegerTransformer.transform(INTEGER, null);
        });
    }

    @Test
    void testReturnsNull() {
        EasyMock.replay(context);

        var result = integerToBigIntegerTransformer.transform(null, context);

        Assertions.assertNull(result);
    }


    @Test
    void testSuccessfulSimple() {
        // record
        EasyMock.replay(context);

        // invoke
        var result = integerToBigIntegerTransformer.transform(INTEGER, context);

        // verify
        Assertions.assertEquals(BigInteger.valueOf(INTEGER), result);
    }
}
