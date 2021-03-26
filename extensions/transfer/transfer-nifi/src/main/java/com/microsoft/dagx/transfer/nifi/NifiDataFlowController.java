package com.microsoft.dagx.transfer.nifi;

import com.microsoft.dagx.spi.DagxException;
import com.microsoft.dagx.spi.transfer.flow.DataFlowController;
import com.microsoft.dagx.spi.transfer.flow.DataFlowInitiateResponse;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.types.TypeManager;
import com.microsoft.dagx.spi.types.domain.metadata.GenericDataEntryExtensions;
import com.microsoft.dagx.spi.types.domain.transfer.DataRequest;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.microsoft.dagx.spi.transfer.response.ResponseStatus.ERROR_RETRY;
import static com.microsoft.dagx.spi.transfer.response.ResponseStatus.FATAL_ERROR;
import static java.lang.String.format;

public class NifiDataFlowController implements DataFlowController {
    private static final String PROCESS_GROUPS = "/process-groups/";
    private static final String FLOW = "/flow/process-groups/";
    private static final MediaType JSON = MediaType.get("application/json");
    private static final String PROCESS_GROUP_KEY = "processGroup";

    private String baseUrl;
    private TypeManager typeManager;
    private Monitor monitor;

    public NifiDataFlowController(NifiTransferManagerConfiguration configuration, TypeManager typeManager, Monitor monitor) {
        baseUrl = configuration.getUrl();
        this.typeManager = typeManager;
        this.monitor = monitor;
    }

    @Override
    public boolean canHandle(DataRequest dataRequest) {
        // handle everything for now
        return true;
    }

    @Override
    public @NotNull DataFlowInitiateResponse initiateFlow(DataRequest dataRequest) {
        if (!(dataRequest.getDataEntry().getExtensions() instanceof GenericDataEntryExtensions)) {
            throw new DagxException("Invalid extensions type, expected:" + GenericDataEntryExtensions.class.getName());
        }

        Request request = createTransferRequest(dataRequest);

        OkHttpClient client = createClient();

        try (Response response = client.newCall(request).execute()) {
            int code = response.code();
            if (code != 200) {
                monitor.severe(format("Error initiating transfer request with Nifi. Code was: %d. Request id was: %s", code, dataRequest.getId()));
                return new DataFlowInitiateResponse(FATAL_ERROR, "Error initiating transfer");
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

            return DataFlowInitiateResponse.OK;
        } catch (IOException e) {
            monitor.severe("Error initiating data transfer request: " + dataRequest.getId(), e);
            return new DataFlowInitiateResponse(ERROR_RETRY, "Error initiating transfer");
        }
    }

    @NotNull
    private Request createTransferRequest(DataRequest dataRequest) {
        GenericDataEntryExtensions extensions = (GenericDataEntryExtensions) dataRequest.getDataEntry().getExtensions();
        String processId = extensions.getProperties().get(PROCESS_GROUP_KEY);

        String url = baseUrl + FLOW + processId;
        Map<String, String> payload = new HashMap<>();
        payload.put("id", processId);
        payload.put("state", "RUNNING");
        return new Request.Builder().url(url).put(RequestBody.create(typeManager.writeValueAsString(payload), JSON)).build();
    }

    @NotNull
    private DataFlowInitiateResponse emptyBodyError(DataRequest dataRequest) {
        monitor.severe(format("Error initiating transfer request with Nifi. Empty message body returned. Request id was: %s", dataRequest.getId()));
        return new DataFlowInitiateResponse(FATAL_ERROR, "Error initiating transfer");
    }

    private OkHttpClient createClient() {
        return new OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build();

    }
}
