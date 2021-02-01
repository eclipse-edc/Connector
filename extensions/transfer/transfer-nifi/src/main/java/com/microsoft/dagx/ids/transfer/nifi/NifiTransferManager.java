package com.microsoft.dagx.ids.transfer.nifi;

import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.transfer.TransferManager;
import com.microsoft.dagx.spi.transfer.TransferResponse;
import com.microsoft.dagx.spi.types.TypeManager;
import com.microsoft.dagx.spi.types.domain.transfer.DataRequest;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.microsoft.dagx.spi.transfer.TransferResponse.Status.OK;
import static java.lang.String.format;

public class NifiTransferManager implements TransferManager {
    private String url;
    private TypeManager typeManager;
    private Monitor monitor;

    public NifiTransferManager(NifiTransferManagerConfiguration configuration, TypeManager typeManager, Monitor monitor) {
        url = configuration.getUrl();
        this.typeManager = typeManager;
        this.monitor = monitor;
    }

    @Override
    public boolean canHandle(URI dataUrn) {
        // handle everything for now
        return true;
    }

    @Override
    public TransferResponse initiateTransfer(DataRequest dataRequest) {
        OkHttpClient client = createClient();
        Request request = new Request.Builder().url(url + "/flow/status").get().build();

        try (Response response = client.newCall(request).execute()) {
            int code = response.code();
            if (code != 200) {
                monitor.severe(format("Error initiating transfer request with Nifi. Code was: %d. Request id was: %s", code, dataRequest.getId()));
                return new TransferResponse(TransferResponse.Status.ERROR);
            }
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                return emptyBodyError(dataRequest);
            }
            String message = responseBody.string();
            if (message.length() == 0) {
                return emptyBodyError(dataRequest);
            }

            @SuppressWarnings("unchecked") Map<String, Object> values = typeManager.readValue(message, Map.class);

            return new TransferResponse(OK);
        } catch (IOException e) {
            monitor.severe("Error initiating data transfer request: " + dataRequest.getId(), e);
            return new TransferResponse(TransferResponse.Status.ERROR);
        }
    }

    @NotNull
    private TransferResponse emptyBodyError(DataRequest dataRequest) {
        monitor.severe(format("Error initiating transfer request with Nifi. Empty message body returned. Request id was: %s", dataRequest.getId()));
        return new TransferResponse(TransferResponse.Status.ERROR);
    }

    private OkHttpClient createClient() {
        return new OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build();

    }
}
