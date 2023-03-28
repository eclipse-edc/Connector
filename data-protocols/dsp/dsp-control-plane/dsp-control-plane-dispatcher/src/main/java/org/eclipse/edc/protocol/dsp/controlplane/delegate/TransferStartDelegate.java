package org.eclipse.edc.protocol.dsp.controlplane.delegate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferStartMessage;
import org.eclipse.edc.jsonld.transformer.JsonLdTransformerRegistry;
import org.eclipse.edc.protocol.dsp.spi.dispatcher.DspDispatcherDelegate;
import org.eclipse.edc.spi.EdcException;

import java.util.function.Function;

import static org.eclipse.edc.protocol.dsp.transform.util.DocumentUtil.compactDocument;

public class TransferStartDelegate implements DspDispatcherDelegate<TransferStartMessage, JsonObject> {

    private final ObjectMapper mapper;

    private final JsonLdTransformerRegistry registry;

    public TransferStartDelegate(ObjectMapper mapper, JsonLdTransformerRegistry registry){
        this.mapper = mapper;
        this.registry= registry;
    }

    @Override
    public Class<TransferStartMessage> getMessageType() {
        return TransferStartMessage.class;
    }

    @Override
    public Request buildRequest(TransferStartMessage message) {
        var start = registry.transform(message, JsonObject.class);
        if (start.failed()){
            throw new EdcException("Failed to create request body for transfer start message.");
        }

        var content = mapper.convertValue(compactDocument(start.getContent()),JsonObject.class);
        var requestBody = RequestBody.create(toString(content), MediaType.get(jakarta.ws.rs.core.MediaType.APPLICATION_JSON));

        return new Request.Builder()
                .url(message.getConnectorAddress()+"transfers/" )//TODO FIND CorrelationID
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build();
    }

    @Override
    public Function<Response, JsonObject> parseResponse() {
        return null;
    }

    private String toString(JsonObject input){
        try {
            return mapper.writeValueAsString(input);
        } catch (JsonProcessingException e) {
            throw new EdcException("Failed to serialize dspace:TransferStartMessage",e);
        }
    }
}
