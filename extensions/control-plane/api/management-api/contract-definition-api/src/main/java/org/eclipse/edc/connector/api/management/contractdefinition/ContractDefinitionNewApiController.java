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
import org.eclipse.edc.api.query.QuerySpecDto;
import org.eclipse.edc.connector.api.management.contractdefinition.model.ContractDefinitionRequestDto;
import org.eclipse.edc.connector.api.management.contractdefinition.model.ContractDefinitionResponseDto;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.spi.contractdefinition.ContractDefinitionService;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ObjectNotFoundException;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;


@Produces({ MediaType.APPLICATION_JSON })
@Path("/v2/contractdefinitions")
public class ContractDefinitionNewApiController implements ContractDefinitionNewApi {
    private final JsonLd jsonLdService;
    private final TypeTransformerRegistry transformerRegistry;
    private final ContractDefinitionService service;
    private final Monitor monitor;

    public ContractDefinitionNewApiController(JsonLd jsonLdService, TypeTransformerRegistry transformerRegistry, ContractDefinitionService service, Monitor monitor) {
        this.jsonLdService = jsonLdService;
        this.transformerRegistry = transformerRegistry;
        this.service = service;
        this.monitor = monitor;
    }

    @POST
    @Path("/request")
    @Override
    public List<JsonObject> queryAllContractDefinitions(JsonObject querySpecDto) {

        Function<Result<JsonObject>, Result<QuerySpec>> expandedMapper =
                expandedResult -> expandedResult
                        .compose(jsonObject -> transformerRegistry.transform(jsonObject, QuerySpecDto.class))
                        .compose(dto -> transformerRegistry.transform(dto, QuerySpec.class));

        var querySpec = ofNullable(querySpecDto)
                .map(jsonLdService::expand)
                .map(expandedMapper)
                .orElse(Result.success(QuerySpec.Builder.newInstance().build()))
                .orElseThrow(InvalidRequestException::new);

        try (var stream = service.query(querySpec).orElseThrow(exceptionMapper(ContractDefinition.class))) {
            return stream
                    .map(contractDefinition -> transformerRegistry.transform(contractDefinition, ContractDefinitionResponseDto.class)
                            .compose(dto -> transformerRegistry.transform(dto, JsonObject.class).compose(jsonLdService::compact)))
                    .peek(r -> r.onFailure(f -> monitor.warning(f.getFailureDetail())))
                    .filter(Result::succeeded)
                    .map(Result::getContent)
                    .collect(toList());
        }
    }

    @GET
    @Path("{id}")
    @Override
    public JsonObject getContractDefinition(@PathParam("id") String id) {
        return Optional.ofNullable(id)
                .map(service::findById)
                .map(it -> transformerRegistry.transform(it, ContractDefinitionResponseDto.class)
                        .compose(dto -> transformerRegistry.transform(dto, JsonObject.class).compose(jsonLdService::compact)))
                .map(Result::getContent)
                .orElseThrow(() -> new ObjectNotFoundException(ContractDefinition.class, id));

    }

    @POST
    @Override
    public JsonObject createContractDefinition(JsonObject createObject) {

        var transform = jsonLdService.expand(createObject)
                .compose(expandedJson -> transformerRegistry.transform(expandedJson, ContractDefinitionRequestDto.class))
                .compose(dto -> transformerRegistry.transform(dto, ContractDefinition.class))
                .orElseThrow(InvalidRequestException::new);

        var responseDto = service.create(transform)
                .map(contractDefinition -> IdResponseDto.Builder.newInstance()
                        .id(contractDefinition.getId())
                        .createdAt(contractDefinition.getCreatedAt())
                        .build())
                .orElseThrow(exceptionMapper(ContractDefinition.class));

        return transformerRegistry.transform(responseDto, JsonObject.class)
                .compose(jsonLdService::compact)
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
        var rqDto = jsonLdService.expand(updateObject)
                .compose(expanded -> transformerRegistry.transform(expanded, ContractDefinitionRequestDto.class))
                .compose(dto -> transformerRegistry.transform(dto, ContractDefinition.class))
                .orElseThrow(InvalidRequestException::new);

        service.update(rqDto).orElseThrow(exceptionMapper(ContractDefinition.class));
    }
}
