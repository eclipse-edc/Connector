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

package org.eclipse.edc.iam.verifiablecredentials.spi.model.bitstringstatuslist;


import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.bitstringstatuslist.BitstringStatusListStatus.STATUS_LIST_CREDENTIAL;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.bitstringstatuslist.BitstringStatusListStatus.STATUS_LIST_INDEX;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.bitstringstatuslist.BitstringStatusListStatus.STATUS_LIST_MESSAGES;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.bitstringstatuslist.BitstringStatusListStatus.STATUS_LIST_PURPOSE;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.bitstringstatuslist.BitstringStatusListStatus.STATUS_LIST_REFERENCE;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.bitstringstatuslist.BitstringStatusListStatus.STATUS_LIST_SIZE;

class BitstringStatusListStatusTest {

    @Test
    void parse_fullySpecified() {
        var props = Map.of(
                STATUS_LIST_PURPOSE, "revocation",
                STATUS_LIST_INDEX, "237",
                STATUS_LIST_CREDENTIAL, "https://example.com/credentials/status/3",
                STATUS_LIST_MESSAGES,
                List.of(new StatusMessage("0x0", "foo"),
                        new StatusMessage("0x1", "bar"),
                        new StatusMessage("0x2", "qooz"),
                        new StatusMessage("0x3", "qaz")),
                STATUS_LIST_SIZE, "2",
                STATUS_LIST_REFERENCE, List.of("ref1", "ref2")
        );
        var credentialStatus = new CredentialStatus("https://example.com/credentials/status/3#94567", BitstringStatusListStatus.TYPE, props);

        var parsed = BitstringStatusListStatus.parse(credentialStatus);
        assertThat(parsed.getStatusListCredential()).isEqualTo("https://example.com/credentials/status/3");
        assertThat(parsed.getStatusListIndex()).isEqualTo(237);
        assertThat(parsed.getStatusListPurpose()).isEqualTo("revocation");
        assertThat(parsed.getStatusMessage()).hasSize(4);
        assertThat(parsed.getStatusReference()).hasSize(2);
    }

    @Test
    void parse_sparselyPopulated() {
        var props = Map.of(
                STATUS_LIST_PURPOSE, "revocation",
                STATUS_LIST_INDEX, "237",
                STATUS_LIST_CREDENTIAL, "https://example.com/credentials/status/3",
                STATUS_LIST_MESSAGES,
                List.of(new StatusMessage("0x0", "foo"),
                        new StatusMessage("0x1", "bar")),
                STATUS_LIST_SIZE, "1"
        );
        var credentialStatus = new CredentialStatus("https://example.com/credentials/status/3#94567", BitstringStatusListStatus.TYPE, props);

        var parsed = BitstringStatusListStatus.parse(credentialStatus);
        assertThat(parsed.getStatusListCredential()).isEqualTo("https://example.com/credentials/status/3");
        assertThat(parsed.getStatusListIndex()).isEqualTo(237);
        assertThat(parsed.getStatusListPurpose()).isEqualTo("revocation");
        assertThat(parsed.getStatusMessage()).hasSize(2);
    }

    @Test
    void parse_missingPurpose() {
        Map<String, Object> props = Map.of(
                //"statusPurpose", "revocation",
                STATUS_LIST_INDEX, "237",
                STATUS_LIST_CREDENTIAL, "https://example.com/credentials/status/3"
        );
        var credentialStatus = new CredentialStatus("https://example.com/credentials/status/3#94567", BitstringStatusListStatus.TYPE, props);
        assertThatThrownBy(() -> BitstringStatusListStatus.parse(credentialStatus))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("statusPurpose");
    }

    @Test
    void parse_missingIndex() {
        Map<String, Object> props = Map.of(
                STATUS_LIST_PURPOSE, "revocation",
                //"statusListIndex", "237",
                STATUS_LIST_CREDENTIAL, "https://example.com/credentials/status/3"
        );
        var credentialStatus = new CredentialStatus("https://example.com/credentials/status/3#94567", BitstringStatusListStatus.TYPE, props);
        assertThatThrownBy(() -> BitstringStatusListStatus.parse(credentialStatus)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("statusListIndex");
    }

    @Test
    void parse_missingCredential() {
        Map<String, Object> props = Map.of(
                STATUS_LIST_PURPOSE, "revocation",
                STATUS_LIST_INDEX, "237"
                // "statusListCredential", "https://example.com/credentials/status/3"
        );
        var credentialStatus = new CredentialStatus("https://example.com/credentials/status/3#94567", BitstringStatusListStatus.TYPE, props);
        assertThatThrownBy(() -> BitstringStatusListStatus.parse(credentialStatus)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("statusListCredential");
    }

    @Test
    void parse_statusSize_doesNotMatch_statusMessageLength_expectException() {
        var props = Map.of(
                STATUS_LIST_PURPOSE, "revocation",
                STATUS_LIST_INDEX, "237",
                STATUS_LIST_CREDENTIAL, "https://example.com/credentials/status/3",
                STATUS_LIST_MESSAGES,
                List.of(new StatusMessage("0x0", "foo"),
                        new StatusMessage("0x1", "bar"),
                        new StatusMessage("0x2", "qooz"),
                        new StatusMessage("0x3", "qaz")),
                STATUS_LIST_SIZE, "3" // would expect 8 statusMessage entries
        );
        var credentialStatus = new CredentialStatus("https://example.com/credentials/status/3#94567", BitstringStatusListStatus.TYPE, props);

        assertThatThrownBy(() -> BitstringStatusListStatus.parse(credentialStatus)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("If present, statusSize (in bits) must be equal to the number of possible statusMessage entries");
    }

    @Test
    void parse_statusSizeNotSpecified_statusMessageSpecified_expectException() {
        var props = Map.of(
                STATUS_LIST_PURPOSE, "revocation",
                STATUS_LIST_INDEX, "237",
                STATUS_LIST_CREDENTIAL, "https://example.com/credentials/status/3",
                STATUS_LIST_MESSAGES,
                List.of(new StatusMessage("0x0", "foo"),
                        new StatusMessage("0x1", "bar"),
                        new StatusMessage("0x2", "qooz"),
                        new StatusMessage("0x3", "qaz"))
                //STATUS_LIST_SIZE, "3"
        );
        var credentialStatus = new CredentialStatus("https://example.com/credentials/status/3#94567", BitstringStatusListStatus.TYPE, props);

        assertThatThrownBy(() -> BitstringStatusListStatus.parse(credentialStatus)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("statusSize must be specified and > 1 if statusMessage is present.");
    }


    @Test
    void parse_statusSizeSpecified_statusMessageNotSpecified_succeeds() {
        Map<String, Object> props = Map.of(
                STATUS_LIST_PURPOSE, "revocation",
                STATUS_LIST_INDEX, "237",
                STATUS_LIST_CREDENTIAL, "https://example.com/credentials/status/3",
                STATUS_LIST_SIZE, "1"
        );
        var credentialStatus = new CredentialStatus("https://example.com/credentials/status/3#94567", BitstringStatusListStatus.TYPE, props);

        assertThatNoException().isThrownBy(() -> BitstringStatusListStatus.parse(credentialStatus));
    }

    @ParameterizedTest(name = "statusSize (bits): {0}")
    @ValueSource(ints = { 1, 2, 10, 1000, Integer.MAX_VALUE })
    void parse_statusSizeInvalid_statusMessageNotSpecified_succeeds(int statusSize) {
        Map<String, Object> props = Map.of(
                STATUS_LIST_PURPOSE, "revocation",
                STATUS_LIST_INDEX, "237",
                STATUS_LIST_CREDENTIAL, "https://example.com/credentials/status/3",
                STATUS_LIST_SIZE, statusSize
        );
        var credentialStatus = new CredentialStatus("https://example.com/credentials/status/3#94567", BitstringStatusListStatus.TYPE, props);
        assertThatNoException().isThrownBy(() -> BitstringStatusListStatus.parse(credentialStatus));
    }

    @Test
    void parse_statusSizeNotSpecified_statusMessageNotSpecified_shouldSucceed() {
        Map<String, Object> props = Map.of(
                STATUS_LIST_PURPOSE, "revocation",
                STATUS_LIST_INDEX, "237",
                STATUS_LIST_CREDENTIAL, "https://example.com/credentials/status/3"
        );
        var credentialStatus = new CredentialStatus("https://example.com/credentials/status/3#94567", BitstringStatusListStatus.TYPE, props);

        assertThatNoException().isThrownBy(() -> BitstringStatusListStatus.parse(credentialStatus));
    }

    @ParameterizedTest(name = "Invalid statusSize {0}")
    @ValueSource(ints = { 0, -1, -10 })
    void parse_statusSizeNegative_expectException(Object size) {
        Map<String, Object> props = Map.of(
                STATUS_LIST_PURPOSE, "revocation",
                STATUS_LIST_INDEX, "237",
                STATUS_LIST_CREDENTIAL, "https://example.com/credentials/status/3",
                STATUS_LIST_SIZE, size
        );
        var credentialStatus = new CredentialStatus("https://example.com/credentials/status/3#94567", BitstringStatusListStatus.TYPE, props);

        assertThatThrownBy(() -> BitstringStatusListStatus.parse(credentialStatus)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("If present, statusSize must be a positive integer > 0 but was '%s'.".formatted(size));
    }
}