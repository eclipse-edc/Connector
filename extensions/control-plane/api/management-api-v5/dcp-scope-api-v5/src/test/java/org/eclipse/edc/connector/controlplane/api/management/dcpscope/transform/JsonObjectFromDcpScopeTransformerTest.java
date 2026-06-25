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

import jakarta.json.Json;
import org.eclipse.edc.iam.decentralizedclaims.spi.scope.DcpScope;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.decentralizedclaims.spi.scope.DcpScope.DCP_SCOPE_PREFIX_MAPPING_IRI;
import static org.eclipse.edc.iam.decentralizedclaims.spi.scope.DcpScope.DCP_SCOPE_PROFILE_IRI;
import static org.eclipse.edc.iam.decentralizedclaims.spi.scope.DcpScope.DCP_SCOPE_TYPE_IRI;
import static org.eclipse.edc.iam.decentralizedclaims.spi.scope.DcpScope.DCP_SCOPE_TYPE_PROPERTY_IRI;
import static org.eclipse.edc.iam.decentralizedclaims.spi.scope.DcpScope.DCP_SCOPE_VALUE_IRI;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.mockito.Mockito.mock;

class JsonObjectFromDcpScopeTransformerTest {

    private final TransformerContext context = mock();
    private final JsonObjectFromDcpScopeTransformer transformer =
            new JsonObjectFromDcpScopeTransformer(Json.createBuilderFactory(Map.of()));

    @Test
    void transform_defaultScope() {
        var scope = DcpScope.Builder.newInstance().id("scope-1").value("org.example.scope").profile("profile-1").build();

        var result = transformer.transform(scope, context);

        assertThat(result).isNotNull();
        assertThat(result.getString(ID)).isEqualTo("scope-1");
        assertThat(result.getString(TYPE)).isEqualTo(DCP_SCOPE_TYPE_IRI);
        assertThat(result.getString(DCP_SCOPE_VALUE_IRI)).isEqualTo("org.example.scope");
        assertThat(result.getString(DCP_SCOPE_PROFILE_IRI)).isEqualTo("profile-1");
        assertThat(result.getString(DCP_SCOPE_TYPE_PROPERTY_IRI)).isEqualTo("DEFAULT");
        assertThat(result.containsKey(DCP_SCOPE_PREFIX_MAPPING_IRI)).isFalse();
    }

    @Test
    void transform_policyScope() {
        var scope = DcpScope.Builder.newInstance().id("scope-2").value("org.example.scope")
                .type(DcpScope.Type.POLICY).prefixMapping("mapping").build();

        var result = transformer.transform(scope, context);

        assertThat(result).isNotNull();
        assertThat(result.getString(DCP_SCOPE_TYPE_PROPERTY_IRI)).isEqualTo("POLICY");
        assertThat(result.getString(DCP_SCOPE_PREFIX_MAPPING_IRI)).isEqualTo("mapping");
    }
}
