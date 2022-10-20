/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.iam.did.web.resolution;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class WebDidUrlResolverTest {

    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("provideDidResolutionScenario")
    void verifyDidResolution(String name, boolean useHttpsScheme, String did, String expectedUrl) {
        var resolver = new WebDidUrlResolver(useHttpsScheme);

        var url = resolver.apply(did);

        assertThat(url).isEqualTo(expectedUrl);
    }

    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("provideInvalidDid")
    void verifyIllegalArgumentExceptionThrownIsMissingDidScheme(String name, String did) {
        var resolver = new WebDidUrlResolver(true);

        assertThatIllegalArgumentException().isThrownBy(() -> resolver.apply(did));
    }

    private static Stream<Arguments> provideDidResolutionScenario() {
        return Stream.of(
                Arguments.of("HTTPS", true, "did:web:w3c-ccg.github.io", "https://w3c-ccg.github.io/.well-known/did.json"),
                Arguments.of("HTTP", false, "did:web:w3c-ccg.github.io", "http://w3c-ccg.github.io/.well-known/did.json"),
                Arguments.of("HTTPS WITH PATH", true, "did:web:w3c-ccg.github.io:user:alice", "https://w3c-ccg.github.io/user/alice/did.json"),
                Arguments.of("HTTPS WITH DOMAIN PORT", true, "did:web:example.com%3A3000:user:alice", "https://example.com:3000/user/alice/did.json")
        );
    }

    private static Stream<Arguments> provideInvalidDid() {
        return Stream.of(
                Arguments.of("MISSING DID SCHEME", "web:w3c-ccg.github.io:user:alice"),
                Arguments.of("MISSING DID METHOD", "did:w3c-ccg.github.io:user:alice:"),
                Arguments.of("INVALID DID FORMAT", "did:web:w3c-ccg.github.io:user:alice:")
        );
    }
}
