/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.identitytrust.model;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.net.URI;
import java.util.Date;
import java.util.Map;
import java.util.stream.Stream;

import static java.time.Instant.now;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProofTest {

    @Test
    void embeddedProof() {
        assertThatNoException()
                .isThrownBy(() -> Proof.Builder.newInstance()
                        .type("test-type")
                        .created(Date.from(now()))
                        .proofPurpose("assertionMethod")
                        .verificationMethod(Map.of("type", "JsonWebKey2020",
                                "publicKeyJwk", Map.of("kty", "EC",
                                        "crv", "P-384",
                                        "x", "test-X",
                                        "y", "test-Y"),
                                "id", "test-id"))
                        .proofContent("jws", "test-jws-value")
                        .build());
    }

    @Test
    void linkedProof() {
        assertThatNoException()
                .isThrownBy(() -> Proof.Builder.newInstance()
                        .type("test-type")
                        .created(Date.from(now()))
                        .proofPurpose("assertionMethod")
                        .verificationMethod(URI.create("https://there.is.my/proof"))
                        .proofContent("jws", "test-jws-value")
                        .build());
    }

    @Test
    void emptyProofContent() {
        assertThatThrownBy(() -> Proof.Builder.newInstance()
                .type("test-type")
                .created(Date.from(now()))
                .proofPurpose("assertionMethod")
                .verificationMethod(URI.create("https://there.is.my/proof"))
                .build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void illegalProof() {
        assertThatThrownBy(() -> Proof.Builder.newInstance()
                .type("test-type")
                .created(Date.from(now()))
                .proofPurpose("assertionMethod")
                .verificationMethod("some-proof") //violation: must be URI or Map
                .proofContent("jws", "test-jws-value")
                .build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ArgumentsSource(IllegalArgumentsProvider.class)
    void assertRequiredFields(String type, Date created, Object verificationMethod, String proofPurpose) {
        assertThatThrownBy(() -> Proof.Builder.newInstance()
                .type(type)
                .created(created)
                .proofPurpose(proofPurpose)
                .verificationMethod(verificationMethod)
                .proofContent("jws", "test-jws-value")
                .build())
                .isInstanceOf(NullPointerException.class);
    }

    private static class IllegalArgumentsProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            return Stream.of(
                    Arguments.of(null, Date.from(now()), URI.create("http://there.is.my/proof"), "assertionMethod"),
                    Arguments.of("test-type", null, URI.create("http://there.is.my/proof"), "assertionMethod"),
                    Arguments.of("test-type", Date.from(now()), null, "assertionMethod"),
                    Arguments.of("test-type", Date.from(now()), URI.create("http://there.is.my/proof"), null)
            );
        }
    }
}