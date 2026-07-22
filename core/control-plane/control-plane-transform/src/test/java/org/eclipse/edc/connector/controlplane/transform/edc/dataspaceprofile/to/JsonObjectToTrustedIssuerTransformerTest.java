/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.transform.edc.dataspaceprofile.to;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.protocol.spi.TrustedIssuer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.protocol.spi.TrustedIssuer.TRUSTED_ISSUER_SUPPORTED_TYPES_IRI;
import static org.eclipse.edc.protocol.spi.TrustedIssuer.TRUSTED_ISSUER_TYPE_IRI;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class JsonObjectToTrustedIssuerTransformerTest {

    private final TransformerContext context = mock();
    private final JsonObjectToTrustedIssuerTransformer transformer = new JsonObjectToTrustedIssuerTransformer();

    @Test
    void types() {
        assertThat(transformer.getInputType()).isEqualTo(JsonObject.class);
        assertThat(transformer.getOutputType()).isEqualTo(TrustedIssuer.class);
    }

    @Test
    void transform() {
        var json = Json.createObjectBuilder()
                .add("@context", Json.createObjectBuilder().add("@vocab", EDC_NAMESPACE))
                .add("@id", "did:web:trusted.issuer")
                .add("@type", TRUSTED_ISSUER_TYPE_IRI)
                .add(TRUSTED_ISSUER_SUPPORTED_TYPES_IRI, Json.createArrayBuilder()
                        .add("MembershipCredential")
                        .add("OnboardingCredential"))
                .build();

        var result = transformer.transform(json, context);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("did:web:trusted.issuer");
        assertThat(result.getSupportedTypes()).containsExactly("MembershipCredential", "OnboardingCredential");
        verifyNoInteractions(context);
    }

    @Test
    void transform_withNoSupportedTypes() {
        var json = Json.createObjectBuilder()
                .add("@context", Json.createObjectBuilder().add("@vocab", EDC_NAMESPACE))
                .add("@id", "did:web:trusted.issuer")
                .add("@type", TRUSTED_ISSUER_TYPE_IRI)
                .build();

        var result = transformer.transform(json, context);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("did:web:trusted.issuer");
        assertThat(result.getSupportedTypes()).isEmpty();
        verifyNoInteractions(context);
    }
}
