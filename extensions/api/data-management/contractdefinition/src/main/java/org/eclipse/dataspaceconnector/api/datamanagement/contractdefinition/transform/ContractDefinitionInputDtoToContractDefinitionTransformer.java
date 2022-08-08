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

import org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.model.ContractDefinitionInputDto;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformer;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class ContractDefinitionInputDtoToContractDefinitionTransformer implements DtoTransformer<ContractDefinitionInputDto, ContractDefinition> {

    @Override
    public Class<ContractDefinitionInputDto> getInputType() {
        return ContractDefinitionInputDto.class;
    }

    @Override
    public Class<ContractDefinition> getOutputType() {
        return ContractDefinition.class;
    }

    @Override
    public @Nullable ContractDefinition transform(@Nullable ContractDefinitionInputDto object, @NotNull TransformerContext context) {
        return Optional.ofNullable(object)
                .map(input -> ContractDefinition.Builder.newInstance()
                        .id(input.getId())
                        .accessPolicyId(input.getAccessPolicyId())
                        .contractPolicyId(input.getContractPolicyId())
                        .selectorExpression(AssetSelectorExpression.Builder.newInstance().criteria(input.getCriteria()).build())
                        .build()
                )
                .orElseGet(() -> {
                    context.reportProblem("input contract definition is null");
                    return null;
                });
    }
}
