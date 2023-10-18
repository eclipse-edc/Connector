/*
 *  Copyright (c) 2023 ZF Friedrichshafen AG
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       ZF Friedrichshafen AG - Negotiation API enhancement
 *
 */

package org.eclipse.edc.connector.api.management.contractnegotiation.v3.transform;

import org.eclipse.edc.connector.api.management.contractnegotiation.v3.model.ContractRequestDto;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequest;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ContractRequestDtoToContractrequestTransformerTest {

    private final TransformerContext context = mock(TransformerContext.class);

    private final ContractRequestDtoToContractRequestTransformer transformer = new ContractRequestDtoToContractRequestTransformer();

    @Test
    void transform_success() {
        var policy = Policy.Builder.newInstance().build();
        var contractRequestDto = ContractRequestDto.Builder.newInstance()
                .protocol("protocol")
                .providerId("provider-id")
                .counterPartyAddress("test-cb")
                .assetId("asset-id")
                .policyId("policy-id")
                .policy(policy)
                .build();

        var contractRequestTransformed = transformer.transform(contractRequestDto, context);

        var contractOffer = ContractOffer.Builder.newInstance()
                .id("policy-id")
                .assetId("asset-id")
                .policy(policy)
                .build();
        var contractRequest = ContractRequest.Builder.newInstance()
                .protocol("protocol")
                .providerId("provider-id")
                .counterPartyAddress("test-cb")
                .counterPartyAddress("test-cb")
                .contractOffer(contractOffer)
                .build();

        assertThat(contractRequestTransformed).isNotNull();
        assertThat(contractRequestTransformed.getProtocol()).isEqualTo(contractRequest.getProtocol());
        assertThat(contractRequestTransformed.getProviderId()).isEqualTo(contractRequest.getProviderId());
        assertThat(contractRequestTransformed.getCounterPartyAddress()).isEqualTo(contractRequest.getCounterPartyAddress());
        assertThat(contractRequestTransformed.getContractOffer().getId()).isEqualTo(contractRequest.getContractOffer().getId());
        assertThat(contractRequestTransformed.getContractOffer().getAssetId()).isEqualTo(contractRequest.getContractOffer().getAssetId());
        assertThat(contractRequestTransformed.getContractOffer().getPolicy()).isEqualTo(contractRequest.getContractOffer().getPolicy());
    }

}
