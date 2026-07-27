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

package org.eclipse.edc.iam.verifiablecredentials.rules;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.eclipse.edc.iam.verifiablecredentials.spi.TestFunctions.createCredentialBuilder;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

class IsInValidityPeriodTest {

    private static final Instant NOW = Instant.parse("2024-06-01T12:00:00Z");
    private final Clock clock = Clock.fixed(NOW, ZoneId.of("UTC"));
    private final IsInValidityPeriod rule = new IsInValidityPeriod(clock);

    @Test
    void credentialAlreadyValid_noExpirationDate_shouldSucceed() {
        var vc = createCredentialBuilder()
                .issuanceDate(NOW.minusSeconds(60))
                .build();

        assertThat(rule.apply(vc)).isSucceeded();
    }

    @Test
    void credentialAlreadyValid_notYetExpired_shouldSucceed() {
        var vc = createCredentialBuilder()
                .issuanceDate(NOW.minusSeconds(60))
                .expirationDate(NOW.plusSeconds(60))
                .build();

        assertThat(rule.apply(vc)).isSucceeded();
    }

    @Test
    void credentialNotYetValid_shouldFail() {
        var vc = createCredentialBuilder()
                .issuanceDate(NOW.plusSeconds(60))
                .build();

        assertThat(rule.apply(vc))
                .isFailed()
                .detail()
                .isEqualTo("Credential is not yet valid.");
    }

    @Test
    void credentialExpired_shouldFail() {
        var vc = createCredentialBuilder()
                .issuanceDate(NOW.minusSeconds(120))
                .expirationDate(NOW.minusSeconds(60))
                .build();

        assertThat(rule.apply(vc))
                .isFailed()
                .detail()
                .isEqualTo("Credential expired.");
    }

    @Test
    void issuanceDateEqualsNow_shouldSucceed() {
        var vc = createCredentialBuilder()
                .issuanceDate(NOW)
                .build();

        assertThat(rule.apply(vc)).isSucceeded();
    }

    @Test
    void expirationDateEqualsNow_shouldSucceed() {
        var vc = createCredentialBuilder()
                .issuanceDate(NOW.minusSeconds(60))
                .expirationDate(NOW)
                .build();

        assertThat(rule.apply(vc)).isSucceeded();
    }
}
