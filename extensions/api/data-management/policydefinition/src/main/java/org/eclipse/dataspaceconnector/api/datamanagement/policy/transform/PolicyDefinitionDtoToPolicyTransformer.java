/*
 * Copyright (c) 2022 ZF Friedrichshafen AG
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *    ZF Friedrichshafen AG - Initial API and Implementation
 */

package org.eclipse.dataspaceconnector.api.datamanagement.policy.transform;

import org.eclipse.dataspaceconnector.api.datamanagement.policy.model.PolicyDefinitionDto;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformer;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PolicyDefinitionDtoToPolicyTransformer implements DtoTransformer<PolicyDefinitionDto, Policy> {

    @Override
    public Class<PolicyDefinitionDto> getInputType() {
        return PolicyDefinitionDto.class;
    }

    @Override
    public Class<Policy> getOutputType() {
        return Policy.class;
    }

    @Override
    public @Nullable Policy transform(@Nullable PolicyDefinitionDto object, @NotNull TransformerContext context) {
        assert object != null;
        return Policy.Builder.newInstance().permissions(object.getPermissions())
                .prohibitions(object.getProhibitions())
                .duties(object.getObligations())
                .extensibleProperties(object.getExtensibleProperties())
                .inheritsFrom(object.getInheritsFrom())
                .assigner(object.getAssigner())
                .assignee(object.getAssignee())
                .target(object.getTarget())
                .type(object.getType())
                .build();
    }
}
