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

package org.eclipse.edc.connector.controlplane.transform.edc.dataspaceprofile.to;

import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.protocol.spi.DataspaceProfile;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import static org.eclipse.edc.protocol.spi.DataspaceProfileContext.DATASPACE_PROFILE_CONTEXT_JSONLD_CONTEXTS_URL_IRI;
import static org.eclipse.edc.protocol.spi.DataspaceProfileContext.DATASPACE_PROFILE_CONTEXT_NAME_IRI;
import static org.eclipse.edc.protocol.spi.DataspaceProfileContext.DATASPACE_PROFILE_CONTEXT_PROTOCOL_BINDING_IRI;
import static org.eclipse.edc.protocol.spi.DataspaceProfileContext.DATASPACE_PROFILE_CONTEXT_PROTOCOL_IRI;
import static org.eclipse.edc.protocol.spi.DataspaceProfileContext.DATASPACE_PROFILE_CONTEXT_PROTOCOL_NAMESPACE_IRI;
import static org.eclipse.edc.protocol.spi.DataspaceProfileContext.DATASPACE_PROFILE_CONTEXT_PROTOCOL_PATH_IRI;
import static org.eclipse.edc.protocol.spi.DataspaceProfileContext.DATASPACE_PROFILE_CONTEXT_PROTOCOL_VERSION_IRI;

public class JsonObjectToDataspaceProfileTransformer extends AbstractJsonLdTransformer<JsonObject, DataspaceProfile> {

    public JsonObjectToDataspaceProfileTransformer() {
        super(JsonObject.class, DataspaceProfile.class);
    }

    @Override
    public @Nullable DataspaceProfile transform(@NotNull JsonObject request, @NotNull TransformerContext context) {
        var builder = DataspaceProfile.Builder.newInstance();

        builder.name(transformString(request.get(DATASPACE_PROFILE_CONTEXT_NAME_IRI), context));

        var protocol = returnJsonObject(request.get(DATASPACE_PROFILE_CONTEXT_PROTOCOL_IRI), context, DATASPACE_PROFILE_CONTEXT_PROTOCOL_IRI, false);
        if (protocol != null) {
            builder.protocolVersion(transformString(protocol.get(DATASPACE_PROFILE_CONTEXT_PROTOCOL_VERSION_IRI), context));
            builder.path(transformString(protocol.get(DATASPACE_PROFILE_CONTEXT_PROTOCOL_PATH_IRI), context));
            builder.binding(transformString(protocol.get(DATASPACE_PROFILE_CONTEXT_PROTOCOL_BINDING_IRI), context));
            builder.namespace(transformString(protocol.get(DATASPACE_PROFILE_CONTEXT_PROTOCOL_NAMESPACE_IRI), context));
        }

        Optional.ofNullable(request.getJsonArray(DATASPACE_PROFILE_CONTEXT_JSONLD_CONTEXTS_URL_IRI))
                .ifPresent(urls -> builder.jsonLdContextsUrl(urls.stream().map(v -> transformString(v, context)).toList()));

        return builder.build();
    }
}
