/*
 *  Copyright (c) 2024 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.policy.contract;

import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Policy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;

import static java.time.Instant.now;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.policy.model.Operator.EQ;
import static org.eclipse.edc.policy.model.Operator.GEQ;
import static org.eclipse.edc.policy.model.Operator.GT;
import static org.eclipse.edc.policy.model.Operator.LEQ;
import static org.eclipse.edc.policy.model.Operator.LT;
import static org.eclipse.edc.policy.model.Operator.NEQ;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class ContractExpiryCheckTest {

    private final ContractExpiryCheck contractExpiryCheck = new ContractExpiryCheck();

    @Test
    void shouldFail_whenRightValueIsNull() {
        var result = contractExpiryCheck.evaluate(EQ, null, Instant.now(), null);

        assertThat(result).isFailed();
    }

    @Test
    void shouldFail_whenRightValueIsNotString() {
        var result = contractExpiryCheck.evaluate(EQ, 3, Instant.now(), null);

        assertThat(result).isFailed();
    }

    @Test
    void shouldFail_whenRightValueIsNotParsable() {
        var result = contractExpiryCheck.evaluate(EQ, "unparsable", Instant.now(), null);

        assertThat(result).isFailed();
    }

    @ParameterizedTest
    @ArgumentsSource(ValidInstant.class)
    void shouldSucceed_whenRightValueIsIso8061AndValid(Instant now, Operator operator, Instant bound) {
        var result = contractExpiryCheck.evaluate(operator, bound.toString(), now, null);

        assertThat(result).isSucceeded();
    }

    @ParameterizedTest
    @ArgumentsSource(InvalidInstant.class)
    void shouldFail_whenRightValueIsIso8061AndInvalid(Instant now, Operator operator, Instant bound) {
        var result = contractExpiryCheck.evaluate(operator, bound.toString(), now, null);

        assertThat(result).isFailed();
    }

    @ParameterizedTest
    @ArgumentsSource(ValidDuration.class)
    void shouldSucceed_whenRightValueIsDurationAndValid(Instant now, Operator operator, String duration, Instant signingTime) {
        var result = contractExpiryCheck.evaluate(operator, "contractAgreement+" + duration, now, createAgreement(signingTime));

        assertThat(result).isSucceeded();
    }

    @ParameterizedTest
    @ArgumentsSource(InvalidDuration.class)
    void shouldFail_whenRightValueIsDurationAndInvalid(Instant now, Operator operator, String duration, Instant signingTime) {
        var result = contractExpiryCheck.evaluate(operator, "contractAgreement+" + duration, now, createAgreement(signingTime));

        assertThat(result).isFailed();
    }

    private static class ValidInstant implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            var now = now();
            return Stream.of(
                    arguments(now, EQ, now),
                    arguments(now, GEQ, now),
                    arguments(now, GEQ, now.minusNanos(1)),
                    arguments(now, LEQ, now),
                    arguments(now, LEQ, now.plusNanos(1)),
                    arguments(now, GT, now.minusNanos(1)),
                    arguments(now, LT, now.plusNanos(1)),
                    arguments(now, NEQ, now.minusNanos(1))
            );
        }
    }

    private static class InvalidInstant implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            var now = now();
            return Stream.of(
                    arguments(now, EQ, now.plusNanos(1)),
                    arguments(now, GEQ, now.plusNanos(1)),
                    arguments(now, LEQ, now.minusNanos(1)),
                    arguments(now, GT, now),
                    arguments(now, LT, now),
                    arguments(now, NEQ, now)
            );
        }
    }

    private static class ValidDuration implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            var now = now();
            return Stream.of(
                    arguments(now, GT, "34s", now.minus(Duration.ofSeconds(35))),
                    arguments(now, GT, "-23m", now.plus(Duration.ofMinutes(22))),
                    arguments(now, GT, "4h", now.minus(Duration.ofHours(5))),
                    arguments(now, GT, "-1d", now.plus(Duration.ofHours(23))),
                    arguments(now, LT, "-54s", now.plus(Duration.ofSeconds(55))),
                    arguments(now, LT, "17m", now.minus(Duration.ofMinutes(16))),
                    arguments(now, LT, "-2h", now.plus(Duration.ofHours(3))),
                    arguments(now, LT, "1d", now.minusSeconds(59))
            );
        }
    }

    private static class InvalidDuration implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            var now = now();
            return Stream.of(
                    arguments(now, GT, "34s", now.minus(Duration.ofSeconds(33))),
                    arguments(now, GT, "-23m", now.plus(Duration.ofMinutes(24))),
                    arguments(now, GT, "4h", now.minus(Duration.ofHours(3))),
                    arguments(now, GT, "-1d", now.plus(Duration.ofHours(25))),
                    arguments(now, LT, "-54s", now.plus(Duration.ofSeconds(53))),
                    arguments(now, LT, "17m", now.minus(Duration.ofMinutes(18))),
                    arguments(now, LT, "-2h", now.plus(Duration.ofHours(1))),
                    arguments(now, LT, "1d", now.minus(Duration.ofDays(2)))
            );
        }
    }

    private ContractAgreement createAgreement(Instant signingTime) {
        return ContractAgreement.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .providerId(UUID.randomUUID().toString())
                .consumerId(UUID.randomUUID().toString())
                .assetId(UUID.randomUUID().toString())
                .contractSigningDate(signingTime.getEpochSecond())
                .policy(Policy.Builder.newInstance().build())
                .build();
    }

}
