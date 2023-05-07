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

package org.eclipse.edc.connector.api.management.contractnegotiation.transform;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractNegotiationDto.Builder;
import static org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractNegotiationDto.CONTRACT_NEGOTIATION_AGREEMENT_ID;
import static org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractNegotiationDto.CONTRACT_NEGOTIATION_COUNTERPARTY_ADDR;
import static org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractNegotiationDto.CONTRACT_NEGOTIATION_NEG_TYPE;
import static org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractNegotiationDto.CONTRACT_NEGOTIATION_PROTOCOL;
import static org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractNegotiationDto.CONTRACT_NEGOTIATION_STATE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsonObjectFromContractNegotiationDtoTransformerTest {

    private JsonObjectFromContractNegotiationDtoTransformer transformer;
    private TransformerContext context;

    @BeforeEach
    void setUp() {
        context = mock(TransformerContext.class);
        when(context.transform(any(CallbackAddress.class), eq(JsonObject.class))).thenReturn(Json.createObjectBuilder().build());
        transformer = new JsonObjectFromContractNegotiationDtoTransformer(Json.createBuilderFactory(Map.of()));
    }

    @Test
    void transform() {
        var cn = Builder.newInstance()
                .id("test-id")
                .counterPartyAddress("address")
                .contractAgreementId("test-agreement")
                .state("test-state")
                .callbackAddresses(List.of(
                        CallbackAddress.Builder.newInstance()
                                .uri("local://test")
                                .build()))
                .protocol("protocol").build();

        var jsonObject = transformer.transform(cn, context);
        assertThat(jsonObject).isNotNull();
        assertThat(jsonObject.getJsonString(CONTRACT_NEGOTIATION_STATE).getString()).isEqualTo("test-state");
        assertThat(jsonObject.getJsonString(CONTRACT_NEGOTIATION_COUNTERPARTY_ADDR).getString()).isEqualTo("address");
        assertThat(jsonObject.getJsonString(CONTRACT_NEGOTIATION_AGREEMENT_ID).getString()).isEqualTo("test-agreement");
        assertThat(jsonObject.getJsonString(ID).getString()).isEqualTo("test-id");
        assertThat(jsonObject.getJsonString(CONTRACT_NEGOTIATION_NEG_TYPE).getString()).isEqualTo("CONSUMER");
        assertThat(jsonObject.getJsonString(CONTRACT_NEGOTIATION_PROTOCOL).getString()).isEqualTo("protocol");
    }
}