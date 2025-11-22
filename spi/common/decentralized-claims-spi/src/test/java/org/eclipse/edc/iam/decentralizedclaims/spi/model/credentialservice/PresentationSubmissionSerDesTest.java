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

package org.eclipse.edc.iam.decentralizedclaims.spi.model.credentialservice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.credentialservice.InputDescriptorMapping;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.credentialservice.PresentationSubmission;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PresentationSubmissionSerDesTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void verifyDeserialization() throws JsonProcessingException {
        var json = """
                 {
                    "id": "a30e3b91-fb77-4d22-95fa-871689c322e2",
                    "definition_id": "32f54163-7166-48f1-93d8-ff217bdb0653",
                    "descriptor_map": [
                      {
                        "id": "banking_input_2",
                        "format": "jwt_vc",
                        "path": "$.verifiableCredential[0]"
                      },
                      {
                        "id": "employment_input",
                        "format": "ldp_vc",
                        "path": "$.verifiableCredential[1]"
                      },
                      {
                        "id": "citizenship_input_1",
                        "format": "ldp_vc",
                        "path": "$.verifiableCredential[2]"
                      }
                    ]
                  }
                """;
        var pd = mapper.readValue(json, org.eclipse.edc.iam.verifiablecredentials.spi.model.credentialservice.PresentationSubmission.class);
        assertThat(pd).isNotNull();

        assertThat(pd.id()).isEqualTo("a30e3b91-fb77-4d22-95fa-871689c322e2");
        assertThat(pd.definitionId()).isEqualTo("32f54163-7166-48f1-93d8-ff217bdb0653");
        assertThat(pd.descriptorMap()).hasSize(3);
    }

    @Test
    void verifySerialization() throws JsonProcessingException {
        var pd = new org.eclipse.edc.iam.verifiablecredentials.spi.model.credentialservice.PresentationSubmission("test-id", "test-def-id", List.of(new InputDescriptorMapping("test-input", "ldp_vc", "$.verifiableCredentials[0]")));
        var json = mapper.writeValueAsString(pd);

        var deser = mapper.readValue(json, PresentationSubmission.class);
        assertThat(deser).usingRecursiveComparison().isEqualTo(pd);
    }
}