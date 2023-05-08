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
import jakarta.json.JsonString;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractAgreementDto.Builder;
import static org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractAgreementDto.CONTRACT_AGREEMENT_ASSETID;
import static org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractAgreementDto.CONTRACT_AGREEMENT_CONSUMER_ID;
import static org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractAgreementDto.CONTRACT_AGREEMENT_POLICY;
import static org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractAgreementDto.CONTRACT_AGREEMENT_PROVIDER_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsonObjectFromContractAgreementDtoTransformerTest {

    private JsonObjectFromContractAgreementDtoTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectFromContractAgreementDtoTransformer(Json.createBuilderFactory(Map.of()));
    }

    @Test
    void transform() {
        var agreement = Builder.newInstance()
                .id("test-id")
                .providerId("test-provider")
                .consumerId("test-consumer")
                .assetId("test-asset")
                .policy(Policy.Builder.newInstance().build())
                .build();
        var context = mock(TransformerContext.class);
        when(context.transform(any(Policy.class), eq(JsonObject.class))).thenReturn(Json.createObjectBuilder().build());

        var jobj = transformer.transform(agreement, context);
        assertThat(jobj).isNotNull();
        assertThat(jobj.getJsonString(CONTRACT_AGREEMENT_ASSETID)).extracting(JsonString::getString).isEqualTo("test-asset");
        assertThat(jobj.getJsonString(CONTRACT_AGREEMENT_PROVIDER_ID)).extracting(JsonString::getString).isEqualTo("test-provider");
        assertThat(jobj.getJsonString(CONTRACT_AGREEMENT_CONSUMER_ID)).extracting(JsonString::getString).isEqualTo("test-consumer");
        assertThat(jobj.getJsonObject(CONTRACT_AGREEMENT_POLICY)).isNotNull();
    }
}