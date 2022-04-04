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

import org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.model.ContractDefinitionDto;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformer;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ContractDefinitionDtoToContractDefinitionTransformer implements DtoTransformer<ContractDefinitionDto, ContractDefinition> {

    @Override
    public Class<ContractDefinitionDto> getInputType() {
        return ContractDefinitionDto.class;
    }

    @Override
    public Class<ContractDefinition> getOutputType() {
        return ContractDefinition.class;
    }

    @Override
    public @Nullable ContractDefinition transform(@Nullable ContractDefinitionDto object, @NotNull TransformerContext context) {
        return ContractDefinition.Builder.newInstance()
                .id(object.getId())
                .accessPolicy(Policy.Builder.newInstance().id(object.getAccessPolicyId()).build()) // TODO: policy will be replaced by policy id
                .contractPolicy(Policy.Builder.newInstance().id(object.getContractPolicyId()).build()) // TODO: policy will be replaced by policy id
                .selectorExpression(AssetSelectorExpression.Builder.newInstance().criteria(object.getCriteria()).build())
                .build();
    }
}
