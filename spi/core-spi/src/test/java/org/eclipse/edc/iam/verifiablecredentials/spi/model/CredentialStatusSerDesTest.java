/*
 *  Copyright (c) 2025 Cofinity-X
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

package org.eclipse.edc.iam.verifiablecredentials.spi.model;

import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CredentialStatusSerDesTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void verifySerialization() throws JacksonException {
        var json = """
                  {
                    "id": "https://example.com/credentials/status/3#94567",
                    "type": "BitstringStatusListEntry",
                    "statusPurpose": "revocation",
                    "statusListIndex": "94567",
                    "statusListCredential": "https://example.com/credentials/status/3"
                }
                """;

        var status = objectMapper.readValue(json, CredentialStatus.class);

        assertThat(status).isNotNull();
        assertThat(status.additionalProperties()).hasSize(3)
                .containsEntry("statusPurpose", "revocation")
                .containsEntry("statusListIndex", "94567")
                .containsEntry("statusListCredential", "https://example.com/credentials/status/3");
    }

    @Test
    void verifyDeserialization() throws JacksonException {

        var status = new CredentialStatus("test-id", "BitStringStatusListEntry", Map.of(
                "statusPurpose", "revocation",
                "statusListIndex", "94567",
                "statusListCredential", "https://example.com/credentials/status/3"));

        var json = objectMapper.writeValueAsString(status);
        assertThat(json).isNotNull();
        assertThat(json)
                .matches(".*\"statusPurpose\":.*\"revocation\".*")
                .matches(".*\"statusListIndex\":.*\"94567\".*")
                .matches(".*\"statusListCredential\":.*\"https://example.com/credentials/status/3\".*");
    }

}