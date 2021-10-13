package org.eclipse.dataspaceconnector.ids.core.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import okhttp3.OkHttpClient;
import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.iam.TokenResult;
import org.eclipse.dataspaceconnector.spi.message.MessageContext;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.metadata.QueryRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.util.concurrent.ExecutionException;

import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

class QueryMessageSenderTest {

    private final IdentityService identityService = EasyMock.mock(IdentityService.class);
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Monitor monitor = EasyMock.mock(Monitor.class);

    private final WireMockServer wireMockServer = new WireMockServer(wireMockConfig().port(9090));

    @BeforeEach
    void setUp() {
        wireMockServer.start();
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void shouldSendQueryToConnector() throws ExecutionException, InterruptedException {
        wireMockServer.stubFor(post("/api/ids/query")
                .willReturn(ok()
                        .withBody("[\"response\"]")));
        var connectorId = "otherConnectorId";
        TokenResult tokenResult = TokenResult.Builder.newInstance().token("token").build();
        EasyMock.expect(identityService.obtainClientCredentials(connectorId)).andReturn(tokenResult);
        EasyMock.replay(identityService);
        var objectMapper = new ObjectMapper();
        var queryMessageSender = new QueryMessageSender("thisConnectorId", identityService, objectMapper, monitor, httpClient);
        var queryRequest = QueryRequest.Builder.newInstance()
                .protocol("protocol")
                .connectorId(connectorId)
                .queryLanguage("queryLanguage")
                .query("query")
                .connectorAddress("http://localhost:9090")
                .build();
        MessageContext context = () -> "processId";

        var result = queryMessageSender.send(queryRequest, context).get();

        assertThat(result).containsExactly("response");
    }
}