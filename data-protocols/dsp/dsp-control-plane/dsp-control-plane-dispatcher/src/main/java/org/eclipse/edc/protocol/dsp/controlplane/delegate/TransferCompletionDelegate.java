package org.eclipse.edc.protocol.dsp.controlplane.delegate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferCompletionMessage;
import org.eclipse.edc.jsonld.transformer.JsonLdTransformerRegistry;
import org.eclipse.edc.protocol.dsp.spi.dispatcher.DspDispatcherDelegate;
import org.eclipse.edc.spi.EdcException;

import java.util.function.Function;

import static org.eclipse.edc.protocol.dsp.transform.util.DocumentUtil.compactDocument;

public class TransferCompletionDelegate implements DspDispatcherDelegate<TransferCompletionMessage, JsonObject> {

    private final ObjectMapper mapper;

    private final JsonLdTransformerRegistry registry;

    public TransferCompletionDelegate(ObjectMapper mapper, JsonLdTransformerRegistry registry){
        this.mapper = mapper;
        this.registry = registry;
    }

    @Override
    public Class<TransferCompletionMessage> getMessageType() {
        return TransferCompletionMessage.class;
    }

    @Override
    public Request buildRequest(TransferCompletionMessage message) {
        var completion = registry.transform(message,JsonObject.class);

        if (completion.failed()){
            throw new EdcException("Failed to create request body for transfer completion message");
        }

        var content = mapper.convertValue(compactDocument(completion.getContent()),JsonObject.class);

        var requestBody = RequestBody.create(toString(content), MediaType.get(jakarta.ws.rs.core.MediaType.APPLICATION_JSON));

        return new Request.Builder()
                .url(message.getConnectorAddress()+ "transfers/") //TODO CREATE CORRELATIONID
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
            throw new EdcException("Failed to serialize dspace:TransferCompletionMessage", e);
        }
    }
}
