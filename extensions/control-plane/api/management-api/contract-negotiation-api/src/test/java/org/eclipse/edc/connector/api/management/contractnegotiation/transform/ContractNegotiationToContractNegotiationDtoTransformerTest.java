/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation.Type.PROVIDER;
import static org.mockito.Mockito.mock;

class ContractNegotiationToContractNegotiationDtoTransformerTest {

    private final ContractNegotiationToContractNegotiationDtoTransformer transformer = new ContractNegotiationToContractNegotiationDtoTransformer();

    @Test
    void inputOutputType() {
        assertThat(transformer.getInputType()).isNotNull();
        assertThat(transformer.getOutputType()).isNotNull();
    }

    @Test
    void transform() {
        var context = mock(TransformerContext.class);
        var callback = CallbackAddress.Builder.newInstance()
                .uri("local://test")
                .build();
        var contractNegotiation = ContractNegotiation.Builder.newInstance()
                .id("negotiationId")
                .type(PROVIDER)
                .state(ContractNegotiationStates.INITIAL.code())
                .protocol("protocol")
                .counterPartyId("counterPartyId")
                .counterPartyAddress("counterPartyAddress")
                .contractAgreement(createContractAgreement("agreementId"))
                .errorDetail("errorDetail")
                .correlationId("correlationId")
                .callbackAddresses(List.of(callback))
                .build();

        var dto = transformer.transform(contractNegotiation, context);

        assertThat(dto.getId()).isEqualTo("negotiationId");
        assertThat(dto.getType()).isEqualTo(PROVIDER);
        assertThat(dto.getContractAgreementId()).isEqualTo("agreementId");
        assertThat(dto.getState()).isEqualTo("INITIAL");
        assertThat(dto.getProtocol()).isEqualTo("protocol");
        assertThat(dto.getCounterPartyAddress()).isEqualTo("counterPartyAddress");
        assertThat(dto.getErrorDetail()).isEqualTo("errorDetail");
        assertThat(dto.getUpdatedAt()).isEqualTo(contractNegotiation.getUpdatedAt());
        assertThat(dto.getCreatedAt()).isEqualTo(contractNegotiation.getCreatedAt());
        assertThat(dto.getCallbackAddresses()).usingRecursiveFieldByFieldElementComparator().contains(callback);
    }

    private ContractAgreement createContractAgreement(String id) {
        return ContractAgreement.Builder.newInstance()
                .id(id)
                .consumerAgentId("any")
                .providerAgentId("any")
                .assetId(UUID.randomUUID().toString())
                .policy(Policy.Builder.newInstance().build())
                .build();
    }

}
