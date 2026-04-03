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

package org.eclipse.edc.connector.controlplane.transform.edc.contractagreement.from;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement.CONTRACT_AGREEMENT_ASSET_ID;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement.CONTRACT_AGREEMENT_CLAIMS;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement.CONTRACT_AGREEMENT_CONSUMER_ID;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement.CONTRACT_AGREEMENT_ID;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement.CONTRACT_AGREEMENT_POLICY;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement.CONTRACT_AGREEMENT_PROVIDER_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsonObjectFromContractAgreementTransformerTest {

    private JsonObjectFromContractAgreementTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectFromContractAgreementTransformer(Json.createBuilderFactory(Map.of()), JacksonJsonLd::createObjectMapper);
    }

    @Test
    void transform() {
        var agreement = ContractAgreement.Builder.newInstance()
                .id("test-id")
                .providerId("test-provider")
                .consumerId("test-consumer")
                .assetId("test-asset")
                .agreementId("agreement-id")
                .policy(Policy.Builder.newInstance().build())
                .claims(Map.of("key", "value"))
                .build();
        var context = mock(TransformerContext.class);
        when(context.transform(any(Policy.class), eq(JsonObject.class))).thenReturn(Json.createObjectBuilder().build());
        when(context.transform(anyString(), eq(JsonValue.class))).thenAnswer(i -> Json.createValue(i.getArgument(0, String.class)));

        var result = transformer.transform(agreement, context);

        assertThat(result).isNotNull();
        assertThat(result.getJsonString(CONTRACT_AGREEMENT_ASSET_ID)).extracting(JsonString::getString).isEqualTo("test-asset");
        assertThat(result.getJsonString(CONTRACT_AGREEMENT_PROVIDER_ID)).extracting(JsonString::getString).isEqualTo("test-provider");
        assertThat(result.getJsonString(CONTRACT_AGREEMENT_CONSUMER_ID)).extracting(JsonString::getString).isEqualTo("test-consumer");
        assertThat(result.getJsonString(CONTRACT_AGREEMENT_ID)).extracting(JsonString::getString).isEqualTo("agreement-id");
        assertThat(result.getJsonObject(CONTRACT_AGREEMENT_POLICY)).isNotNull();
        assertThat(result.getJsonObject(CONTRACT_AGREEMENT_CLAIMS)).isNotNull().hasSize(1).contains(entry("key", Json.createValue("value")));
    }
}
