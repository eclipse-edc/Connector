package org.eclipse.dataspaceconnector.ids.api.multipart.handler.description;

import de.fraunhofer.iais.eis.DescriptionRequestMessage;
import de.fraunhofer.iais.eis.ResourceCatalog;
import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.service.DataCatalogService;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformResult;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerRegistry;
import org.eclipse.dataspaceconnector.ids.spi.types.DataCatalog;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;

import static org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.DescriptionRequestHandlerMocks.mockDescriptionRequestMessage;
import static org.eclipse.dataspaceconnector.ids.api.multipart.handler.description.DescriptionRequestHandlerMocks.mockTransformerRegistry;

public class DataCatalogDescriptionRequestHandlerTest {

    private static final String CONNECTOR_ID = "urn:connector:edc";

    // subject
    private DataCatalogDescriptionRequestHandler dataCatalogDescriptionRequestHandler;

    // mocks
    private Monitor monitor;
    private TransformerRegistry transformerRegistry;
    private DescriptionRequestMessage descriptionRequestMessage;
    private DataCatalogService dataCatalogService;
    private ResourceCatalog resourceCatalog;

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @BeforeEach
    public void setup() throws URISyntaxException {
        monitor = EasyMock.createMock(Monitor.class);
        resourceCatalog = EasyMock.createMock(ResourceCatalog.class);
        EasyMock.expect(resourceCatalog.getId()).andReturn(new URI("https://resource-catalog.com"));
        EasyMock.replay(monitor, resourceCatalog);

        dataCatalogService = EasyMock.createMock(DataCatalogService.class);
        EasyMock.expect(dataCatalogService.getDataCatalog()).andReturn(EasyMock.createMock(DataCatalog.class));
        EasyMock.replay(dataCatalogService);

        transformerRegistry = mockTransformerRegistry(IdsType.CATALOG);
        var dataCatalogResult = (TransformResult) EasyMock.createMock(TransformResult.class);
        EasyMock.expect(dataCatalogResult.getOutput()).andReturn(resourceCatalog);
        EasyMock.expect(dataCatalogResult.hasProblems()).andReturn(false);
        EasyMock.expect(transformerRegistry.transform(EasyMock.isA(DataCatalog.class), EasyMock.eq(ResourceCatalog.class))).andReturn(dataCatalogResult);
        EasyMock.replay(transformerRegistry, dataCatalogResult);

        descriptionRequestMessage = mockDescriptionRequestMessage(resourceCatalog.getId());
        EasyMock.replay(descriptionRequestMessage);

        dataCatalogDescriptionRequestHandler = new DataCatalogDescriptionRequestHandler(monitor, CONNECTOR_ID, dataCatalogService, transformerRegistry);
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void testConstructorArgumentsNotNullable() {
        Assertions.assertThrows(NullPointerException.class,
                () -> new DataCatalogDescriptionRequestHandler(null, CONNECTOR_ID, dataCatalogService, transformerRegistry));
        Assertions.assertThrows(NullPointerException.class,
                () -> new DataCatalogDescriptionRequestHandler(monitor, null, dataCatalogService, transformerRegistry));
        Assertions.assertThrows(NullPointerException.class,
                () -> new DataCatalogDescriptionRequestHandler(monitor, CONNECTOR_ID, null, transformerRegistry));
        Assertions.assertThrows(NullPointerException.class,
                () -> new DataCatalogDescriptionRequestHandler(monitor, CONNECTOR_ID, dataCatalogService, null));
    }

    @Test
    public void testSimpleSuccessPath() {
        var result = dataCatalogDescriptionRequestHandler.handle(descriptionRequestMessage, null);

        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getHeader());
        Assertions.assertEquals(resourceCatalog, result.getPayload());
    }
}
