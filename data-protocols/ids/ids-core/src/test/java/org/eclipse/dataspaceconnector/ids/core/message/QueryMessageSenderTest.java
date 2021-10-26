package org.eclipse.dataspaceconnector.ids.core.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.iam.TokenResult;
import org.eclipse.dataspaceconnector.spi.message.MessageContext;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.metadata.QueryRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.okForJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.replay;


class QueryMessageSenderTest {

    private final IdentityService identityService = mock(IdentityService.class);
    private final OkHttpClient httpClient = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Monitor monitor = mock(Monitor.class);
    private final WireMockServer httpServer = new WireMockServer(new WireMockConfiguration().port(9999));

    private final QueryMessageSender sender = new QueryMessageSender("connectorId", identityService, httpClient, objectMapper, monitor);

    @BeforeEach
    void setUp() {
        httpServer.start();
    }

    @AfterEach
    void tearDown() {
        httpServer.stop();
    }

    @Test
    void name() throws ExecutionException, InterruptedException {
        var assets = List.of(Asset.Builder.newInstance().id("anId").build());
        httpServer.stubFor(WireMock.post("/api/ids/query").willReturn(okForJson(assets)));
        TokenResult tokenResult = TokenResult.Builder.newInstance().token("token").build();
        expect(identityService.obtainClientCredentials("otherConnectorId")).andReturn(tokenResult);
        replay(identityService);
        QueryRequest queryRequest = QueryRequest.Builder.newInstance()
                .protocol("ids-rest")
                .connectorId("otherConnectorId")
                .query("select *")
                .queryLanguage("dataspaceconnector")
                .connectorAddress("http://localhost:9999")
                .build();
        MessageContext messageContext = () -> "processId";

        List<String> result = sender.send(queryRequest, messageContext).get();

        assertThat(result).containsOnly("anId");
    }
}