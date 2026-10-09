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

package org.eclipse.edc.participantcontext.spi.config.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.participantcontext.spi.config.validation.ParticipantConfigValidators.bool;
import static org.eclipse.edc.participantcontext.spi.config.validation.ParticipantConfigValidators.httpUrl;
import static org.eclipse.edc.participantcontext.spi.config.validation.ParticipantConfigValidators.longValue;
import static org.eclipse.edc.participantcontext.spi.config.validation.ParticipantConfigValidators.notBlank;
import static org.eclipse.edc.participantcontext.spi.config.validation.ParticipantConfigValidators.oneOf;
import static org.eclipse.edc.participantcontext.spi.config.validation.ParticipantConfigValidators.startsWith;

class ParticipantConfigValidatorsTest {

    @Test
    void notBlank_validation() {
        assertThat(notBlank().validate("value")).isSucceeded();
        assertThat(notBlank().validate(" ")).isFailed().detail().isEqualTo("must not be blank");
    }

    @Test
    void longValue_validation() {
        assertThat(longValue().validate("42")).isSucceeded();
        assertThat(longValue(1, 10).validate(" 10 ")).isSucceeded();
        assertThat(longValue(1, 10).validate("11")).isFailed().detail().isEqualTo("must be between 1 and 10");
        assertThat(longValue().validate("4.2")).isFailed().detail().isEqualTo("must be an integer number");
    }

    @Test
    void bool_validation() {
        assertThat(bool().validate("true")).isSucceeded();
        assertThat(bool().validate("FALSE")).isSucceeded();
        assertThat(bool().validate("yes")).isFailed();
    }

    @ParameterizedTest
    @ValueSource(strings = { "http://host", "https://host:8080/path?query" })
    void httpUrl_shouldSucceed(String value) {
        assertThat(httpUrl().validate(value)).isSucceeded();
    }

    @ParameterizedTest
    @ValueSource(strings = { "host", "/path", "ftp://host", "http://", "not an url" })
    void httpUrl_shouldFail(String value) {
        assertThat(httpUrl().validate(value)).isFailed().detail().isEqualTo("must be an absolute http(s) URL");
    }

    @Test
    void startsWith_validation() {
        assertThat(startsWith("did:").validate("did:web:host")).isSucceeded();
        assertThat(startsWith("did:").validate("web:host")).isFailed().detail().isEqualTo("must start with 'did:'");
    }

    @Test
    void oneOf_validation() {
        var validator = oneOf(() -> Set.of("b", "a"));

        assertThat(validator.validate("a")).isSucceeded();
        assertThat(validator.validate("c")).isFailed().detail().isEqualTo("must be one of [a, b]");
    }
}
