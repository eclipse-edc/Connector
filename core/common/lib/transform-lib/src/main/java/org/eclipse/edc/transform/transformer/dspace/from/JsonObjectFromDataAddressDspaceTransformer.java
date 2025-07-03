/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.transform.transformer.dspace.from;

import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import jakarta.json.stream.JsonCollectors;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.jsonld.spi.transformer.AbstractNamespaceAwareJsonLdTransformer;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_SCHEMA;
import static org.eclipse.edc.spi.types.domain.DataAddress.EDC_DATA_ADDRESS_RESPONSE_CHANNEL;
import static org.eclipse.edc.spi.types.domain.DataAddress.EDC_DATA_ADDRESS_TYPE_PROPERTY;
import static org.eclipse.edc.transform.transformer.dspace.DataAddressDspaceSerialization.DSPACE_DATAADDRESS_TYPE_TERM;
import static org.eclipse.edc.transform.transformer.dspace.DataAddressDspaceSerialization.ENDPOINT_PROPERTIES_PROPERTY_TERM;
import static org.eclipse.edc.transform.transformer.dspace.DataAddressDspaceSerialization.ENDPOINT_PROPERTY_NAME_PROPERTY_TERM;
import static org.eclipse.edc.transform.transformer.dspace.DataAddressDspaceSerialization.ENDPOINT_PROPERTY_PROPERTY_TYPE_TERM;
import static org.eclipse.edc.transform.transformer.dspace.DataAddressDspaceSerialization.ENDPOINT_PROPERTY_VALUE_PROPERTY_TERM;
import static org.eclipse.edc.transform.transformer.dspace.DataAddressDspaceSerialization.ENDPOINT_TYPE_PROPERTY_TERM;

public class JsonObjectFromDataAddressDspaceTransformer extends AbstractNamespaceAwareJsonLdTransformer<DataAddress, JsonObject> {

    private static final Set<String> EXCLUDED_PROPERTIES = Set.of(EDC_DATA_ADDRESS_TYPE_PROPERTY);
    private final JsonBuilderFactory jsonFactory;
    private final TypeManager typeManager;
    private final String typeContext;

    public JsonObjectFromDataAddressDspaceTransformer(JsonBuilderFactory jsonFactory, TypeManager typeManager, String typeContext) {
        this(jsonFactory, typeManager, typeContext, new JsonLdNamespace(DSPACE_SCHEMA));
    }

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
                .map(it -> endpointProperty(it.getKey(), it.getValue(), context))
                .collect(JsonCollectors.toJsonArray());

        return jsonFactory.createObjectBuilder()
                .add(TYPE, forNamespace(DSPACE_DATAADDRESS_TYPE_TERM))
                .add(forNamespace(ENDPOINT_TYPE_PROPERTY_TERM), dataAddress.getType())
                .add(forNamespace(ENDPOINT_PROPERTIES_PROPERTY_TERM), endpointProperties)
                .build();
    }

    private JsonObject endpointProperty(String key, Object value, TransformerContext context) {
        var builder = jsonFactory.createObjectBuilder()
                .add(TYPE, forNamespace(ENDPOINT_PROPERTY_PROPERTY_TYPE_TERM))
                .add(forNamespace(ENDPOINT_PROPERTY_NAME_PROPERTY_TERM), key);

        if (value instanceof String stringVal) {
            builder.add(forNamespace(ENDPOINT_PROPERTY_VALUE_PROPERTY_TERM), stringVal);
        } else {
            var complexValue = key.equals(EDC_DATA_ADDRESS_RESPONSE_CHANNEL) ? context.transform(value, JsonObject.class) :
                    typeManager.getMapper(typeContext).convertValue(value, JsonObject.class);

            builder.add(forNamespace(ENDPOINT_PROPERTY_VALUE_PROPERTY_TERM), complexValue);
        }

        return builder.build();
    }
}
