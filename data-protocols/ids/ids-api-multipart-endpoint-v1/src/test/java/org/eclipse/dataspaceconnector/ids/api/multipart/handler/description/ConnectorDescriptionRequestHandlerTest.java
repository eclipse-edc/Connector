package org.eclipse.dataspaceconnector.ids.api.multipart.handler.description;

import de.fraunhofer.iais.eis.Connector;
import de.fraunhofer.iais.eis.DescriptionRequestMessage;
import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.ids.core.configuration.IllegalSettingException;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.service.ConnectorService;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformResult;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerRegistry;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;

import static org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.DescriptionRequestHandlerMocks.mockDescriptionRequestMessage;
import static org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.DescriptionRequestHandlerMocks.mockSettingsResolver;
import static org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.DescriptionRequestHandlerMocks.mockTransformerRegistry;

public class ConnectorDescriptionRequestHandlerTest {
    // subject
    private ConnectorDescriptionRequestHandler connectorDescriptionRequestHandler;

    // mocks
    private Monitor monitor;
    private ConnectorDescriptionRequestHandlerSettings connectorDescriptionRequestHandlerSettings;
    private ConnectorService connectorService;
    private TransformerRegistry transformerRegistry;
    private DescriptionRequestMessage descriptionRequestMessage;
    private Connector connector;

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @BeforeEach
    public void setup() throws URISyntaxException, IllegalSettingException {
        monitor = EasyMock.createMock(Monitor.class);
        connector = EasyMock.createMock(Connector.class);
        EasyMock.replay(monitor, connector);

        var settingsResolver = mockSettingsResolver();
        EasyMock.replay(settingsResolver);
        var settingFactory = new ConnectorDescriptionRequestHandlerSettingsFactory(settingsResolver);
        var settingFactoryResult = settingFactory.getSettingsResult();
        var settings = settingFactoryResult.getSettings();
        connectorDescriptionRequestHandlerSettings = settings;

        connectorService = EasyMock.createMock(ConnectorService.class);
        EasyMock.expect(connectorService.getConnector()).andReturn(EasyMock.createMock(org.eclipse.dataspaceconnector.ids.spi.types.Connector.class));
        EasyMock.replay(connectorService);

        transformerRegistry = mockTransformerRegistry(IdsType.CONNECTOR);
        var connectorResult = (TransformResult) EasyMock.createMock(TransformResult.class);
        EasyMock.expect(connectorResult.getOutput()).andReturn(connector);
        EasyMock.expect(connectorResult.hasProblems()).andReturn(false);
        EasyMock.expect(transformerRegistry.transform(EasyMock.isA(org.eclipse.dataspaceconnector.ids.spi.types.Connector.class), EasyMock.eq(Connector.class))).andReturn(connectorResult);
        EasyMock.replay(transformerRegistry, connectorResult);

        descriptionRequestMessage = mockDescriptionRequestMessage(new URI("urn:connector:" + settings.getId()));
        EasyMock.replay(descriptionRequestMessage);

        connectorDescriptionRequestHandler = new ConnectorDescriptionRequestHandler(monitor, connectorDescriptionRequestHandlerSettings, connectorService, transformerRegistry);
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testConstructorArgumentsNotNullable() {
        Assertions.assertThrows(NullPointerException.class,
                () -> new ConnectorDescriptionRequestHandler(null, connectorDescriptionRequestHandlerSettings, connectorService, transformerRegistry));
        Assertions.assertThrows(NullPointerException.class,
                () -> new ConnectorDescriptionRequestHandler(monitor, null, connectorService, transformerRegistry));
        Assertions.assertThrows(NullPointerException.class,
                () -> new ConnectorDescriptionRequestHandler(monitor, connectorDescriptionRequestHandlerSettings, null, transformerRegistry));
        Assertions.assertThrows(NullPointerException.class,
                () -> new ConnectorDescriptionRequestHandler(monitor, connectorDescriptionRequestHandlerSettings, connectorService, null));
    }

    @Test
    public void testSimpleSuccessPath() {
        var result = connectorDescriptionRequestHandler.handle(descriptionRequestMessage, null);

        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getHeader());
        Assertions.assertEquals(connector, result.getPayload());
    }
}
