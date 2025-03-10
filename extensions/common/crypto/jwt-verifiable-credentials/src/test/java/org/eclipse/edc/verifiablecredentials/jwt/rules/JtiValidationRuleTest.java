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
import org.eclipse.edc.spi.result.StoreResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JtiValidationRuleTest {

    private final JtiValidationStore store = mock();
    private final JtiValidationRule rule = new JtiValidationRule(store, mock());

    @BeforeEach
    void setUp() {
        when(store.storeEntry(any())).thenReturn(StoreResult.success());
    }

    @Test
    void checkRule_noExpiration_success() {
        when(store.findById(eq("test-id"))).thenReturn(new JtiValidationEntry("test-id"));
        assertThat(rule.checkRule(ClaimToken.Builder.newInstance().claim("jti", "test-id").build(), Map.of())).isFailed()
                .detail().isEqualTo("The JWT id 'test-id' was already used.");
        verify(store).storeEntry(any());
    }

    @Test
    void checkRule_withExpiration_success() {
        when(store.findById(eq("test-id"))).thenReturn(new JtiValidationEntry("test-id", Instant.now().plusSeconds(3600).toEpochMilli()));
        assertThat(rule.checkRule(ClaimToken.Builder.newInstance().claim("jti", "test-id").build(), Map.of())).isFailed()
                .detail().isEqualTo("The JWT id 'test-id' was already used.");
        verify(store).storeEntry(any());
    }

    @Test
    void checkRule_withExpiration_alreadyExpired() {
        when(store.findById(eq("test-id"))).thenReturn(new JtiValidationEntry("test-id", Instant.now().minusSeconds(3600).toEpochMilli()));
        assertThat(rule.checkRule(ClaimToken.Builder.newInstance().claim("jti", "test-id").build(), Map.of())).isSucceeded();
        verify(store).storeEntry(any());
    }

    @Test
    void checkRule_entryNotFound_success() {
        when(store.findById(eq("test-id"))).thenReturn(null);
        assertThat(rule.checkRule(ClaimToken.Builder.newInstance().claim("jti", "test-id").build(), Map.of())).isSucceeded();
        verify(store).storeEntry(any());
    }

    @Test
    void checkRule_entryNotFound_storeFails_failure() {
        when(store.findById(eq("test-id"))).thenReturn(null);
        when(store.storeEntry(any())).thenReturn(StoreResult.duplicateKeys("foobar"));
        assertThat(rule.checkRule(ClaimToken.Builder.newInstance().claim("jti", "test-id").build(), Map.of())).isFailed()
                .detail().isEqualTo("foobar");
    }

    @Test
    void checkRule_whenClaimTokenNoKid() {
        assertThat(rule.checkRule(ClaimToken.Builder.newInstance().build(), Map.of())).isSucceeded();
        verify(store, never()).storeEntry(any());
    }
}