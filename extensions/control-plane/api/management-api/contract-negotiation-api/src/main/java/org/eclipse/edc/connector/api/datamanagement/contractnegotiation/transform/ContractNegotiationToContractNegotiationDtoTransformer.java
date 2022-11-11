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

package org.eclipse.edc.connector.api.datamanagement.contractnegotiation.transform;

import org.eclipse.edc.api.transformer.DtoTransformer;
import org.eclipse.edc.connector.api.datamanagement.contractnegotiation.model.ContractNegotiationDto;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.util.Optional.ofNullable;

public class ContractNegotiationToContractNegotiationDtoTransformer implements DtoTransformer<ContractNegotiation, ContractNegotiationDto> {
    @Override
    public Class<ContractNegotiation> getInputType() {
        return ContractNegotiation.class;
    }

    @Override
    public Class<ContractNegotiationDto> getOutputType() {
        return ContractNegotiationDto.class;
    }

    @Override
    public @Nullable ContractNegotiationDto transform(@Nullable ContractNegotiation object, @NotNull TransformerContext context) {
        return ContractNegotiationDto.Builder.newInstance()
                .id(object.getId())
                .type(object.getType())
                .contractAgreementId(ofNullable(object.getContractAgreement()).map(ContractAgreement::getId).orElse(null))
                .state(ContractNegotiationStates.from(object.getState()).name())
                .protocol(object.getProtocol())
                .counterPartyAddress(object.getCounterPartyAddress())
                .errorDetail(object.getErrorDetail())
                .createdAt(object.getCreatedAt())
                .updatedAt(object.getUpdatedAt())
                .build();
    }
}
