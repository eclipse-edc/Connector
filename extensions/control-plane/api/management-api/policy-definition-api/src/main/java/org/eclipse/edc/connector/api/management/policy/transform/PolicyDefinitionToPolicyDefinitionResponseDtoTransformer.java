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

package org.eclipse.edc.connector.api.management.policy.transform;

import org.eclipse.edc.api.transformer.DtoTransformer;
import org.eclipse.edc.connector.api.management.policy.model.PolicyDefinitionResponseDto;
import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PolicyDefinitionToPolicyDefinitionResponseDtoTransformer implements DtoTransformer<PolicyDefinition, PolicyDefinitionResponseDto> {

    @Override
    public Class<PolicyDefinition> getInputType() {
        return PolicyDefinition.class;
    }

    @Override
    public Class<PolicyDefinitionResponseDto> getOutputType() {
        return PolicyDefinitionResponseDto.class;
    }

    @Override
    public @Nullable PolicyDefinitionResponseDto transform(@NotNull PolicyDefinition object, @NotNull TransformerContext context) {
        return PolicyDefinitionResponseDto.Builder.newInstance()
                .id(object.getId())
                .policy(object.getPolicy())
                .createdAt(object.getCreatedAt())
                .build();
    }
}
