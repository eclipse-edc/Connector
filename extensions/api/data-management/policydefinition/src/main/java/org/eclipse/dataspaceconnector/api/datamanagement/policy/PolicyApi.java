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
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.eclipse.dataspaceconnector.api.query.QuerySpecDto;
import org.eclipse.dataspaceconnector.policy.model.Policy;

import java.util.List;

@OpenAPIDefinition
@Tag(name = "Policy")
public interface PolicyApi {

    List<Policy> getAllPolicies(@Valid QuerySpecDto querySpecDto);

    Policy getPolicy(String id);

    void createPolicy(Policy policy);

    void deletePolicy(String id);

}
