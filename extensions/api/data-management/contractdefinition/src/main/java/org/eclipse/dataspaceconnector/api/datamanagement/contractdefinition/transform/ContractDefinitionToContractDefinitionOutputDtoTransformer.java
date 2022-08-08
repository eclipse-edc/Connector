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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.transform;

import org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.model.ContractDefinitionOutputDto;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformer;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class ContractDefinitionToContractDefinitionOutputDtoTransformer implements DtoTransformer<ContractDefinition, ContractDefinitionOutputDto> {

    @Override
    public Class<ContractDefinition> getInputType() {
        return ContractDefinition.class;
    }

    @Override
    public Class<ContractDefinitionOutputDto> getOutputType() {
        return ContractDefinitionOutputDto.class;
    }

    @Override
    public @Nullable ContractDefinitionOutputDto transform(@Nullable ContractDefinition object, @NotNull TransformerContext context) {
        return Optional.ofNullable(object)
                .map(input -> ContractDefinitionOutputDto.Builder.newInstance()
                        .id(object.getId())
                        .accessPolicyId(object.getAccessPolicyId())
                        .createdAt(object.getCreatedAt())
                        .contractPolicyId(object.getContractPolicyId())
                        .criteria(object.getSelectorExpression().getCriteria())
                        .build()
                )
                .orElseGet(() -> {
                    context.reportProblem("input contract definition is null");
                    return null;
                });
    }
}
