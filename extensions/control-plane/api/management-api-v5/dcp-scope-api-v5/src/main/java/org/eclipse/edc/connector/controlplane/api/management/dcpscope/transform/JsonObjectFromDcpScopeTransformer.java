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

package org.eclipse.edc.connector.controlplane.api.management.dcpscope.transform;

import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.iam.decentralizedclaims.spi.scope.DcpScope;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.iam.decentralizedclaims.spi.scope.DcpScope.DCP_SCOPE_PREFIX_MAPPING_IRI;
import static org.eclipse.edc.iam.decentralizedclaims.spi.scope.DcpScope.DCP_SCOPE_PROFILE_IRI;
import static org.eclipse.edc.iam.decentralizedclaims.spi.scope.DcpScope.DCP_SCOPE_TYPE_IRI;
import static org.eclipse.edc.iam.decentralizedclaims.spi.scope.DcpScope.DCP_SCOPE_TYPE_PROPERTY_IRI;
import static org.eclipse.edc.iam.decentralizedclaims.spi.scope.DcpScope.DCP_SCOPE_VALUE_IRI;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;

public class JsonObjectFromDcpScopeTransformer extends AbstractJsonLdTransformer<DcpScope, JsonObject> {

    private final JsonBuilderFactory jsonFactory;

    public JsonObjectFromDcpScopeTransformer(JsonBuilderFactory jsonFactory) {
        super(DcpScope.class, JsonObject.class);
        this.jsonFactory = jsonFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull DcpScope scope, @NotNull TransformerContext context) {
        var builder = jsonFactory.createObjectBuilder()
                .add(TYPE, DCP_SCOPE_TYPE_IRI)
                .add(ID, scope.getId())
                .add(DCP_SCOPE_VALUE_IRI, scope.getValue())
                .add(DCP_SCOPE_PROFILE_IRI, scope.getProfile())
                .add(DCP_SCOPE_TYPE_PROPERTY_IRI, scope.getType().name());

        if (scope.getPrefixMapping() != null) {
            builder.add(DCP_SCOPE_PREFIX_MAPPING_IRI, scope.getPrefixMapping());
        }

        return builder.build();
    }
}
