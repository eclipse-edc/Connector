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

package org.eclipse.dataspaceconnector.api.datamanagement.policy.transform;

import org.eclipse.dataspaceconnector.api.datamanagement.policy.model.PolicyDefinitionRequestDto;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformer;
import org.eclipse.dataspaceconnector.spi.policy.PolicyDefinition;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class PolicyDefinitionRequestDtoToPolicyDefinitionTransformer implements DtoTransformer<PolicyDefinitionRequestDto, PolicyDefinition> {

    @Override
    public Class<PolicyDefinitionRequestDto> getInputType() {
        return PolicyDefinitionRequestDto.class;
    }

    @Override
    public Class<PolicyDefinition> getOutputType() {
        return PolicyDefinition.class;
    }

    @Override
    public @Nullable PolicyDefinition transform(@Nullable PolicyDefinitionRequestDto object, @NotNull TransformerContext context) {
        return Optional.ofNullable(object)
                .map(input -> PolicyDefinition.Builder.newInstance()
                        .id(input.getId())
                        .policy(input.getPolicy())
                        .build()
                )
                .orElseGet(() -> {
                    context.reportProblem("input policy definition is null");
                    return null;
                });
    }
}
