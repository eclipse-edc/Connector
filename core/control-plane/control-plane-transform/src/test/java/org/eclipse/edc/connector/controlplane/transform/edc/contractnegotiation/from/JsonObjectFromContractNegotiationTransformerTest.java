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

package org.eclipse.edc.connector.controlplane.transform.edc.contractnegotiation.from;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation.CONTRACT_NEGOTIATION_AGREEMENT_ID;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation.CONTRACT_NEGOTIATION_ASSET_ID;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation.CONTRACT_NEGOTIATION_CORRELATION_ID;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation.CONTRACT_NEGOTIATION_COUNTERPARTY_ADDR;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation.CONTRACT_NEGOTIATION_COUNTERPARTY_ID;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation.CONTRACT_NEGOTIATION_CREATED_AT;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation.CONTRACT_NEGOTIATION_NEG_TYPE;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation.CONTRACT_NEGOTIATION_PROTOCOL;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation.CONTRACT_NEGOTIATION_STATE;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates.REQUESTED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsonObjectFromContractNegotiationTransformerTest {

    private final TransformerContext context = mock();
    private final JsonObjectFromContractNegotiationTransformer transformer = new JsonObjectFromContractNegotiationTransformer(Json.createBuilderFactory(Map.of()));

    @Test
    void transform() {
        when(context.transform(any(CallbackAddress.class), eq(JsonObject.class))).thenReturn(Json.createObjectBuilder().build());
        var co = createContractOffer("asset-id");
        var cn = ContractNegotiation.Builder.newInstance()
                .id("test-id")
                .correlationId("correlation-id")
                .counterPartyId("counter-party-id")
                .counterPartyAddress("address")
                .contractAgreement(createContractAgreement("test-agreement"))
                .state(REQUESTED.code())
                .type(ContractNegotiation.Type.PROVIDER)
                .contractOffers(List.of(co))
                .callbackAddresses(List.of(
                        CallbackAddress.Builder.newInstance()
                                .uri("local://test")
                                .build()))
                .protocol("protocol")
                .createdAt(1234)
                .build();

        var jsonObject = transformer.transform(cn, context);

        assertThat(jsonObject).isNotNull();
        assertThat(jsonObject.getString(ID)).isEqualTo("test-id");
        assertThat(jsonObject.getString(CONTRACT_NEGOTIATION_STATE)).isEqualTo(REQUESTED.name());
        assertThat(jsonObject.getString(CONTRACT_NEGOTIATION_COUNTERPARTY_ID)).isEqualTo("counter-party-id");
        assertThat(jsonObject.getString(CONTRACT_NEGOTIATION_COUNTERPARTY_ADDR)).isEqualTo("address");
        assertThat(jsonObject.getString(CONTRACT_NEGOTIATION_AGREEMENT_ID)).isEqualTo("test-agreement");
        assertThat(jsonObject.getString(CONTRACT_NEGOTIATION_NEG_TYPE)).isEqualTo("PROVIDER");
        assertThat(jsonObject.getString(CONTRACT_NEGOTIATION_PROTOCOL)).isEqualTo("protocol");
        assertThat(jsonObject.getString(CONTRACT_NEGOTIATION_CORRELATION_ID)).isEqualTo(cn.getCorrelationId());
        assertThat(jsonObject.getString(CONTRACT_NEGOTIATION_ASSET_ID)).isEqualTo(co.getAssetId());
        assertThat(jsonObject.getJsonNumber(CONTRACT_NEGOTIATION_CREATED_AT).longValue()).isEqualTo(1234);
    }

    private ContractOffer createContractOffer(String assetId) {
        return ContractOffer.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .assetId(assetId)
                .policy(Policy.Builder.newInstance().build())
                .build();
    }

    private ContractAgreement createContractAgreement(String id) {
        return ContractAgreement.Builder.newInstance()
                .id(id)
                .providerId("providerId")
                .consumerId("consumerId")
                .assetId("assetId")
                .policy(Policy.Builder.newInstance().build())
                .build();
    }
}
