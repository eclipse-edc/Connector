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

package org.eclipse.edc.token.rules;

import org.eclipse.edc.jwt.validation.jti.JtiValidationEntry;
import org.eclipse.edc.jwt.validation.jti.JtiValidationStore;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.token.InMemoryJtiValidationStore;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JtiUnusedValidationRuleTest {

    private final JtiValidationStore store = new InMemoryJtiValidationStore();
    private final JtiUnusedValidationRule rule = new JtiUnusedValidationRule(store, mock());

    @Test
    void checkRule_whenNoJti_shouldSucceed() {
        var failingStore = mock(JtiValidationStore.class);

        var result = new JtiUnusedValidationRule(failingStore, mock()).checkRule(ClaimToken.Builder.newInstance().build(), Map.of());

        assertThat(result).isSucceeded();
        verifyNoInteractions(failingStore);
    }

    @Test
    void checkRule_whenUnused_shouldSucceedWithoutStoringIt() {
        assertThat(rule.checkRule(token("test-id"), Map.of())).isSucceeded();

        assertThat(store.findById("test-id", false)).isNull();
    }

    @Test
    void checkRule_whenUsed_shouldFailAndKeepEntry() {
        store.storeEntry(new JtiValidationEntry("test-id", Instant.now().plusSeconds(3600).toEpochMilli()));

        assertThat(rule.checkRule(token("test-id"), Map.of())).isFailed()
                .detail().isEqualTo("The JWT id 'test-id' was already used.");
        assertThat(rule.checkRule(token("test-id"), Map.of())).isFailed();
        assertThat(store.findById("test-id", false)).isNotNull();
    }

    @Test
    void checkRule_whenUsedWithoutExpiration_shouldFail() {
        store.storeEntry(new JtiValidationEntry("test-id", null));

        assertThat(rule.checkRule(token("test-id"), Map.of())).isFailed();
    }

    @Test
    void checkRule_whenEntryExpired_shouldSucceed() {
        store.storeEntry(new JtiValidationEntry("test-id", Instant.now().minusSeconds(10).toEpochMilli()));

        assertThat(rule.checkRule(token("test-id"), Map.of())).isSucceeded();
    }

    @Test
    void checkRule_whenStoreFails_shouldFailRetryable() {
        var failingStore = mock(JtiValidationStore.class);
        when(failingStore.findById(anyString(), anyBoolean())).thenThrow(new EdcPersistenceException("database not reachable"));

        var result = new JtiUnusedValidationRule(failingStore, mock()).checkRule(token("test-id"), Map.of());

        assertThat(result).isFailed();
        assertThat(result.isRetryable()).isTrue();
    }

    private ClaimToken token(String jti) {
        return ClaimToken.Builder.newInstance().claim("jti", jti).build();
    }
}
