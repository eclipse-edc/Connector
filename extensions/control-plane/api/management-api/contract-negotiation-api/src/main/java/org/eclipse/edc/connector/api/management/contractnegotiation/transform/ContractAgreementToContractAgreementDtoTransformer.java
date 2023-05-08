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

import org.eclipse.edc.api.transformer.DtoTransformer;
import org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractAgreementDto;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.transform.spi.TransformerContext;
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
    public @Nullable ContractAgreementDto transform(@NotNull ContractAgreement object, @NotNull TransformerContext context) {
        return ContractAgreementDto.Builder.newInstance()
                .id(object.getId())
                .assetId(object.getAssetId())
                .policy(object.getPolicy())
                .consumerId(object.getConsumerId())
                .providerId(object.getProviderId())
                .contractStartDate(object.getContractStartDate())
                .contractEndDate(object.getContractEndDate())
                .contractSigningDate(object.getContractSigningDate())
                .build();
    }
}
