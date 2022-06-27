/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.dataspaceconnector.api.datamanagement.policy;

import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.dataspaceconnector.api.datamanagement.policy.service.PolicyDefinitionService;
import org.eclipse.dataspaceconnector.api.query.QuerySpecDto;
import org.eclipse.dataspaceconnector.api.result.ServiceResult;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformerRegistry;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.PolicyDefinition;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.exception.ObjectExistsException;
import org.eclipse.dataspaceconnector.spi.exception.ObjectNotFoundException;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;

@Produces({ MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_JSON })
@Path("/policydefinitions")
public class PolicyDefinitionApiController implements PolicyDefinitionApi {

    private final Monitor monitor;
    private final PolicyDefinitionService policyDefinitionService;
    private final DtoTransformerRegistry transformerRegistry;

    public PolicyDefinitionApiController(Monitor monitor, PolicyDefinitionService policyDefinitionService, DtoTransformerRegistry transformerRegistry) {
        this.monitor = monitor;
        this.policyDefinitionService = policyDefinitionService;
        this.transformerRegistry = transformerRegistry;
    }

    @GET
    @Override
    public List<PolicyDefinition> getAllPolicies(@Valid @BeanParam QuerySpecDto querySpecDto) {
        var result = transformerRegistry.transform(querySpecDto, QuerySpec.class);
        if (result.failed()) {
            monitor.warning("Error transforming QuerySpec: " + String.join(", ", result.getFailureMessages()));
            throw new IllegalArgumentException("Cannot transform QuerySpecDto object");
        }

        var spec = result.getContent();

        monitor.debug(format("get all policies %s", spec));

        return new ArrayList<>(policyDefinitionService.query(spec));

    }

    @GET
    @Path("{id}")
    @Override
    public PolicyDefinition getPolicy(@PathParam("id") String id) {
        monitor.debug(format("Attempting to return policy with ID %s", id));
        return Optional.of(id)
                .map(it -> policyDefinitionService.findById(id))
                .orElseThrow(() -> new ObjectNotFoundException(Policy.class, id));
    }

    @POST
    @Override
    public void createPolicy(PolicyDefinition policy) {

        var result = policyDefinitionService.create(policy);

        if (result.succeeded()) {
            monitor.debug(format("Policy created %s", policy.getUid()));
        } else {
            handleFailedResult(result, policy.getUid());
        }
    }

    @DELETE
    @Path("{id}")
    @Override
    public void deletePolicy(@PathParam("id") String id) {
        monitor.debug(format("Attempting to delete policy with id %s", id));
        var result = policyDefinitionService.deleteById(id);
        if (result.succeeded()) {
            monitor.debug(format("Policy deleted %s", id));
        } else {
            handleFailedResult(result, id);
        }
    }

    private void handleFailedResult(ServiceResult<PolicyDefinition> result, String id) {
        switch (result.reason()) {
            case NOT_FOUND:
                throw new ObjectNotFoundException(Policy.class, id);
            case CONFLICT:
                throw new ObjectExistsException(Policy.class, id);
            default:
                throw new EdcException("unexpected error");
        }
    }

}
