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

import jakarta.json.JsonObject;
import org.eclipse.edc.iam.decentralizedclaims.spi.scope.DcpScope;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.iam.decentralizedclaims.spi.scope.DcpScope.DCP_SCOPE_PREFIX_MAPPING_IRI;
import static org.eclipse.edc.iam.decentralizedclaims.spi.scope.DcpScope.DCP_SCOPE_PROFILE_IRI;
import static org.eclipse.edc.iam.decentralizedclaims.spi.scope.DcpScope.DCP_SCOPE_TYPE_PROPERTY_IRI;
import static org.eclipse.edc.iam.decentralizedclaims.spi.scope.DcpScope.DCP_SCOPE_VALUE_IRI;

public class JsonObjectToDcpScopeTransformer extends AbstractJsonLdTransformer<JsonObject, DcpScope> {

    public JsonObjectToDcpScopeTransformer() {
        super(JsonObject.class, DcpScope.class);
    }

    @Override
    public @Nullable DcpScope transform(@NotNull JsonObject jsonObject, @NotNull TransformerContext context) {
        var builder = DcpScope.Builder.newInstance();

        var id = nodeId(jsonObject);
        if (id != null) {
            builder.id(id);
        }

        var value = jsonObject.get(DCP_SCOPE_VALUE_IRI);
        if (value != null) {
            transformString(value, builder::value, context);
        }

        var profile = jsonObject.get(DCP_SCOPE_PROFILE_IRI);
        if (profile != null) {
            transformString(profile, builder::profile, context);
        }

        var prefixMapping = jsonObject.get(DCP_SCOPE_PREFIX_MAPPING_IRI);
        if (prefixMapping != null) {
            transformString(prefixMapping, builder::prefixMapping, context);
        }

        var type = jsonObject.get(DCP_SCOPE_TYPE_PROPERTY_IRI);
        if (type != null) {
            transformString(type, t -> builder.type(DcpScope.Type.valueOf(t)), context);
        }

        try {
            return builder.build();
        } catch (RuntimeException e) {
            context.reportProblem("Invalid DcpScope: " + e.getMessage());
            return null;
        }
    }
}
