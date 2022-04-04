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

package org.eclipse.dataspaceconnector.api.datamanagement.contractagreement.transform;

import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ContractAgreementToContractAgreementDtoTransformerTest {

    private final ContractAgreementToContractAgreementDtoTransformer transformer = new ContractAgreementToContractAgreementDtoTransformer();

    @Test
    void inputOutputType() {
        assertThat(transformer.getInputType()).isNotNull();
        assertThat(transformer.getOutputType()).isNotNull();
    }

    @Test
    void transform() {
        var context = mock(TransformerContext.class);
        var contractAgreement = ContractAgreement.Builder.newInstance()
                .id("agreementId")
                .consumerAgentId("consumerAgentId")
                .providerAgentId("providerAgentId")
                .assetId("assetId")
                .policy(Policy.Builder.newInstance().id("policyId").build())
                .contractStartDate(1)
                .contractSigningDate(2)
                .contractEndDate(3)
                .build();

        var dto = transformer.transform(contractAgreement, context);

        assertThat(dto.getId()).isEqualTo("agreementId");
        assertThat(dto.getAssetId()).isEqualTo("assetId");
        assertThat(dto.getPolicyId()).isEqualTo("policyId");
        assertThat(dto.getContractStartDate()).isEqualTo(1);
        assertThat(dto.getContractSigningDate()).isEqualTo(2);
        assertThat(dto.getContractEndDate()).isEqualTo(3);
        assertThat(dto.getConsumerAgentId()).isEqualTo("consumerAgentId");
        assertThat(dto.getProviderAgentId()).isEqualTo("providerAgentId");
    }

}