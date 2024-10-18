/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.verifiablecredentials.jwt.rules;

import org.eclipse.edc.jwt.validation.jti.JtiValidationEntry;
import org.eclipse.edc.jwt.validation.jti.JtiValidationStore;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JtiValidationRuleTest {

    private final JtiValidationStore store = mock();
    private final JtiValidationRule rule = new JtiValidationRule(store, mock());

    @Test
    void checkRule_noExpiration_success() {
        when(store.findById(eq("test-id"))).thenReturn(new JtiValidationEntry("test-id"));
        assertThat(rule.checkRule(ClaimToken.Builder.newInstance().claim("jti", "test-id").build(), Map.of())).isSucceeded();
    }

    @Test
    void checkRule_withExpiration_success() {
        when(store.findById(eq("test-id"))).thenReturn(new JtiValidationEntry("test-id", Instant.now().plusSeconds(3600).toEpochMilli()));
        assertThat(rule.checkRule(ClaimToken.Builder.newInstance().claim("jti", "test-id").build(), Map.of())).isSucceeded();
    }

    @Test
    void checkRule_withExpiration_alreadyExpired() {
        when(store.findById(eq("test-id"))).thenReturn(new JtiValidationEntry("test-id", Instant.now().minusSeconds(3600).toEpochMilli()));
        assertThat(rule.checkRule(ClaimToken.Builder.newInstance().claim("jti", "test-id").build(), Map.of())).isSucceeded();
    }

    @Test
    void checkRule_entryNotFound_success() {
        when(store.findById(eq("test-id"))).thenReturn(null);
        assertThat(rule.checkRule(ClaimToken.Builder.newInstance().claim("jti", "test-id").build(), Map.of())).isFailed()
                .detail().isEqualTo("The JWT id 'test-id' was not found");
    }
}