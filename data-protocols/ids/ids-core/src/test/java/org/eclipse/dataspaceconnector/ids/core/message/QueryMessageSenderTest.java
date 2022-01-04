package org.eclipse.dataspaceconnector.ids.core.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.types.domain.metadata.QueryRequest;
import org.junit.jupiter.api.Test;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QueryMessageSenderTest {

    private final IdentityService identityService = mock(IdentityService.class);

    private final QueryMessageSender sender = new QueryMessageSender("anId", identityService,
            mock(OkHttpClient.class), new ObjectMapper(), mock(Monitor.class));

    @Test
    void should_return_failed_future_if_client_credentials_retrieval_fails() {
        when(identityService.obtainClientCredentials(any())).thenReturn(Result.failure("error"));
        var queryRequest = QueryRequest.Builder.newInstance()
                .protocol("protocol")
                .connectorId("connectorId")
                .queryLanguage("queryLanguage")
                .query("query")
                .connectorAddress("connectorAddress")
                .build();

        var result = sender.send(queryRequest, () -> "processId");

        assertThat(result).failsWithin(1, MILLISECONDS);
    }
}