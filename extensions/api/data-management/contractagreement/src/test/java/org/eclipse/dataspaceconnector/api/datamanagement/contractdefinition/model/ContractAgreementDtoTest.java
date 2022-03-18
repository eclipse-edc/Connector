/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContractAgreementDtoTest {

    @Test
    void verifySerialization() throws JsonProcessingException {
        var om = new ObjectMapper();
        var dto = ContractAgreementDto.Builder.newInstance()
                .assetId("test-asset-id")
                .id("test-id")
                .contractEndDate(1234L)
                .contractStartDate(1234L)
                .contractSigningDate(5432L)
                .providerAgentId("provider")
                .consumerAgentId("consumer")
                .policyId("policy-id")
                .build();

        var json = om.writeValueAsString(dto);
        assertThat(json).isNotNull();

        var deserialized = om.readValue(json, ContractAgreementDto.class);
        assertThat(deserialized).usingRecursiveComparison().isEqualTo(dto);

    }

}