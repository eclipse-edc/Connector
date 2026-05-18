/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.transform.edc.dataspaceprofile.from;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.protocol.spi.DataspaceProfileContext;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import static jakarta.json.stream.JsonCollectors.toJsonArray;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.spi.DataspaceProfileContext.DATASPACE_PROFILE_CONTEXT_JSONLD_CONTEXTS_URL_IRI;
import static org.eclipse.edc.protocol.spi.DataspaceProfileContext.DATASPACE_PROFILE_CONTEXT_NAME_IRI;
import static org.eclipse.edc.protocol.spi.DataspaceProfileContext.DATASPACE_PROFILE_CONTEXT_PROTOCOL_BINDING_IRI;
import static org.eclipse.edc.protocol.spi.DataspaceProfileContext.DATASPACE_PROFILE_CONTEXT_PROTOCOL_IRI;
import static org.eclipse.edc.protocol.spi.DataspaceProfileContext.DATASPACE_PROFILE_CONTEXT_PROTOCOL_NAMESPACE_IRI;
import static org.eclipse.edc.protocol.spi.DataspaceProfileContext.DATASPACE_PROFILE_CONTEXT_PROTOCOL_PATH_IRI;
import static org.eclipse.edc.protocol.spi.DataspaceProfileContext.DATASPACE_PROFILE_CONTEXT_PROTOCOL_VERSION_IRI;
import static org.eclipse.edc.protocol.spi.DataspaceProfileContext.DATASPACE_PROFILE_CONTEXT_TYPE_IRI;

public class JsonObjectFromDataspaceProfileContextTransformer extends AbstractJsonLdTransformer<DataspaceProfileContext, JsonObject> {

    private final JsonBuilderFactory jsonFactory;

    public JsonObjectFromDataspaceProfileContextTransformer(JsonBuilderFactory jsonFactory) {
        super(DataspaceProfileContext.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull DataspaceProfileContext profile, @NotNull TransformerContext context) {
        var builder = jsonFactory.createObjectBuilder()
                .add(TYPE, DATASPACE_PROFILE_CONTEXT_TYPE_IRI)
                .add(DATASPACE_PROFILE_CONTEXT_NAME_IRI, profile.name())
                .add(DATASPACE_PROFILE_CONTEXT_PROTOCOL_IRI, getProtocolVersionConsumer(profile));

        Optional.ofNullable(profile.jsonLdContextsUrl())
                .ifPresent(urls -> builder.add(DATASPACE_PROFILE_CONTEXT_JSONLD_CONTEXTS_URL_IRI, urls.stream()
                        .map(Json::createValue)
                        .collect(toJsonArray())));

        return builder.build();
    }

    private @NotNull JsonObject getProtocolVersionConsumer(@NotNull DataspaceProfileContext profile) {
        var version = profile.protocolVersion();
        return jsonFactory.createObjectBuilder()
                .add(DATASPACE_PROFILE_CONTEXT_PROTOCOL_VERSION_IRI, version.version())
                .add(DATASPACE_PROFILE_CONTEXT_PROTOCOL_PATH_IRI, version.path())
                .add(DATASPACE_PROFILE_CONTEXT_PROTOCOL_BINDING_IRI, version.binding())
                .add(DATASPACE_PROFILE_CONTEXT_PROTOCOL_NAMESPACE_IRI, profile.protocolNamespace().namespace())
                .build();
    }
}
