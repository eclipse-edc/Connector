/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.transform.transformer.dspace.from;

import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.stream.JsonCollectors;
import org.eclipse.edc.controlplane.DataAddress;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.jsonld.spi.transformer.NamespacedJsonLdFromModelTransformer;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

import static org.eclipse.edc.controlplane.DataAddress.EDC_DATA_ADDRESS_TYPE_PROPERTY;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.transform.transformer.dspace.DataAddressDspaceSerialization.DSPACE_DATAADDRESS_TYPE_TERM;
import static org.eclipse.edc.transform.transformer.dspace.DataAddressDspaceSerialization.ENDPOINT_PROPERTIES_PROPERTY_TERM;
import static org.eclipse.edc.transform.transformer.dspace.DataAddressDspaceSerialization.ENDPOINT_PROPERTY_NAME_PROPERTY_TERM;
import static org.eclipse.edc.transform.transformer.dspace.DataAddressDspaceSerialization.ENDPOINT_PROPERTY_PROPERTY_TYPE_TERM;
import static org.eclipse.edc.transform.transformer.dspace.DataAddressDspaceSerialization.ENDPOINT_PROPERTY_TERM;
import static org.eclipse.edc.transform.transformer.dspace.DataAddressDspaceSerialization.ENDPOINT_PROPERTY_VALUE_PROPERTY_TERM;
import static org.eclipse.edc.transform.transformer.dspace.DataAddressDspaceSerialization.ENDPOINT_TYPE_PROPERTY_TERM;

public class JsonObjectFromDataAddressDspaceTransformer extends NamespacedJsonLdFromModelTransformer<DataAddress, JsonObject> {

    private static final Set<String> EXCLUDED_PROPERTIES = Set.of(EDC_DATA_ADDRESS_TYPE_PROPERTY);
    private final JsonBuilderFactory jsonFactory;
    private final TypeManager typeManager;
    private final String typeContext;

    public JsonObjectFromDataAddressDspaceTransformer(JsonBuilderFactory jsonFactory, TypeManager typeManager, String typeContext, JsonLdNamespace namespace) {
        super(DataAddress.class, JsonObject.class, namespace);
        this.jsonFactory = jsonFactory;
        this.typeManager = typeManager;
        this.typeContext = typeContext;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull DataAddress dataAddress, @NotNull TransformerContext context) {
        var endpointProperties = dataAddress.getProperties().entrySet().stream()
                .filter(e -> !EXCLUDED_PROPERTIES.contains(e.getKey()))
                .map(it -> endpointProperty(it.getKey(), it.getValue()))
                .collect(JsonCollectors.toJsonArray());

        var builder = jsonFactory.createObjectBuilder()
                .add(TYPE, forNamespace(DSPACE_DATAADDRESS_TYPE_TERM))
                .add(forNamespace(ENDPOINT_TYPE_PROPERTY_TERM), createId(jsonFactory, dataAddress.getType()))
                .add(forNamespace(ENDPOINT_PROPERTIES_PROPERTY_TERM), endpointProperties);

        var endpoint = dataAddress.getStringProperty(EDC_NAMESPACE + "endpoint");
        if (endpoint != null) {
            builder.add(forNamespace(ENDPOINT_PROPERTY_TERM), endpoint);
        }
        return builder.build();
    }

    private JsonObject endpointProperty(String key, Object value) {
        var builder = jsonFactory.createObjectBuilder()
                .add(TYPE, forNamespace(ENDPOINT_PROPERTY_PROPERTY_TYPE_TERM))
                .add(forNamespace(ENDPOINT_PROPERTY_NAME_PROPERTY_TERM), key);

        if (value instanceof String stringVal) {
            builder.add(forNamespace(ENDPOINT_PROPERTY_VALUE_PROPERTY_TERM), stringVal);
        } else {
            convertToJsonObject(value, builder);
        }

        return builder.build();
    }

    /**
     * convert property value to json object
     *
     * @param value the value.
     * @param builder the builder.
     * @deprecated DataAddress properties can only be String, as defined in DSP protocol, this method will be removed soon
     *     and non-string DataAddress properties, if ever exist, will be serialized in some way.
     */
    @Deprecated(since = "1.0.0")
    private void convertToJsonObject(Object value, JsonObjectBuilder builder) {
        var convertedValue = typeManager.getMapper(typeContext).convertValue(value, JsonObject.class);
        builder.add(forNamespace(ENDPOINT_PROPERTY_VALUE_PROPERTY_TERM), convertedValue);
    }
}
