package org.eclipse.dataspaceconnector.ids.transform;

import de.fraunhofer.iais.eis.Artifact;
import de.fraunhofer.iais.eis.ArtifactBuilder;
import de.fraunhofer.iais.eis.CustomMediaTypeBuilder;
import de.fraunhofer.iais.eis.MediaType;
import de.fraunhofer.iais.eis.Representation;
import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformKeys;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerContext;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AssetToRepresentationTransformerTest {
    private static final String REPRESENTATION_ID = "test_id";
    private static final URI REPRESENTATION_ID_URI = URI.create("urn:representation:1");
    private static final String ASSET_FILE_EXTENSION = "file_extension";
    private static final MediaType MEDIA_TYPE = new CustomMediaTypeBuilder()._filenameExtension_(ASSET_FILE_EXTENSION).build();

    // subject
    private AssetToRepresentationTransformer assetToRepresentationTransformer;

    // mocks
    private Asset asset;
    private TransformerContext context;

    @BeforeEach
    void setUp() {
        assetToRepresentationTransformer = new AssetToRepresentationTransformer();
        asset = EasyMock.createMock(Asset.class);
        context = EasyMock.createMock(TransformerContext.class);
    }

    @Test
    void testThrowsNullPointerExceptionForAll() {
        EasyMock.replay(asset, context);

        Assertions.assertThrows(NullPointerException.class, () -> {
            assetToRepresentationTransformer.transform(null, null);
        });
    }

    @Test
    void testThrowsNullPointerExceptionForContext() {
        EasyMock.replay(asset, context);

        Assertions.assertThrows(NullPointerException.class, () -> {
            assetToRepresentationTransformer.transform(asset, null);
        });
    }

    @Test
    void testReturnsNull() {
        EasyMock.replay(asset, context);

        var result = assetToRepresentationTransformer.transform(null, context);

        Assertions.assertNull(result);
    }


    @Test
    void testSuccessfulSimple() {
        // prepare
        EasyMock.expect(asset.getId()).andReturn(REPRESENTATION_ID);
        EasyMock.expect(asset.getProperties()).andReturn(Collections.emptyMap());

        var artifact = new ArtifactBuilder().build();
        EasyMock.expect(context.transform(EasyMock.anyObject(Asset.class), EasyMock.eq(Artifact.class))).andReturn(artifact);

        IdsId id = IdsId.Builder.newInstance().value(REPRESENTATION_ID).type(IdsType.REPRESENTATION).build();
        EasyMock.expect(context.transform(EasyMock.eq(id), EasyMock.eq(URI.class))).andReturn(REPRESENTATION_ID_URI);

        context.reportProblem(EasyMock.anyString());
        EasyMock.expectLastCall().atLeastOnce();

        // record
        EasyMock.replay(asset, context);

        // invoke
        var result = assetToRepresentationTransformer.transform(asset, context);

        // verify
        Assertions.assertNotNull(result);
        Assertions.assertEquals(REPRESENTATION_ID_URI, result.getId());
    }

    @Test
    void testSuccessfulMap() {
        // prepare
        EasyMock.expect(asset.getId()).andReturn(REPRESENTATION_ID);
        Map<String, Object> properties = new HashMap<>() {
            {
                put(TransformKeys.KEY_ASSET_FILE_EXTENSION, ASSET_FILE_EXTENSION);
            }
        };
        EasyMock.expect(asset.getProperties()).andReturn(properties);

        EasyMock.expect(context.transform(ASSET_FILE_EXTENSION, MediaType.class)).andReturn(MEDIA_TYPE);

        var artifact = new ArtifactBuilder().build();
        EasyMock.expect(context.transform(EasyMock.anyObject(Asset.class), EasyMock.eq(Artifact.class))).andReturn(artifact);

        IdsId id = IdsId.Builder.newInstance().value(REPRESENTATION_ID).type(IdsType.REPRESENTATION).build();
        EasyMock.expect(context.transform(EasyMock.eq(id), EasyMock.eq(URI.class))).andReturn(REPRESENTATION_ID_URI);

        // record
        EasyMock.replay(asset, context);

        // invoke
        Representation result = assetToRepresentationTransformer.transform(asset, context);

        // verify
        Assertions.assertNotNull(result);
        Assertions.assertEquals(REPRESENTATION_ID_URI, result.getId());
        Assertions.assertEquals(ASSET_FILE_EXTENSION, result.getMediaType().getFilenameExtension());
    }


    @AfterEach
    void tearDown() {
        EasyMock.verify(asset, context);
    }
}
