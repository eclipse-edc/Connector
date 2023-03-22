package org.eclipse.edc.protocol.dsp.transform.type;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferStartMessage;
import org.eclipse.edc.jsonld.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.jsonld.transformer.JsonLdKeywords;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

import static org.eclipse.edc.protocol.dsp.transform.DspNamespaces.DSPACE_SCHEMA;

public class JsonObjectFromTransferStartMessageTransformer extends AbstractJsonLdTransformer<TransferStartMessage, JsonObject> {

    private final JsonBuilderFactory jsonBuilderFactory;

    private final ObjectMapper mapper;

    public JsonObjectFromTransferStartMessageTransformer(JsonBuilderFactory jsonBuilderFactory, ObjectMapper mapper){
        super(TransferStartMessage.class,JsonObject.class);
        this.jsonBuilderFactory = jsonBuilderFactory;
        this.mapper = mapper;
    }

    @Override
    public @Nullable JsonObject transform(@Nullable TransferStartMessage transferStartMessage, @NotNull TransformerContext context) {
        if (transferStartMessage == null){
            return null;
        }

        var builder = jsonBuilderFactory.createObjectBuilder();

        builder.add(JsonLdKeywords.ID,String.valueOf(UUID.randomUUID()));
        builder.add(JsonLdKeywords.TYPE, DSPACE_SCHEMA + "TransferStartMessage");

        //TODO ADD More fields after Model is evolved

        return builder.build();
    }
}
