package org.eclipse.dataspaceconnector.ids.api.multipart.handler.description;

import de.fraunhofer.iais.eis.Artifact;
import de.fraunhofer.iais.eis.DescriptionRequestMessage;
import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.ids.core.configuration.IllegalSettingException;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformResult;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerRegistry;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;

import static org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.DescriptionRequestHandlerMocks.mockAssetIndex;
import static org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.DescriptionRequestHandlerMocks.mockDescriptionRequestMessage;
import static org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.DescriptionRequestHandlerMocks.mockSettingsResolver;
import static org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.DescriptionRequestHandlerMocks.mockTransformerRegistry;

public class ArtifactDescriptionRequestHandlerTest {

    // subject
    private ArtifactDescriptionRequestHandler artifactDescriptionRequestHandler;

    // mocks
    private Monitor monitor;
    private ArtifactDescriptionRequestHandlerSettings artifactDescriptionRequestHandlerSettings;
    private AssetIndex assetIndex;
    private TransformerRegistry transformerRegistry;
    private DescriptionRequestMessage descriptionRequestMessage;
    private Artifact artifact;

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @BeforeEach
    public void setup() throws URISyntaxException, IllegalSettingException {
        monitor = EasyMock.createMock(Monitor.class);
        artifact = EasyMock.createMock(Artifact.class);
        EasyMock.expect(artifact.getId()).andReturn(new URI("urn:artifact:test"));
        EasyMock.replay(monitor, artifact);

        var settingsResolver = mockSettingsResolver();
        EasyMock.replay(settingsResolver);
        var settingFactory = new ArtifactDescriptionRequestHandlerSettingsFactory(settingsResolver);
        var settingFactoryResult = settingFactory.getSettingsResult();
        artifactDescriptionRequestHandlerSettings = settingFactoryResult.getSettings();

        assetIndex = mockAssetIndex();
        EasyMock.replay(assetIndex);

        transformerRegistry = mockTransformerRegistry(IdsType.ARTIFACT);
        var artifactResult = (TransformResult) EasyMock.createMock(TransformResult.class);
        EasyMock.expect(artifactResult.getOutput()).andReturn(artifact);
        EasyMock.expect(artifactResult.hasProblems()).andReturn(false);
        EasyMock.expect(transformerRegistry.transform(EasyMock.isA(Asset.class), EasyMock.eq(Artifact.class))).andReturn(artifactResult);
        EasyMock.replay(transformerRegistry, artifactResult);

        descriptionRequestMessage = mockDescriptionRequestMessage(artifact.getId());
        EasyMock.replay(descriptionRequestMessage);

        artifactDescriptionRequestHandler = new ArtifactDescriptionRequestHandler(monitor, artifactDescriptionRequestHandlerSettings, assetIndex, transformerRegistry);
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testConstructorArgumentsNotNullable() {
        Assertions.assertThrows(NullPointerException.class,
                () -> new ArtifactDescriptionRequestHandler(null, artifactDescriptionRequestHandlerSettings, assetIndex, transformerRegistry));
        Assertions.assertThrows(NullPointerException.class,
                () -> new ArtifactDescriptionRequestHandler(monitor, null, assetIndex, transformerRegistry));
        Assertions.assertThrows(NullPointerException.class,
                () -> new ArtifactDescriptionRequestHandler(monitor, artifactDescriptionRequestHandlerSettings, null, transformerRegistry));
        Assertions.assertThrows(NullPointerException.class,
                () -> new ArtifactDescriptionRequestHandler(monitor, artifactDescriptionRequestHandlerSettings, assetIndex, null));
    }

    @Test
    public void testSimpleSuccessPath() {
        var result = artifactDescriptionRequestHandler.handle(descriptionRequestMessage, null);

        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getHeader());
        Assertions.assertEquals(artifact, result.getPayload());
    }
}
