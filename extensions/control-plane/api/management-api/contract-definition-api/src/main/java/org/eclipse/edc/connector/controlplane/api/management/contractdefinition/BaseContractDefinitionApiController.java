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

package org.eclipse.edc.connector.controlplane.api.management.contractdefinition;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.eclipse.edc.api.model.IdResponse;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.controlplane.services.spi.contractdefinition.ContractDefinitionService;
import org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier;
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
import static org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition.CONTRACT_DEFINITION_TYPE;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;


public abstract class BaseContractDefinitionApiController {
    protected final TypeTransformerRegistry transformerRegistry;
    protected final ContractDefinitionService service;
    protected final Monitor monitor;
    protected final JsonObjectValidatorRegistry validatorRegistry;
    protected final SingleParticipantContextSupplier participantContextSupplier;

    public BaseContractDefinitionApiController(TypeTransformerRegistry transformerRegistry, ContractDefinitionService service,
                                               Monitor monitor, JsonObjectValidatorRegistry validatorRegistry, SingleParticipantContextSupplier participantContextSupplier) {
        this.transformerRegistry = transformerRegistry;
        this.service = service;
        this.monitor = monitor;
        this.validatorRegistry = validatorRegistry;
        this.participantContextSupplier = participantContextSupplier;
    }

    public JsonArray queryContractDefinitions(JsonObject querySpecJson) {
        QuerySpec querySpec;
        if (querySpecJson == null) {
            querySpec = QuerySpec.Builder.newInstance().build();
        } else {
            validatorRegistry.validate(QuerySpec.EDC_QUERY_SPEC_TYPE, querySpecJson)
                    .orElseThrow(ValidationFailureException::new);

            querySpec = transformerRegistry.transform(querySpecJson, QuerySpec.class)
                    .orElseThrow(InvalidRequestException::new);
        }

        return service.search(querySpec).orElseThrow(exceptionMapper(ContractDefinition.class)).stream()
                .map(contractDefinition -> transformerRegistry.transform(contractDefinition, JsonObject.class))
                .peek(r -> r.onFailure(f -> monitor.warning(f.getFailureDetail())))
                .filter(Result::succeeded)
                .map(Result::getContent)
                .collect(toJsonArray());
    }

    public JsonObject getContractDefinition(String id) {
        return Optional.ofNullable(id)
                .map(service::findById)
                .map(it -> transformerRegistry.transform(it, JsonObject.class))
                .map(Result::getContent)
                .orElseThrow(() -> new ObjectNotFoundException(ContractDefinition.class, id));
    }

    public JsonObject createContractDefinition(JsonObject createObject) {
        validatorRegistry.validate(CONTRACT_DEFINITION_TYPE, createObject)
                .orElseThrow(ValidationFailureException::new);

        var transform = transformerRegistry.transform(createObject, ContractDefinition.class)
                .orElseThrow(InvalidRequestException::new)
                .toBuilder()
                .participantContextId(participantContextSupplier.get().getParticipantContextId())
                .build();

        var responseDto = service.create(transform)
                .map(contractDefinition -> IdResponse.Builder.newInstance()
                        .id(contractDefinition.getId())
                        .createdAt(contractDefinition.getCreatedAt())
                        .build())
                .orElseThrow(exceptionMapper(ContractDefinition.class));

        return transformerRegistry.transform(responseDto, JsonObject.class)
                .orElseThrow(f -> new EdcException("Error creating response body: " + f.getFailureDetail()));
    }

    public void deleteContractDefinition(String id) {
        service.delete(id).orElseThrow(exceptionMapper(ContractDefinition.class, id));
    }

    public void updateContractDefinition(JsonObject updateObject) {
        validatorRegistry.validate(CONTRACT_DEFINITION_TYPE, updateObject)
                .orElseThrow(ValidationFailureException::new);

        var contractDefinition = transformerRegistry.transform(updateObject, ContractDefinition.class)
                .orElseThrow(InvalidRequestException::new);

        service.update(contractDefinition).orElseThrow(exceptionMapper(ContractDefinition.class));
    }
}
