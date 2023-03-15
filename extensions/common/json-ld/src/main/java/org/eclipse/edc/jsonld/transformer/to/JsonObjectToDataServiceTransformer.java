package org.eclipse.edc.jsonld.transformer.to;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.connector.contract.spi.types.offer.DataService;
import org.eclipse.edc.jsonld.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.lang.String.format;
import static org.eclipse.edc.jsonld.transformer.JsonLdFunctions.nodeId;
import static org.eclipse.edc.jsonld.transformer.JsonLdFunctions.nodeType;
import static org.eclipse.edc.jsonld.transformer.JsonLdNavigator.visitProperties;
import static org.eclipse.edc.jsonld.transformer.Namespaces.DCAT_SCHEMA;
import static org.eclipse.edc.jsonld.transformer.Namespaces.DCT_SCHEMA;
import static org.eclipse.edc.jsonld.transformer.TransformerUtil.transformString;

/**
 *
 */
public class JsonObjectToDataServiceTransformer extends AbstractJsonLdTransformer<JsonObject, DataService> {

    private static final String DCAT_DATA_SERVICE_TYPE =  DCAT_SCHEMA + "DataService";
    private static final String DCT_TERMS_PROPERTY = DCT_SCHEMA + "terms";
    private static final String DCT_ENDPOINT_URL_PROPERTY = DCT_SCHEMA + "endpointUrl";
    
    public JsonObjectToDataServiceTransformer() {
        super(JsonObject.class, DataService.class);
    }

    @Override
    public @Nullable DataService transform(@Nullable JsonObject object, @NotNull TransformerContext context) {
        if (object == null) {
            return null;
        }
    
        var type = nodeType(object, context);
        if (DCAT_DATA_SERVICE_TYPE.equals(type)) {
            var builder = DataService.Builder.newInstance();
    
            builder.id(nodeId(object));
            visitProperties(object, (key, value) -> transformProperties(key, value, builder, context));
    
            return builder.build();
        } else {
            context.reportProblem(format("Cannot transform type %s to DataService", type));
            return null;
        }
    }
    
    private void transformProperties(String key, JsonValue value, DataService.Builder builder, TransformerContext context) {
        if (DCT_TERMS_PROPERTY.equals(key)) {
            transformString(value, builder::terms, context);
        } else if (DCT_ENDPOINT_URL_PROPERTY.equals(key)) {
            transformString(value, builder::endpointUrl, context);
        } else {
            context.reportProblem(format("Invalid property found for Distribution: %s", key));
        }
    }
}
