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

package org.eclipse.edc.iam.verifiablecredentials.spi.model.statuslist;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialStatus;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StatusListStatusTest {

    @Test
    void verifyStatusList2021() {
        Map<String, Object> props = Map.of(
                "statusPurpose", "revocation",
                "statusListIndex", "237",
                "statusListCredential", "https://example.com/credentials/status/3"
        );
        var credentialStatus = new CredentialStatus("https://example.com/credentials/status/3#94567", "StatusList2021Entry", props);

        var parsed = StatusListStatus.parse(credentialStatus);
        assertThat(parsed.getStatusListCredential()).isEqualTo("https://example.com/credentials/status/3");
        assertThat(parsed.getStatusListIndex()).isEqualTo(237);
        assertThat(parsed.getStatusListPurpose()).isEqualTo("revocation");
    }

    @Test
    void verifyMissingPurpose() {
        Map<String, Object> props = Map.of(
                //"statusPurpose", "revocation",
                "statusListIndex", "237",
                "statusListCredential", "https://example.com/credentials/status/3"
        );
        var credentialStatus = new CredentialStatus("https://example.com/credentials/status/3#94567", "StatusList2021Entry", props);
        assertThatThrownBy(() -> StatusListStatus.parse(credentialStatus)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("statusPurpose");
    }

    @Test
    void verifyMissingIndex() {
        Map<String, Object> props = Map.of(
                "statusPurpose", "revocation",
                //"statusListIndex", "237",
                "statusListCredential", "https://example.com/credentials/status/3"
        );
        var credentialStatus = new CredentialStatus("https://example.com/credentials/status/3#94567", "StatusList2021Entry", props);
        assertThatThrownBy(() -> StatusListStatus.parse(credentialStatus)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("statusListIndex");
    }

    @Test
    void verifyMissingCredential() {
        Map<String, Object> props = Map.of(
                "statusPurpose", "revocation",
                "statusListIndex", "237"
                //"statusListCredential", "https://example.com/credentials/status/3"
        );
        var credentialStatus = new CredentialStatus("https://example.com/credentials/status/3#94567", "StatusList2021Entry", props);
        assertThatThrownBy(() -> StatusListStatus.parse(credentialStatus)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("statusListCredential");
    }
}