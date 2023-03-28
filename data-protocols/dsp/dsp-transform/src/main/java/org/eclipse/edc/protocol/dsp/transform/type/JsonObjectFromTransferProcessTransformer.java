package org.eclipse.edc.protocol.dsp.transform.type;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.jsonld.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.jsonld.transformer.JsonLdKeywords;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.protocol.dsp.transform.DspNamespaces.DSPACE_PREFIX;
import static org.eclipse.edc.protocol.dsp.transform.DspNamespaces.DSPACE_SCHEMA;

public class JsonObjectFromTransferProcessTransformer extends AbstractJsonLdTransformer<TransferProcess, JsonObject> {

    private final JsonBuilderFactory jsonBuilderFactory;

    private final ObjectMapper mapper;


    public JsonObjectFromTransferProcessTransformer(JsonBuilderFactory jsonBuilderFactory, ObjectMapper mapper){
        super(TransferProcess.class,JsonObject.class);

        this.jsonBuilderFactory = jsonBuilderFactory;
        this.mapper = mapper;
    }

    @Override
    public @Nullable JsonObject transform(@Nullable TransferProcess transferProcess, @NotNull TransformerContext context) {
        if(transferProcess == null){
            return null;
        }

        var builder = jsonBuilderFactory.createObjectBuilder();

        builder.add(JsonLdKeywords.ID,transferProcess.getId());
        builder.add(JsonLdKeywords.TYPE, DSPACE_SCHEMA + "TransferProcess");

      //  builder.add(DSPACE_Prefix+"correlationId", transferProcess.); //TODO Find ID
        builder.add(DSPACE_SCHEMA+ "state", TransferProcessStates.from(transferProcess.getState()).name());

        return builder.build();
    }
}
