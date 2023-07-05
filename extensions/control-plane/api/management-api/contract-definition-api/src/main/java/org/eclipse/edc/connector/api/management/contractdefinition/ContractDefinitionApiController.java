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

package org.eclipse.edc.connector.api.management.contractdefinition;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.api.model.IdResponseDto;
import org.eclipse.edc.api.model.QuerySpecDto;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.spi.contractdefinition.ContractDefinitionService;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ObjectNotFoundException;
import org.eclipse.edc.web.spi.exception.ValidationFailureException;

import java.util.Optional;

import static jakarta.json.stream.JsonCollectors.toJsonArray;
import static org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_TYPE;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;


@Produces({ MediaType.APPLICATION_JSON })
@Path("/v2/contractdefinitions")
public class ContractDefinitionApiController implements ContractDefinitionApi {
    private final TypeTransformerRegistry transformerRegistry;
    private final ContractDefinitionService service;
    private final Monitor monitor;
    private final JsonObjectValidatorRegistry validatorRegistry;

    public ContractDefinitionApiController(TypeTransformerRegistry transformerRegistry, ContractDefinitionService service,
                                           Monitor monitor, JsonObjectValidatorRegistry validatorRegistry) {
        this.transformerRegistry = transformerRegistry;
        this.service = service;
        this.monitor = monitor;
        this.validatorRegistry = validatorRegistry;
    }

    @POST
    @Path("/request")
    @Override
    public JsonArray queryAllContractDefinitions(JsonObject querySpecDto) {
        QuerySpec querySpec;
        if (querySpecDto == null) {
            querySpec = QuerySpec.Builder.newInstance().build();
        } else {
            validatorRegistry.validate(QuerySpecDto.EDC_QUERY_SPEC_TYPE, querySpecDto)
                    .orElseThrow(ValidationFailureException::new);

            querySpec = transformerRegistry.transform(querySpecDto, QuerySpecDto.class)
                    .compose(dto -> transformerRegistry.transform(dto, QuerySpec.class))
                    .orElseThrow(InvalidRequestException::new);
        }

        try (var stream = service.query(querySpec).orElseThrow(exceptionMapper(ContractDefinition.class))) {
            return stream
                    .map(contractDefinition -> transformerRegistry.transform(contractDefinition, JsonObject.class))
                    .peek(r -> r.onFailure(f -> monitor.warning(f.getFailureDetail())))
                    .filter(Result::succeeded)
                    .map(Result::getContent)
                    .collect(toJsonArray());
        }
    }

    @GET
    @Path("{id}")
    @Override
    public JsonObject getContractDefinition(@PathParam("id") String id) {
        return Optional.ofNullable(id)
                .map(service::findById)
                .map(it -> transformerRegistry.transform(it, JsonObject.class))
                .map(Result::getContent)
                .orElseThrow(() -> new ObjectNotFoundException(ContractDefinition.class, id));
    }

    @POST
    @Override
    public JsonObject createContractDefinition(JsonObject createObject) {
        validatorRegistry.validate(CONTRACT_DEFINITION_TYPE, createObject)
                .orElseThrow(ValidationFailureException::new);

        var transform = transformerRegistry.transform(createObject, ContractDefinition.class)
                .orElseThrow(InvalidRequestException::new);

        var responseDto = service.create(transform)
                .map(contractDefinition -> IdResponseDto.Builder.newInstance()
                        .id(contractDefinition.getId())
                        .createdAt(contractDefinition.getCreatedAt())
                        .build())
                .orElseThrow(exceptionMapper(ContractDefinition.class));

        return transformerRegistry.transform(responseDto, JsonObject.class)
                .orElseThrow(f -> new EdcException("Error creating response body: " + f.getFailureDetail()));
    }

    @DELETE
    @Path("{id}")
    @Override
    public void deleteContractDefinition(@PathParam("id") String id) {
        service.delete(id).orElseThrow(exceptionMapper(ContractDefinition.class, id));
    }

    @PUT
    @Override
    public void updateContractDefinition(JsonObject updateObject) {
        validatorRegistry.validate(CONTRACT_DEFINITION_TYPE, updateObject)
                .orElseThrow(ValidationFailureException::new);

        var contractDefinition = transformerRegistry.transform(updateObject, ContractDefinition.class)
                .orElseThrow(InvalidRequestException::new);

        service.update(contractDefinition).orElseThrow(exceptionMapper(ContractDefinition.class));
    }
}
