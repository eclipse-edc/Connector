package org.eclipse.dataspaceconnector.ids.core.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.easymock.EasyMock;
import org.easymock.IArgumentMatcher;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.iam.TokenResult;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.domain.metadata.DataEntry;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.util.concurrent.ExecutionException;

import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates.REQUESTED;

class DataRequestMessageSenderTest {

    private final IdentityService identityService = EasyMock.mock(IdentityService.class);
    private final TransferProcessStore transferProcessStore = EasyMock.mock(TransferProcessStore.class);
    private final Vault vault = EasyMock.mock(Vault.class);
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Monitor monitor = EasyMock.mock(Monitor.class);

    private final WireMockServer wireMockServer = new WireMockServer(wireMockConfig().port(9091));

    @BeforeEach
    void setUp() {
        wireMockServer.start();
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void shouldSendDataRequestToConnector() throws ExecutionException, InterruptedException {
        wireMockServer.stubFor(post("/api/ids/request").willReturn(ok()));
        TokenResult token = TokenResult.Builder.newInstance().token("token").build();
        EasyMock.expect(identityService.obtainClientCredentials("otherConnectorId")).andReturn(token);
        TransferProcess transferProcess = TransferProcess.Builder.newInstance().id("id").state(REQUESTED.code()).build();
        EasyMock.expect(transferProcessStore.find("processId")).andReturn(transferProcess);
        transferProcessStore.update(stateIs(TransferProcessStates.REQUESTED_ACK));
        EasyMock.expectLastCall();
        EasyMock.replay(identityService, transferProcessStore);
        var dataRequestMessageSender = new DataRequestMessageSender("thisConnectorId", identityService,
                transferProcessStore, vault, new ObjectMapper(), monitor, httpClient);
        var dataRequest = DataRequest.Builder.newInstance()
                .connectorId("otherConnectorId")
                .connectorAddress("http://localhost:9091")
                .dataDestination(DataAddress.Builder.newInstance().build())
                .destinationType("type")
                .dataEntry(DataEntry.Builder.newInstance().id("http://anUrl").build())
                .build();

        Void result = dataRequestMessageSender.send(dataRequest, () -> "processId").get();

        assertThat(result).isNull();
        EasyMock.verify(transferProcessStore);
    }

    public static TransferProcess stateIs(TransferProcessStates state){
        EasyMock.reportMatcher(new IArgumentMatcher() {
            @Override
            public boolean matches(Object argument) {
                var process = (TransferProcess) argument;
                return process.getState() == state.code();
            }

            @Override
            public void appendTo(StringBuffer buffer) {
                buffer.append("state(\"" + state.name() + "\")");
            }
        });
        return null;
    }
}