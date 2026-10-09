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

package org.eclipse.edc.token.rules;

import org.eclipse.edc.jwt.validation.jti.JtiValidationEntry;
import org.eclipse.edc.jwt.validation.jti.JtiValidationStore;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.result.StoreResult;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class JtiValidationRuleTest {

    private final JtiValidationStore store = mock();
    private final JtiValidationRule rule = new JtiValidationRule(store, mock());

    @Test
    void checkRule_whenFirstUse_shouldStoreEntryExpiringWithToken() {
        var expiration = Instant.now().plusSeconds(3600).getEpochSecond();
        when(store.storeEntry(any())).thenReturn(StoreResult.success());

        assertThat(rule.checkRule(ClaimToken.Builder.newInstance().claim("jti", "test-id").claim("exp", expiration).build(), Map.of())).isSucceeded();

        verify(store).storeEntry(new JtiValidationEntry("test-id", expiration * 1000));
        // looking the entry up before storing it would let a concurrent replay pass
        verifyNoMoreInteractions(store);
    }

    @Test
    void checkRule_whenTokenHasNoExpiration_shouldStoreEntryThatNeverExpires() {
        when(store.storeEntry(any())).thenReturn(StoreResult.success());

        assertThat(rule.checkRule(ClaimToken.Builder.newInstance().claim("jti", "test-id").build(), Map.of())).isSucceeded();

        verify(store).storeEntry(new JtiValidationEntry("test-id", null));
    }

    @Test
    void checkRule_whenAlreadyUsed_shouldFail() {
        when(store.storeEntry(any())).thenReturn(StoreResult.alreadyExists("foobar"));

        assertThat(rule.checkRule(ClaimToken.Builder.newInstance().claim("jti", "test-id").build(), Map.of())).isFailed()
                .detail().isEqualTo("The JWT id 'test-id' was already used.");
        verify(store).storeEntry(any());
        verifyNoMoreInteractions(store);
    }

    @Test
    void checkRule_whenStoreFails_shouldFail() {
        when(store.storeEntry(any())).thenReturn(StoreResult.generalError("foobar"));

        assertThat(rule.checkRule(ClaimToken.Builder.newInstance().claim("jti", "test-id").build(), Map.of())).isFailed()
                .detail().isEqualTo("foobar");
    }

    @Test
    void checkRule_whenClaimTokenNoJti() {
        assertThat(rule.checkRule(ClaimToken.Builder.newInstance().build(), Map.of())).isSucceeded();
        verifyNoInteractions(store);
    }
}
