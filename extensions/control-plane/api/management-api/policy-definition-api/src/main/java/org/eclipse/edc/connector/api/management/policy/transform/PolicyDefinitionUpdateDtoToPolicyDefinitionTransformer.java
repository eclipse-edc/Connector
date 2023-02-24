/*
 *  Copyright (c) 2022 T-Systems International GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       T-Systems International GmbH
 *
 */

package org.eclipse.edc.connector.api.management.policy.transform;

import org.eclipse.edc.api.transformer.DtoTransformer;
import org.eclipse.edc.connector.api.management.policy.model.PolicyDefinitionUpdateDto;
import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PolicyDefinitionUpdateDtoToPolicyDefinitionTransformer implements DtoTransformer<PolicyDefinitionUpdateDto, PolicyDefinition> {

    @Override
    public Class<PolicyDefinitionUpdateDto> getInputType() {
        return PolicyDefinitionUpdateDto.class;
    }

    @Override
    public Class<PolicyDefinition> getOutputType() {
        return PolicyDefinition.class;
    }

    @Override
    public @Nullable PolicyDefinition transform(@NotNull PolicyDefinitionUpdateDto object, @NotNull TransformerContext context) {
        return PolicyDefinition.Builder.newInstance()
                .policy(object.getPolicy())
                .build();
    }
}
