package com.microsoft.dagx.ids.core.message;

import com.microsoft.dagx.spi.iam.IdentityService;
import com.microsoft.dagx.spi.types.domain.transfer.DataRequest;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

import java.util.concurrent.CompletableFuture;

import static jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE;

/**
 * Binds and sends {@link DataRequest} messages through the IDS protocol.
 */
public class DataRequestMessageSender implements IdsMessageSender<DataRequest, Void> {
    private IdentityService identityService;
    private OkHttpClient httpClient;

    public DataRequestMessageSender(IdentityService identityService, OkHttpClient httpClient) {
        this.identityService = identityService;
        this.httpClient = httpClient;
    }

    @Override
    public Class<DataRequest> messageType() {
        return DataRequest.class;
    }

    @Override
    public CompletableFuture<Void> send(DataRequest dataRequest) {
        RequestBody requestBody = new FormBody.Builder().build();

        var connectorId = dataRequest.getConnectorId();

        var credentials = identityService.obtainClientCredentials(connectorId);

        var connectorAddress = dataRequest.getConnectorAddress();

        // TODO add JWT credentials
        Request request = new Request.Builder().url(connectorAddress).addHeader("Content-Type", CONTENT_TYPE).post(requestBody).build();

        CompletableFuture<Void> future = new CompletableFuture<>();

        // TODO implement
        httpClient.newCall(request).enqueue(new FutureCallback<>(future, r -> null));
        return future;
    }

}
