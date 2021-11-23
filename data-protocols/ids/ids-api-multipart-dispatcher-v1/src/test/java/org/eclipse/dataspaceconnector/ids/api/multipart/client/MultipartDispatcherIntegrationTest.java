package org.eclipse.dataspaceconnector.ids.api.multipart.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import de.fraunhofer.iais.eis.BaseConnector;
import de.fraunhofer.iais.eis.DescriptionResponseMessage;
import okhttp3.OkHttpClient;
import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.ids.spi.Protocols;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.metadata.MetadataRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MultipartDispatcherIntegrationTest extends AbstractMultipartDispatcherIntegrationTest {
    private static final String CONNECTOR_ID = UUID.randomUUID().toString();
    private MultipartDescriptionRequestSender sender;

    @Override
    protected Map<String, String> getSystemProperties() {
        return new HashMap<>() {
            {
                put("web.http.port", String.valueOf(getPort()));
                put("edc.ids.id", "urn:connector:" + CONNECTOR_ID);
            }
        };
    }

    @BeforeEach
    void init() {
        Monitor monitor = EasyMock.createNiceMock(Monitor.class);
        EasyMock.replay(monitor);
        var httpClient = new OkHttpClient.Builder().build();
        sender = new MultipartDescriptionRequestSender(CONNECTOR_ID, httpClient, OBJECT_MAPPER, monitor, identityService);
    }

    @Test
    void test() throws Exception {
        var request = MetadataRequest.Builder.newInstance()
                .connectorId(CONNECTOR_ID)
                .connectorAddress(getUrl())
                .protocol(Protocols.IDS_MULTIPART)
                .build();
        var result = sender.send(request, null).get();

        assertThat(result).isNotNull();
        assertThat(result.getHeader()).isNotNull();
        assertThat(result.getHeader()).isInstanceOf(DescriptionResponseMessage.class);
        assertThat(result.getPayload()).isNotNull();
        assertThat(result.getPayload()).isInstanceOf(BaseConnector.class);
    }

}
