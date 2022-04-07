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

import org.eclipse.dataspaceconnector.api.datamanagement.contractagreement.model.ContractAgreementDto;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformer;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ContractAgreementToContractAgreementDtoTransformer implements DtoTransformer<ContractAgreement, ContractAgreementDto> {

    @Override
    public Class<ContractAgreement> getInputType() {
        return ContractAgreement.class;
    }

    @Override
    public Class<ContractAgreementDto> getOutputType() {
        return ContractAgreementDto.class;
    }

    @Override
    public @Nullable ContractAgreementDto transform(@Nullable ContractAgreement object, @NotNull TransformerContext context) {
        return ContractAgreementDto.Builder.newInstance()
                .id(object.getId())
                .assetId(object.getAssetId())
                .policyId(object.getPolicy().getUid())
                .consumerAgentId(object.getConsumerAgentId())
                .providerAgentId(object.getProviderAgentId())
                .contractStartDate(object.getContractStartDate())
                .contractEndDate(object.getContractEndDate())
                .contractSigningDate(object.getContractSigningDate())
                .build();
    }
}
