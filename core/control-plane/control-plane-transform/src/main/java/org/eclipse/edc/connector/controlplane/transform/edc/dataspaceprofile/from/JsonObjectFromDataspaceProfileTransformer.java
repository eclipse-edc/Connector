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
import org.eclipse.edc.protocol.spi.DataspaceProfile;
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
import static org.eclipse.edc.protocol.spi.DataspaceProfileContext.DATASPACE_PROFILE_CONTEXT_TRUSTED_ISSUERS_IRI;
import static org.eclipse.edc.protocol.spi.DataspaceProfileContext.DATASPACE_PROFILE_CONTEXT_TYPE_IRI;

public class JsonObjectFromDataspaceProfileTransformer extends AbstractJsonLdTransformer<DataspaceProfile, JsonObject> {

    private final JsonBuilderFactory jsonFactory;

    public JsonObjectFromDataspaceProfileTransformer(JsonBuilderFactory jsonFactory) {
        super(DataspaceProfile.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull DataspaceProfile profile, @NotNull TransformerContext context) {
        var builder = jsonFactory.createObjectBuilder()
                .add(TYPE, DATASPACE_PROFILE_CONTEXT_TYPE_IRI)
                .add(DATASPACE_PROFILE_CONTEXT_NAME_IRI, profile.getName())
                .add(DATASPACE_PROFILE_CONTEXT_PROTOCOL_IRI, protocol(profile));

        Optional.ofNullable(profile.getJsonLdContextsUrl())
                .ifPresent(urls -> builder.add(DATASPACE_PROFILE_CONTEXT_JSONLD_CONTEXTS_URL_IRI, urls.stream()
                        .map(Json::createValue)
                        .collect(toJsonArray())));

        Optional.ofNullable(profile.getTrustedIssuers())
                .ifPresent(trustedIssuers -> builder.add(DATASPACE_PROFILE_CONTEXT_TRUSTED_ISSUERS_IRI, trustedIssuers.stream()
                        .map(t -> context.transform(t, JsonObject.class))
                        .collect(toJsonArray())
                ));

        return builder.build();
    }

    private @NotNull JsonObject protocol(@NotNull DataspaceProfile profile) {
        var builder = jsonFactory.createObjectBuilder();
        Optional.ofNullable(profile.getProtocolVersion()).ifPresent(v -> builder.add(DATASPACE_PROFILE_CONTEXT_PROTOCOL_VERSION_IRI, v));
        Optional.ofNullable(profile.getPath()).ifPresent(v -> builder.add(DATASPACE_PROFILE_CONTEXT_PROTOCOL_PATH_IRI, v));
        Optional.ofNullable(profile.getBinding()).ifPresent(v -> builder.add(DATASPACE_PROFILE_CONTEXT_PROTOCOL_BINDING_IRI, v));
        Optional.ofNullable(profile.getNamespace()).ifPresent(v -> builder.add(DATASPACE_PROFILE_CONTEXT_PROTOCOL_NAMESPACE_IRI, v));
        return builder.build();
    }
}
