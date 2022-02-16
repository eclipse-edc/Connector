/*
 * Copyright (c) 2022 ZF Friedrichshafen AG
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *   ZF Friedrichshafen AG - Initial API and Implementation
 */

package org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation.Type;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContractNegotiationDtoTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void verifySerialization() throws JsonProcessingException {
        var dto = ContractNegotiationDto.Builder.newInstance()
                .contractAgreementId("test-contract-agreement-id")
                .counterPartyAddress("test-counter-party-address")
                .errorDetail("test-error-detail")
                .protocol("test-protocol")
                .state("test-state")
                .id("test-id")
                .build();

        var str = objectMapper.writeValueAsString(dto);

        assertThat(str).isNotNull();

        var deserialized = objectMapper.readValue(str, ContractNegotiationDto.class);
        assertThat(deserialized).usingRecursiveComparison().isEqualTo(dto);
    }

    @Test
    void deserialize_wrongType() throws JsonProcessingException {
        var dto = ContractNegotiationDto.Builder.newInstance()
                .contractAgreementId("test-contract-agreement-id")
                .counterPartyAddress("test-counter-party-address")
                .errorDetail("test-error-detail")
                .protocol("test-protocol")
                .state("test-state")
                .id("test-id")
                .build();

        var str = objectMapper.writeValueAsString(dto)
                .replace(Type.CONSUMER.toString(), Type.PROVIDER.toString());

        assertThat(str).isNotNull();
        assertThrows(JsonMappingException.class,
                () -> objectMapper.readValue(str, ContractNegotiationDto.class));
    }
}
