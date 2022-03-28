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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 */

package org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.transform;

import org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.model.ContractDefinitionDto;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformer;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ContractDefinitionToContractDefinitionDtoTransformer implements DtoTransformer<ContractDefinition, ContractDefinitionDto> {

    @Override
    public Class<ContractDefinition> getInputType() {
        return ContractDefinition.class;
    }

    @Override
    public Class<ContractDefinitionDto> getOutputType() {
        return ContractDefinitionDto.class;
    }

    @Override
    public @Nullable ContractDefinitionDto transform(@Nullable ContractDefinition object, @NotNull TransformerContext context) {
        return ContractDefinitionDto.Builder.newInstance()
                .id(object.getId())
                .accessPolicyId(object.getAccessPolicy().getUid())
                .contractPolicyId(object.getContractPolicy().getUid())
                .criteria(object.getSelectorExpression().getCriteria())
                .build();
    }
}
