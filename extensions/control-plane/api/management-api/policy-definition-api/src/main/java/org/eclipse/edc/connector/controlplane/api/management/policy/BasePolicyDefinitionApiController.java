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

package org.eclipse.edc.connector.controlplane.api.management.policy;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.eclipse.edc.api.model.IdResponse;
import org.eclipse.edc.connector.controlplane.api.management.policy.model.PolicyEvaluationPlanRequest;
import org.eclipse.edc.connector.controlplane.api.management.policy.model.PolicyValidationResult;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.controlplane.services.spi.policydefinition.PolicyDefinitionService;
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

import java.util.ArrayList;

import static jakarta.json.stream.JsonCollectors.toJsonArray;
import static java.lang.String.format;
import static org.eclipse.edc.connector.controlplane.api.management.policy.model.PolicyEvaluationPlanRequest.EDC_POLICY_EVALUATION_PLAN_REQUEST_TYPE;
import static org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition.EDC_POLICY_DEFINITION_TYPE;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_TYPE;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

public abstract class BasePolicyDefinitionApiController {

    protected final Monitor monitor;
    protected final PolicyDefinitionService service;
    protected final TypeTransformerRegistry transformerRegistry;
    protected final JsonObjectValidatorRegistry validatorRegistry;
    protected final SingleParticipantContextSupplier participantContextSupplier;

    public BasePolicyDefinitionApiController(Monitor monitor, TypeTransformerRegistry transformerRegistry,
                                             PolicyDefinitionService service, JsonObjectValidatorRegistry validatorRegistry,
                                             SingleParticipantContextSupplier participantContextSupplier) {
        this.monitor = monitor;
        this.transformerRegistry = transformerRegistry;
        this.service = service;
        this.validatorRegistry = validatorRegistry;
        this.participantContextSupplier = participantContextSupplier;
    }

    public JsonArray queryPolicyDefinitions(JsonObject querySpecJson) {
        QuerySpec querySpec;
        if (querySpecJson == null) {
            querySpec = QuerySpec.Builder.newInstance().build();
        } else {
            validatorRegistry.validate(EDC_QUERY_SPEC_TYPE, querySpecJson).orElseThrow(ValidationFailureException::new);

            querySpec = transformerRegistry.transform(querySpecJson, QuerySpec.class)
                    .orElseThrow(InvalidRequestException::new);
        }

        return service.search(querySpec).orElseThrow(exceptionMapper(PolicyDefinition.class)).stream()
                .map(policyDefinition -> transformerRegistry.transform(policyDefinition, JsonObject.class))
                .filter(Result::succeeded)
                .map(Result::getContent)
                .collect(toJsonArray());
    }

    public JsonObject getPolicyDefinition(String id) {
        var definition = service.findById(id);
        if (definition == null) {
            throw new ObjectNotFoundException(PolicyDefinition.class, id);
        }

        return transformerRegistry.transform(definition, JsonObject.class)
                .orElseThrow(failure -> new ObjectNotFoundException(PolicyDefinition.class, id));
    }

    public JsonObject createPolicyDefinition(JsonObject request) {
        validatorRegistry.validate(EDC_POLICY_DEFINITION_TYPE, request).orElseThrow(ValidationFailureException::new);

        var participantContext = participantContextSupplier.get()
                .orElseThrow(exceptionMapper(PolicyDefinition.class));

        var definition = transformerRegistry.transform(request, PolicyDefinition.class)
                .orElseThrow(InvalidRequestException::new)
                .toBuilder()
                .participantContextId(participantContext.getParticipantContextId())
                .build();

        var createdDefinition = service.create(definition)
                .onSuccess(d -> monitor.debug(format("Policy Definition created %s", d.getId())))
                .orElseThrow(exceptionMapper(PolicyDefinition.class, definition.getId()));

        var responseDto = IdResponse.Builder.newInstance()
                .id(createdDefinition.getId())
                .createdAt(createdDefinition.getCreatedAt())
                .build();

        return transformerRegistry.transform(responseDto, JsonObject.class)
                .orElseThrow(f -> new EdcException("Error creating response body: " + f.getFailureDetail()));
    }

    public void deletePolicyDefinition(String id) {
        service.deleteById(id)
                .onSuccess(d -> monitor.debug(format("Policy Definition deleted %s", d.getId())))
                .orElseThrow(exceptionMapper(PolicyDefinition.class, id));
    }

    public void updatePolicyDefinition(String id, JsonObject input) {
        validatorRegistry.validate(EDC_POLICY_DEFINITION_TYPE, input).orElseThrow(ValidationFailureException::new);

        var policyDefinition = transformerRegistry.transform(input, PolicyDefinition.class)
                .orElseThrow(InvalidRequestException::new);

        service.update(policyDefinition)
                .onSuccess(d -> monitor.debug(format("Policy Definition updated %s", d.getId())))
                .orElseThrow(exceptionMapper(PolicyDefinition.class, id));
    }

    public JsonObject validatePolicyDefinition(String id) {
        var definition = service.findById(id);
        if (definition == null) {
            throw new ObjectNotFoundException(PolicyDefinition.class, id);
        }

        var messages = new ArrayList<String>();

        var result = service.validate(definition.getPolicy())
                .onFailure(failure -> messages.addAll(failure.getMessages()));

        var validationResult = new PolicyValidationResult(result.succeeded(), messages);

        return transformerRegistry.transform(validationResult, JsonObject.class)
                .orElseThrow(f -> new EdcException("Error creating response body: " + f.getFailureDetail()));
    }

    public JsonObject createExecutionPlan(String id, JsonObject request) {
        validatorRegistry.validate(EDC_POLICY_EVALUATION_PLAN_REQUEST_TYPE, request).orElseThrow(ValidationFailureException::new);

        var planeRequest = transformerRegistry.transform(request, PolicyEvaluationPlanRequest.class)
                .orElseThrow(InvalidRequestException::new);

        var definition = service.findById(id);
        if (definition == null) {
            throw new ObjectNotFoundException(PolicyDefinition.class, id);
        }

        var plan = service.createEvaluationPlan(planeRequest.policyScope(), definition.getPolicy())
                .orElseThrow(exceptionMapper(PolicyDefinition.class, definition.getId()));

        return transformerRegistry.transform(plan, JsonObject.class)
                .orElseThrow(f -> new EdcException("Error creating response body: " + f.getFailureDetail()));
    }

}
