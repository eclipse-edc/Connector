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

package org.eclipse.edc.transform.transformer.dspace.v2024.from;

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
import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_SCHEMA_2024_1;
import static org.eclipse.edc.spi.types.domain.DataAddress.EDC_DATA_ADDRESS_TYPE_PROPERTY;
import static org.eclipse.edc.transform.transformer.dspace.DataAddressDspaceSerialization.DSPACE_DATAADDRESS_TYPE_TERM;
import static org.eclipse.edc.transform.transformer.dspace.DataAddressDspaceSerialization.ENDPOINT_PROPERTIES_PROPERTY_TERM;
import static org.eclipse.edc.transform.transformer.dspace.DataAddressDspaceSerialization.ENDPOINT_PROPERTY_NAME_PROPERTY_TERM;
import static org.eclipse.edc.transform.transformer.dspace.DataAddressDspaceSerialization.ENDPOINT_PROPERTY_PROPERTY_TYPE_TERM;
import static org.eclipse.edc.transform.transformer.dspace.DataAddressDspaceSerialization.ENDPOINT_PROPERTY_VALUE_PROPERTY_TERM;
import static org.eclipse.edc.transform.transformer.dspace.DataAddressDspaceSerialization.ENDPOINT_TYPE_PROPERTY_TERM;

public class JsonObjectFromDataAddressDspace2024Transformer extends AbstractNamespaceAwareJsonLdTransformer<DataAddress, JsonObject> {

    private static final Set<String> EXCLUDED_PROPERTIES = Set.of(EDC_DATA_ADDRESS_TYPE_PROPERTY);
    private final JsonBuilderFactory jsonFactory;
    private final TypeManager typeManager;
    private final String typeContext;

    public JsonObjectFromDataAddressDspace2024Transformer(JsonBuilderFactory jsonFactory, TypeManager typeManager, String typeContext) {
        this(jsonFactory, typeManager, typeContext, new JsonLdNamespace(DSPACE_SCHEMA_2024_1));
    }

    public JsonObjectFromDataAddressDspace2024Transformer(JsonBuilderFactory jsonFactory, TypeManager typeManager, String typeContext, JsonLdNamespace namespace) {
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

        return jsonFactory.createObjectBuilder()
                .add(TYPE, forNamespace(DSPACE_DATAADDRESS_TYPE_TERM))
                .add(forNamespace(ENDPOINT_TYPE_PROPERTY_TERM), createId(jsonFactory, dataAddress.getType()))
                .add(forNamespace(ENDPOINT_PROPERTIES_PROPERTY_TERM), endpointProperties)
                .build();
    }

    private JsonObject endpointProperty(String key, Object value) {
        var builder = jsonFactory.createObjectBuilder()
                .add(TYPE, forNamespace(ENDPOINT_PROPERTY_PROPERTY_TYPE_TERM))
                .add(forNamespace(ENDPOINT_PROPERTY_NAME_PROPERTY_TERM), key);

        if (value instanceof String stringVal) {
            builder.add(forNamespace(ENDPOINT_PROPERTY_VALUE_PROPERTY_TERM), stringVal);
        } else {
            builder.add(forNamespace(ENDPOINT_PROPERTY_VALUE_PROPERTY_TERM), typeManager.getMapper(typeContext).convertValue(value, JsonObject.class));
        }

        return builder.build();
    }
}
