package org.eclipse.dataspaceconnector.ids.transform;

import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;

public class UriToIdsIdTransformerTest {

    private static final IdsType IDS_ID_TYPE = IdsType.ARTIFACT;
    private static final String IDS_ID_VALUE = "32d39d70-68f7-44f3-b8b2-27550f2081f4";
    private static final URI URI = java.net.URI.create("urn:artifact:32d39d70-68f7-44f3-b8b2-27550f2081f4");

    // subject
    UriToIdsIdTransformer uriToIdsIdTransformer;

    // mocks
    private TransformerContext context;

    @BeforeEach
    public void setup() {
        uriToIdsIdTransformer = new UriToIdsIdTransformer();
        context = EasyMock.createMock(TransformerContext.class);
    }

    @Test
    void testThrowsNullPointerExceptionForAll() {
        EasyMock.replay(context);

        Assertions.assertThrows(NullPointerException.class, () -> {
            uriToIdsIdTransformer.transform(null, null);
        });
    }

    @Test
    void testThrowsNullPointerExceptionForContext() {
        EasyMock.replay(context);

        Assertions.assertThrows(NullPointerException.class, () -> {
            uriToIdsIdTransformer.transform(URI, null);
        });
    }

    @Test
    void testReturnsNull() {
        EasyMock.replay(context);

        var result = uriToIdsIdTransformer.transform(null, context);

        Assertions.assertNull(result);
    }

    @Test
    void testSuccessfulSimple() {
        //prepare
        var idsId = IdsId.Builder.newInstance().type(IDS_ID_TYPE).value(IDS_ID_VALUE).build();
        EasyMock.expect(context.transform(EasyMock.eq(URI), EasyMock.eq(IdsId.class))).andReturn(idsId);

        // record
        EasyMock.replay(context);

        // invoke
        var result = uriToIdsIdTransformer.transform(URI, context);

        // verify
        Assertions.assertNotNull(result);
        Assertions.assertEquals(IDS_ID_TYPE, result.getType());
        Assertions.assertEquals(IDS_ID_VALUE, result.getValue());
    }
}
