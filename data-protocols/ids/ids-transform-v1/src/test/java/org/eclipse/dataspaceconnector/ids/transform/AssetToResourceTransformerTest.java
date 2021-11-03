package org.eclipse.dataspaceconnector.ids.transform;

import de.fraunhofer.iais.eis.Representation;
import de.fraunhofer.iais.eis.RepresentationBuilder;
import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerContext;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Collections;

public class AssetToResourceTransformerTest {

    private static final String RESOURCE_ID = "test_id";
    private static final URI RESOURCE_ID_URI = URI.create("urn:resource:1");

    // subject
    private AssetToResourceTransformer assetToResourceTransformer;

    // mocks
    private Asset asset;
    private TransformerContext context;

    @BeforeEach
    void setUp() {
        assetToResourceTransformer = new AssetToResourceTransformer();
        asset = EasyMock.createMock(Asset.class);
        context = EasyMock.createMock(TransformerContext.class);
    }


    @Test
    void testThrowsNullPointerExceptionForAll() {
        EasyMock.replay(asset, context);

        Assertions.assertThrows(NullPointerException.class, () -> {
            assetToResourceTransformer.transform(null, null);
        });
    }

    @Test
    void testThrowsNullPointerExceptionForContext() {
        EasyMock.replay(asset, context);

        Assertions.assertThrows(NullPointerException.class, () -> {
            assetToResourceTransformer.transform(asset, null);
        });
    }

    @Test
    void testReturnsNull() {
        EasyMock.replay(asset, context);

        var result = assetToResourceTransformer.transform(null, context);

        Assertions.assertNull(result);
    }

    @Test
    void testSuccessfulSimple() {
        // prepare
        EasyMock.expect(asset.getId()).andReturn(RESOURCE_ID);
        EasyMock.expect(asset.getProperties()).andReturn(Collections.emptyMap());

        var representation = new RepresentationBuilder().build();
        EasyMock.expect(context.transform(EasyMock.anyObject(Asset.class), EasyMock.eq(Representation.class))).andReturn(representation);

        IdsId id = IdsId.Builder.newInstance().value(RESOURCE_ID).type(IdsType.RESOURCE).build();
        EasyMock.expect(context.transform(EasyMock.eq(id), EasyMock.eq(URI.class))).andReturn(RESOURCE_ID_URI);

        context.reportProblem(EasyMock.anyString());
        EasyMock.expectLastCall().atLeastOnce();

        // record
        EasyMock.replay(asset, context);

        // invoke
        var result = assetToResourceTransformer.transform(asset, context);

        // verify
        Assertions.assertNotNull(result);
        Assertions.assertEquals(RESOURCE_ID_URI, result.getId());
        Assertions.assertEquals(1, result.getRepresentation().size());
        Assertions.assertEquals(representation, result.getRepresentation().get(0));
    }

}
