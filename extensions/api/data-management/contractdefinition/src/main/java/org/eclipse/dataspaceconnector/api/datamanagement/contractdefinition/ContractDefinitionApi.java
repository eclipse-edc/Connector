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

package org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.model.ContractDefinitionDto;
import org.eclipse.dataspaceconnector.spi.query.SortOrder;

import java.util.List;

@OpenAPIDefinition
@Tag(name = "Contract Definition")
public interface ContractDefinitionApi {

    List<ContractDefinitionDto> getAllContractDefinitions(Integer offset, Integer limit, String filterExpression, SortOrder sortOrder, String sortField);

    ContractDefinitionDto getContractDefinition(String id);

    void createContractDefinition(@Valid ContractDefinitionDto dto);

    void deleteContractDefinition(String id);
}
