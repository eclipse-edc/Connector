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

package org.eclipse.edc.iam.decentralizedclaims.core.validation;

import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CapabilityInvocationValidationRuleTest {

    private static final String ISSUER = "did:web:consumer";
    private static final String KEY_ID = "did:web:consumer#key-1";

    private final DidResolverRegistry resolverRegistry = mock();

    @Test
    void keyInCapabilityInvocation() {
        var rule = new CapabilityInvocationValidationRule(KEY_ID, resolverRegistry);
        when(resolverRegistry.resolve(ISSUER)).thenReturn(Result.success(didDocument(List.of(KEY_ID))));

        assertThat(rule.checkRule(claimToken(ISSUER), Map.of())).isSucceeded();
    }

    @Test
    void keyNotInCapabilityInvocation() {
        var rule = new CapabilityInvocationValidationRule(KEY_ID, resolverRegistry);
        when(resolverRegistry.resolve(ISSUER)).thenReturn(Result.success(didDocument(List.of("did:web:consumer#other-key"))));

        assertThat(rule.checkRule(claimToken(ISSUER), Map.of())).isFailed()
                .detail()
                .isEqualTo("The key 'did:web:consumer#key-1' is not listed in the 'capabilityInvocation' section of the DID document 'did:web:consumer'");
    }

    @Test
    void emptyCapabilityInvocation() {
        var rule = new CapabilityInvocationValidationRule(KEY_ID, resolverRegistry);
        when(resolverRegistry.resolve(ISSUER)).thenReturn(Result.success(didDocument(List.of())));

        assertThat(rule.checkRule(claimToken(ISSUER), Map.of())).isFailed();
    }

    @Test
    void issuerClaimMissing() {
        var rule = new CapabilityInvocationValidationRule(KEY_ID, resolverRegistry);
        var token = ClaimToken.Builder.newInstance().build();

        assertThat(rule.checkRule(token, Map.of())).isFailed()
                .detail()
                .isEqualTo("Required 'iss' claim is missing");
    }

    @Test
    void didResolutionFails() {
        var rule = new CapabilityInvocationValidationRule(KEY_ID, resolverRegistry);
        when(resolverRegistry.resolve(ISSUER)).thenReturn(Result.failure("DID not found"));

        assertThat(rule.checkRule(claimToken(ISSUER), Map.of())).isFailed()
                .detail()
                .contains("DID not found");
    }

    private ClaimToken claimToken(String iss) {
        return ClaimToken.Builder.newInstance().claim("iss", iss).build();
    }

    private DidDocument didDocument(List<String> capabilityInvocations) {
        return DidDocument.Builder.newInstance()
                .id(ISSUER)
                .capabilityInvocation(capabilityInvocations)
                .build();
    }
}
