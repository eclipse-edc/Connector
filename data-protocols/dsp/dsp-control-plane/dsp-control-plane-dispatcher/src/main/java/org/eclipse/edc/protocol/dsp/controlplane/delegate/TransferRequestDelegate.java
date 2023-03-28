package org.eclipse.edc.protocol.dsp.controlplane.delegate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferRequestMessage;
import org.eclipse.edc.jsonld.transformer.JsonLdTransformerRegistry;
import org.eclipse.edc.protocol.dsp.spi.dispatcher.DspDispatcherDelegate;
import org.eclipse.edc.spi.EdcException;

import java.io.IOException;
import java.util.function.Function;

import static org.eclipse.edc.protocol.dsp.transform.util.DocumentUtil.compactDocument;
import static org.eclipse.edc.protocol.dsp.transform.util.DocumentUtil.expandDocument;

public class TransferRequestDelegate implements DspDispatcherDelegate<TransferRequestMessage, TransferProcess> {

    private final ObjectMapper mapper;

    private final JsonLdTransformerRegistry registry;

    public TransferRequestDelegate(ObjectMapper mapper, JsonLdTransformerRegistry registry){
        this.mapper = mapper;
        this.registry = registry;
    }

    @Override
    public Class<TransferRequestMessage> getMessageType() {
        return TransferRequestMessage.class;
    }

    @Override
    public Request buildRequest(TransferRequestMessage message) {
        var transferRequest = registry.transform(message,JsonObject.class);

        if (transferRequest.failed()){
            throw new EdcException("Failed to create request body for transfer request message");
        }

        var content = mapper.convertValue(compactDocument(transferRequest.getContent()),JsonObject.class); //Funktioniert das out of the Box?
        var requestBody = RequestBody.create(toString(content), MediaType.get(jakarta.ws.rs.core.MediaType.APPLICATION_JSON));

        return new Request.Builder()
                .url(message.getConnectorAddress()+ "/transfers/" + "request")
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build();
    }

    @Override
    public Function<Response, TransferProcess> parseResponse() {
        return response -> {
            try {
                var jsonObject = mapper.readValue(response.body().bytes(), JsonObject.class);
                var result = registry.transform(expandDocument(jsonObject).get(0), TransferProcess.class);

                if (result.succeeded()){
                    return result.getContent();
                }else {
                    throw new EdcException("Failed to read response body from transfer request");
                }

            }catch (RuntimeException | IOException e){
                throw new EdcException("Failed to read response body from contract request.",e);
            }
        };
    }

    private String toString(JsonObject input){
        try {
            return mapper.writeValueAsString(input);
        } catch (JsonProcessingException e) {
            throw new EdcException("Failed to serialize dspace:TransferRequestMessage",e);
        }
    }
}
