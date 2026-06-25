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
import jakarta.json.JsonObject;
import org.eclipse.edc.iam.decentralizedclaims.spi.scope.DcpScope;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.decentralizedclaims.spi.scope.DcpScope.DCP_SCOPE_TYPE_TERM;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.test.TestJsonLd.expand;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.mockito.Mockito.mock;

class JsonObjectToDcpScopeTransformerTest {

    private final TransformerContext context = mock();
    private final JsonObjectToDcpScopeTransformer transformer = new JsonObjectToDcpScopeTransformer();

    @Test
    void types() {
        assertThat(transformer.getInputType()).isEqualTo(JsonObject.class);
        assertThat(transformer.getOutputType()).isEqualTo(DcpScope.class);
    }

    @Test
    void transform_defaultScope() {
        var json = Json.createObjectBuilder()
                .add("@context", Json.createObjectBuilder().add("@vocab", EDC_NAMESPACE))
                .add(TYPE, DCP_SCOPE_TYPE_TERM)
                .add(ID, "scope-1")
                .add("value", "org.example.scope")
                .add("profile", "profile-1")
                .build();

        var result = transformer.transform(expand(json), context);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("scope-1");
        assertThat(result.getValue()).isEqualTo("org.example.scope");
        assertThat(result.getProfile()).isEqualTo("profile-1");
        assertThat(result.getType()).isEqualTo(DcpScope.Type.DEFAULT);
        assertThat(result.getPrefixMapping()).isNull();
    }

    @Test
    void transform_policyScope() {
        var json = Json.createObjectBuilder()
                .add("@context", Json.createObjectBuilder().add("@vocab", EDC_NAMESPACE))
                .add(TYPE, DCP_SCOPE_TYPE_TERM)
                .add(ID, "scope-2")
                .add("value", "org.example.scope")
                .add("type", "POLICY")
                .add("prefixMapping", "mapping")
                .build();

        var result = transformer.transform(expand(json), context);

        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(DcpScope.Type.POLICY);
        assertThat(result.getPrefixMapping()).isEqualTo("mapping");
    }
}
