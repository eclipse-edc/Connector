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

import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.protocol.spi.AssociateDataspaceProfileContext;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

import static org.eclipse.edc.protocol.spi.AssociateDataspaceProfileContext.ASSOCIATE_DATASPACE_PROFILE_CONTEXT_PROFILES_IRI;

public class JsonObjectToAssociateDataspaceProfileContextTransformer extends AbstractJsonLdTransformer<JsonObject, AssociateDataspaceProfileContext> {


    public JsonObjectToAssociateDataspaceProfileContextTransformer() {
        super(JsonObject.class, AssociateDataspaceProfileContext.class);
    }

    @Override
    public @Nullable AssociateDataspaceProfileContext transform(@NotNull JsonObject request, @NotNull TransformerContext context) {
        var profiles = Optional.ofNullable(request.getJsonArray(ASSOCIATE_DATASPACE_PROFILE_CONTEXT_PROFILES_IRI))
                .map(ja -> ja.stream().map(this::nodeValue).toList())
                .orElse(List.of());
        return new AssociateDataspaceProfileContext(profiles);
    }
}
