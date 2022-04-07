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

package org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation.transform;

import org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation.model.NegotiationInitiateRequestDto;
import org.eclipse.dataspaceconnector.policy.model.Identifiable;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.api.datamanagement.contractnegotiation.TestFunctions.createOffer;
import static org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractOfferRequest.Type.INITIAL;
import static org.mockito.Mockito.mock;

class NegotiationInitiateRequestDtoToDataRequestTransformerTest {

    private final TransformerContext context = mock(TransformerContext.class);
    private final NegotiationInitiateRequestDtoToDataRequestTransformer transformer = new NegotiationInitiateRequestDtoToDataRequestTransformer();

    @Test
    void inputOutputType() {
        assertThat(transformer.getInputType()).isNotNull();
        assertThat(transformer.getOutputType()).isNotNull();
    }

    @Test
    void transform_policyId() {
        var dto = NegotiationInitiateRequestDto.Builder.newInstance()
                .connectorId("connectorId")
                .connectorAddress("address")
                .protocol("protocol")
                .offerId(createOffer("offerId", "assetId", "policyId"))
                .build();

        var request = transformer.transform(dto, context);

        assertThat(request.getConnectorId()).isEqualTo("connectorId");
        assertThat(request.getConnectorAddress()).isEqualTo("address");
        assertThat(request.getProtocol()).isEqualTo("protocol");
        assertThat(request.getType()).isEqualTo(INITIAL);
        assertThat(request.getContractOffer().getId()).isEqualTo("offerId");
        assertThat(request.getContractOffer().getPolicyId()).isEqualTo("policyId");
        assertThat(request.getContractOffer().getPolicy()).isNull();
    }

    @Test
    void transform_customPolicy() {
        var policy = Policy.Builder.newInstance().id(UUID.randomUUID().toString()).build();
        var dto = NegotiationInitiateRequestDto.Builder.newInstance()
                .connectorId("connectorId")
                .connectorAddress("address")
                .protocol("protocol")
                .offerId(createOffer(policy))
                .build();

        var request = transformer.transform(dto, context);

        assertThat(request.getConnectorId()).isEqualTo("connectorId");
        assertThat(request.getConnectorAddress()).isEqualTo("address");
        assertThat(request.getProtocol()).isEqualTo("protocol");
        assertThat(request.getType()).isEqualTo(INITIAL);
        assertThat(request.getContractOffer().getPolicyId()).isNull();
        assertThat(request.getContractOffer().getPolicy()).extracting(Identifiable::getUid).isEqualTo(policy.getUid());
    }
}