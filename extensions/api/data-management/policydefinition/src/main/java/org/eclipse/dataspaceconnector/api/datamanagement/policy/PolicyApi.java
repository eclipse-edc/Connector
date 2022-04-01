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

package org.eclipse.dataspaceconnector.api.datamanagement.policy;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import org.eclipse.dataspaceconnector.api.datamanagement.policy.model.PolicyDefinitionDto;
import org.eclipse.dataspaceconnector.spi.query.SortOrder;

import java.util.List;

@OpenAPIDefinition
public interface PolicyApi {

    List<PolicyDefinitionDto> getAllPolicies(Integer offset, Integer limit, String filterExpression, SortOrder sortOrder, String sortField);

    PolicyDefinitionDto getPolicy(String id);

    void createPolicy(PolicyDefinitionDto dto);

    void deletePolicy(String id);

}
