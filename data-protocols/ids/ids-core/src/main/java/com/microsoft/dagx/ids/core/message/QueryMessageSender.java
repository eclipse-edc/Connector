package com.microsoft.dagx.ids.core.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.dagx.spi.DagxException;
import com.microsoft.dagx.spi.iam.IdentityService;
import com.microsoft.dagx.spi.message.MessageContext;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.types.domain.metadata.QueryRequest;
import de.fraunhofer.iais.eis.DynamicAttributeToken;
import de.fraunhofer.iais.eis.DynamicAttributeTokenBuilder;
import de.fraunhofer.iais.eis.QueryMessageBuilder;
import de.fraunhofer.iais.eis.TokenFormat;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.microsoft.dagx.common.Cast.cast;
import static com.microsoft.dagx.ids.core.message.MessageFunctions.writeJson;

/**
 *
 */
public class QueryMessageSender implements IdsMessageSender<QueryRequest, List<String>> {
    private static final String JSON = "application/json";
    private static final String VERSION = "1.0";

    private final OkHttpClient httpClient;
    private final ObjectMapper mapper;
    private final Monitor monitor;
    private final URI connectorName;

    private final IdentityService identityService;

    public QueryMessageSender(String connectorName,
                              IdentityService identityService,
                              OkHttpClient httpClient,
                              ObjectMapper mapper,
                              Monitor monitor) {
        this.connectorName = URI.create(connectorName);
        this.identityService = identityService;
        this.httpClient = httpClient;
        this.mapper = mapper;
        this.monitor = monitor;
    }

    @Override
    public Class<QueryRequest> messageType() {
        return QueryRequest.class;
    }

    @Override
    public CompletableFuture<List<String>> send(QueryRequest queryRequest, MessageContext context) {
        var connectorId = queryRequest.getConnectorId();

        var tokenResult = identityService.obtainClientCredentials(connectorId);

        DynamicAttributeToken token = new DynamicAttributeTokenBuilder()._tokenFormat_(TokenFormat.JWT)._tokenValue_(tokenResult.getToken()).build();

        var queryMessage = new QueryMessageBuilder()
                // FIXME handle timezone issue ._issued_(gregorianNow())
                ._modelVersion_(VERSION)
                ._securityToken_(token)
                ._issuerConnector_(connectorName)
//                ._queryLanguage_(queryRequest.getQueryLanguage())  // TODO report that this type should not be an Enum
                .build();
        queryMessage.setProperty("query", queryRequest.getQuery());
        queryMessage.setProperty("queryLanguage", queryRequest.getQueryLanguage());
        var processId = context.getProcessId();

        var requestBody = writeJson(queryMessage, mapper);

        CompletableFuture<List<String>> future = new CompletableFuture<>();

        var connectorAddress = queryRequest.getConnectorAddress();
        HttpUrl baseUrl = HttpUrl.parse(connectorAddress);
        if (baseUrl == null) {
            future.completeExceptionally(new IllegalArgumentException("Connector address not specified"));
            return future;
        }

        HttpUrl connectorEndpoint = baseUrl.newBuilder().addPathSegment("api").addPathSegment("ids").addPathSegment("query").build();

        Request request = new Request.Builder().url(connectorEndpoint).addHeader("Content-Type", JSON).post(requestBody).build();


        httpClient.newCall(request).enqueue(new FutureCallback<>(future, r -> {
            try (r) {
                if (r.isSuccessful()) {
                    monitor.debug("Query response received");
                    try (var body = r.body()) {
                        if (body == null) {
                            future.completeExceptionally(new DagxException("Received an empty body response from connector"));
                        } else {
                            return cast(mapper.readValue(body.string(), List.class));
                        }
                    } catch (IOException e) {
                        future.completeExceptionally(e);
                    }
                } else {
                    if (r.code() == 403) {
                        // forbidden
                        future.completeExceptionally(new DagxException("Received not authorized from connector"));
                    } else {
                        future.completeExceptionally(new DagxException("Received an error from connector:" + r.code()));
                    }
                }
                return null;
            }
        }));
        return future;

    }
}
