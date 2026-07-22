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

package org.eclipse.edc.protocol.spi;

import org.eclipse.edc.json.JacksonTypeManager;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TrustedIssuerTest {

    private final JacksonTypeManager typeManager = new JacksonTypeManager();

    @Test
    void serdes() {
        var issuer = TrustedIssuer.Builder.newInstance()
                .id("did:web:trusted.issuer")
                .supportedTypes(List.of("MembershipCredential", "OnboardingCredential"))
                .build();

        var json = typeManager.writeValueAsString(issuer);
        var deserialized = typeManager.readValue(json, TrustedIssuer.class);

        assertThat(deserialized).usingRecursiveComparison().isEqualTo(issuer);
    }
}
