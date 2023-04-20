/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.api.management.policy;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import org.eclipse.edc.api.model.IdResponseDto;
import org.eclipse.edc.api.query.QuerySpecDto;
import org.eclipse.edc.api.transformer.DtoTransformerRegistry;
import org.eclipse.edc.connector.api.management.policy.model.PolicyDefinitionNewRequestDto;
import org.eclipse.edc.connector.api.management.policy.model.PolicyDefinitionResponseDto;
import org.eclipse.edc.connector.api.management.policy.model.PolicyDefinitionUpdateDto;
import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.spi.policydefinition.PolicyDefinitionService;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;

import java.util.List;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/v2/policydefinitions")
public class PolicyDefinitionNewApiController implements PolicyDefinitionNewApi {

    private final DtoTransformerRegistry transformerRegistry;
    private final PolicyDefinitionService service;

    public PolicyDefinitionNewApiController(DtoTransformerRegistry transformerRegistry, PolicyDefinitionService service) {
        this.transformerRegistry = transformerRegistry;
        this.service = service;
    }

    @Override
    public List<PolicyDefinitionResponseDto> queryAllPolicyDefinitions(QuerySpecDto querySpecDto) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public PolicyDefinitionResponseDto getPolicyDefinition(String id) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @POST
    @Override
    public IdResponseDto createPolicyDefinition(PolicyDefinitionNewRequestDto policy) {
        var inputDefinition = transformerRegistry.transform(policy, PolicyDefinition.class)
                .orElseThrow(failure -> new InvalidRequestException(failure.getMessages()));

        var createdDefinition = service.create(inputDefinition)
                .orElseThrow(exceptionMapper(PolicyDefinition.class, inputDefinition.getId()));

        return IdResponseDto.Builder.newInstance()
                .id(createdDefinition.getId())
                .createdAt(createdDefinition.getCreatedAt())
                .build();
    }

    @Override
    public void deletePolicyDefinition(String id) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public void updatePolicyDefinition(String policyId, PolicyDefinitionUpdateDto policy) {
        throw new UnsupportedOperationException("not implemented yet");
    }
}
