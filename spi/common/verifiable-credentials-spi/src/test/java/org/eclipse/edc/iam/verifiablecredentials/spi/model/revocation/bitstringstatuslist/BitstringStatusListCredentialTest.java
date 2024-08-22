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

package org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.bitstringstatuslist;

import org.assertj.core.api.ThrowableAssert;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.bitstringstatuslist.BitstringStatusListCredential.STATUS_LIST_ENCODED_LIST;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.bitstringstatuslist.BitstringStatusListCredential.STATUS_LIST_TTL;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.bitstringstatuslist.BitstringStatusListStatus.STATUS_LIST_PURPOSE;

class BitstringStatusListCredentialTest {
    @Test
    void parseBitStringStatusList() {
        // values taken from https://www.w3.org/TR/vc-bitstring-status-list/#example-example-bitstringstatuslistcredential
        //noinspection unchecked
        var credential = BitstringStatusListCredential.Builder.newInstance()
                .id("https://example.com/credentials/status/3")
                .types(List.of("VerifiableCredential", "BitstringStatusListCredential"))
                .issuer(new Issuer("did:example:12345", Map.of()))
                .issuanceDate(Instant.parse("2021-04-05T14:27:40Z"))
                .credentialSubject(CredentialSubject.Builder.newInstance()
                        .id("https://example.com/status/3#list")
                        .claim("type", "BitstringStatusList")
                        .claim(STATUS_LIST_PURPOSE, "revocation")
                        .claim(STATUS_LIST_ENCODED_LIST, "uH4sIAAAAAAAAA-3BMQEAAADCoPVPbQwfoAAAAAAAAAAAAAAAAAAAAIC3AYbSVKsAQAAA")
                        .claim(STATUS_LIST_TTL, 123_456)
                        .build())
                .build();


        assertThat(credential.encodedList()).isNotNull();
        assertThat(credential.statusPurpose()).isEqualTo("revocation");
        assertThat(credential.validFrom()).isEqualTo(Instant.parse("2021-04-05T14:27:40Z"));
        assertThat(credential.validUntil()).isNull();
        assertThat(credential.ttl()).hasMillis(123_456);
    }

    @Test
    void parse_useDefaultValue_ifTtlNotPresent() {
        // values taken from https://www.w3.org/TR/vc-bitstring-status-list/#example-example-bitstringstatuslistcredential
        //noinspection unchecked
        var credential = BitstringStatusListCredential.Builder.newInstance()
                .id("https://example.com/credentials/status/3")
                .types(List.of("VerifiableCredential", "BitstringStatusListCredential"))
                .issuer(new Issuer("did:example:12345", Map.of()))
                .issuanceDate(Instant.parse("2021-04-05T14:27:40Z"))
                .credentialSubject(CredentialSubject.Builder.newInstance()
                        .id("https://example.com/status/3#list")
                        .claim("type", "BitstringStatusList")
                        .claim(STATUS_LIST_PURPOSE, "revocation")
                        .claim(STATUS_LIST_ENCODED_LIST, "uH4sIAAAAAAAAA-3BMQEAAADCoPVPbQwfoAAAAAAAAAAAAAAAAAAAAIC3AYbSVKsAQAAA")
                        .build())
                .build();


        assertThat(credential.ttl()).hasMillis(300_000);
    }

    @Test
    void parse_noPurpose_expectException() {
        ThrowableAssert.ThrowingCallable action = () -> BitstringStatusListCredential.Builder.newInstance()
                .id("https://example.com/credentials/status/3")
                .types(List.of("VerifiableCredential", "BitstringStatusListCredential"))
                .issuer(new Issuer("did:example:12345", Map.of()))
                .issuanceDate(Instant.parse("2021-04-05T14:27:40Z"))
                .credentialSubject(CredentialSubject.Builder.newInstance()
                        .id("https://example.com/status/3#list")
                        .claim("type", "BitstringStatusList")
                        // .claim(STATUS_LIST_PURPOSE, "revocation")
                        .claim(STATUS_LIST_ENCODED_LIST, "uH4sIAAAAAAAAA-3BMQEAAADCoPVPbQwfoAAAAAAAAAAAAAAAAAAAAIC3AYbSVKsAQAAA")
                        .claim(STATUS_LIST_TTL, 123_456)
                        .build())
                .build();

        assertThatThrownBy(action).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parse_noEncodedList_expectException() {
        //noinspection unchecked
        ThrowableAssert.ThrowingCallable action = () -> BitstringStatusListCredential.Builder.newInstance()
                .id("https://example.com/credentials/status/3")
                .types(List.of("VerifiableCredential", "BitstringStatusListCredential"))
                .issuer(new Issuer("did:example:12345", Map.of()))
                .issuanceDate(Instant.parse("2021-04-05T14:27:40Z"))
                .credentialSubject(CredentialSubject.Builder.newInstance()
                        .id("https://example.com/status/3#list")
                        .claim("type", "BitstringStatusList")
                        .claim(STATUS_LIST_PURPOSE, "revocation")
                        //.claim(STATUS_LIST_ENCODED_LIST, "uH4sIAAAAAAAAA-3BMQEAAADCoPVPbQwfoAAAAAAAAAAAAAAAAAAAAIC3AYbSVKsAQAAA")
                        .claim(STATUS_LIST_TTL, 123_456)
                        .build())
                .build();

        assertThatThrownBy(action).isInstanceOf(IllegalArgumentException.class);
    }

}