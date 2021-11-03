package org.eclipse.dataspaceconnector.ids.api.multipart.handler.description;

import de.fraunhofer.iais.eis.DescriptionRequestMessage;
import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.ids.core.configuration.IllegalSettingException;
import org.eclipse.dataspaceconnector.ids.core.configuration.SettingResolver;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformResult;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerRegistry;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.URISyntaxException;

final class DescriptionRequestHandlerMocks {

    public static AssetIndex mockAssetIndex() {
        AssetIndex assetIndex = EasyMock.createMock(AssetIndex.class);
        Asset asset = EasyMock.createMock(Asset.class);
        EasyMock.expect(assetIndex.findById(EasyMock.isA(String.class))).andReturn(asset);
        EasyMock.replay(asset);
        return assetIndex;
    }

    public static TransformerRegistry mockTransformerRegistry(IdsType type) throws URISyntaxException {
        TransformerRegistry transformerRegistry = EasyMock.createMock(TransformerRegistry.class);
        var uri = new URI("https://example.com");
        var uriResult = (TransformResult) EasyMock.createMock(TransformResult.class);
        EasyMock.expect(uriResult.getOutput()).andReturn(uri);
        EasyMock.expect(uriResult.hasProblems()).andReturn(false);
        EasyMock.expect(transformerRegistry.transform(EasyMock.isA(IdsId.class), EasyMock.eq(URI.class))).andReturn(uriResult);
        var idsId = IdsId.Builder.newInstance().type(type).value("value").build();
        var idsIdResult = (TransformResult) EasyMock.createMock(TransformResult.class);
        EasyMock.expect(idsIdResult.getOutput()).andReturn(idsId);
        EasyMock.expect(idsIdResult.hasProblems()).andReturn(false);
        EasyMock.expect(transformerRegistry.transform(EasyMock.isA(URI.class), EasyMock.eq(IdsId.class))).andReturn(idsIdResult);
        EasyMock.replay(uriResult, idsIdResult);
        return transformerRegistry;
    }

    public static DescriptionRequestMessage mockDescriptionRequestMessage(URI requestedElement) throws URISyntaxException {
        DescriptionRequestMessage descriptionRequestMessage = EasyMock.createMock(DescriptionRequestMessage.class);
        EasyMock.expect(descriptionRequestMessage.getId()).andReturn(new URI("https://correlation-id.com/"));
        EasyMock.expect(descriptionRequestMessage.getSenderAgent()).andReturn(new URI("https://sender-agent.com/"));
        EasyMock.expect(descriptionRequestMessage.getIssuerConnector()).andReturn(new URI("https://issuer-connector.com/"));
        EasyMock.expect(descriptionRequestMessage.getRequestedElement()).andReturn(requestedElement);
        return descriptionRequestMessage;
    }

    @NotNull
    public static SettingResolver mockSettingsResolver() throws IllegalSettingException {
        SettingResolver settingResolver = EasyMock.createMock(SettingResolver.class);
        EasyMock.expect(settingResolver.resolveId()).andReturn("connector-id");
        return settingResolver;
    }
}
