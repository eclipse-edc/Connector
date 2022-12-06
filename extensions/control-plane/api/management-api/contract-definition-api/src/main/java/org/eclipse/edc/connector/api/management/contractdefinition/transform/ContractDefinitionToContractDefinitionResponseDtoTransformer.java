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

import org.eclipse.edc.api.model.CriterionDto;
import org.eclipse.edc.api.transformer.DtoTransformer;
import org.eclipse.edc.connector.api.management.contractdefinition.model.ContractDefinitionResponseDto;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import static java.util.stream.Collectors.toList;

public class ContractDefinitionToContractDefinitionResponseDtoTransformer implements DtoTransformer<ContractDefinition, ContractDefinitionResponseDto> {

    @Override
    public Class<ContractDefinition> getInputType() {
        return ContractDefinition.class;
    }

    @Override
    public Class<ContractDefinitionResponseDto> getOutputType() {
        return ContractDefinitionResponseDto.class;
    }

    @Override
    public @Nullable ContractDefinitionResponseDto transform(@Nullable ContractDefinition object, @NotNull TransformerContext context) {
        return Optional.ofNullable(object)
                .map(input -> {
                    var criteria = object.getSelectorExpression().getCriteria().stream().map(it -> context.transform(it, CriterionDto.class)).collect(toList());
                    return ContractDefinitionResponseDto.Builder.newInstance()
                            .id(object.getId())
                            .accessPolicyId(object.getAccessPolicyId())
                            .createdAt(object.getCreatedAt())
                            .contractPolicyId(object.getContractPolicyId())
                            .criteria(criteria)
                            .validity(input.getValidity())
                            .build();
                })
                .orElseGet(() -> {
                    context.reportProblem("input contract definition is null");
                    return null;
                });
    }
}
