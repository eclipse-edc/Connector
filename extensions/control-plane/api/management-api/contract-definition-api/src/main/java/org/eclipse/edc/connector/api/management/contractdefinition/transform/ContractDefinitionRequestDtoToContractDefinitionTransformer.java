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

package org.eclipse.edc.connector.api.management.contractdefinition.transform;

import org.eclipse.edc.api.transformer.DtoTransformer;
import org.eclipse.edc.connector.api.management.contractdefinition.model.ContractDefinitionCreateDto;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.spi.asset.AssetSelectorExpression;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Collectors;

public class ContractDefinitionRequestDtoToContractDefinitionTransformer implements DtoTransformer<ContractDefinitionCreateDto, ContractDefinition> {

    @Override
    public Class<ContractDefinitionCreateDto> getInputType() {
        return ContractDefinitionCreateDto.class;
    }

    @Override
    public Class<ContractDefinition> getOutputType() {
        return ContractDefinition.class;
    }

    @Override
    public @Nullable ContractDefinition transform(@NotNull ContractDefinitionCreateDto object, @NotNull TransformerContext context) {
        var criteria = object.getCriteria().stream().map(it -> context.transform(it, Criterion.class)).collect(Collectors.toList());
        var selectorExpression = AssetSelectorExpression.Builder.newInstance().criteria(criteria).build();
        return ContractDefinition.Builder.newInstance()
                .id(object.getId())
                .accessPolicyId(object.getAccessPolicyId())
                .contractPolicyId(object.getContractPolicyId())
                .selectorExpression(selectorExpression)
                .validity(object.getValidity())
                .build();
    }
}
