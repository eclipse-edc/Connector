/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.api.management.contractagreement;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.edc.api.auth.spi.AuthorizationService;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.controlplane.services.spi.contractagreement.ContractAgreementService;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
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
import static org.eclipse.edc.participantcontext.spi.types.ParticipantResource.filterByParticipantContextId;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_TYPE;
import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;

public abstract class BaseContractAgreementApiV5Controller {
    protected final Monitor monitor;
    private final ContractAgreementService service;
    private final AuthorizationService authorizationService;
    private final TypeTransformerRegistry transformerRegistry;
    private final JsonObjectValidatorRegistry validatorRegistry;

    public BaseContractAgreementApiV5Controller(ContractAgreementService service, AuthorizationService authorizationService, TypeTransformerRegistry transformerRegistry,
                                                Monitor monitor, JsonObjectValidatorRegistry validatorRegistry) {
        this.service = service;
        this.authorizationService = authorizationService;
        this.transformerRegistry = transformerRegistry;
        this.monitor = monitor;
        this.validatorRegistry = validatorRegistry;
    }

    public JsonArray queryAgreements(String participantContextId, JsonObject querySpecJson, SecurityContext securityContext) {
        authorizationService.authorize(securityContext, participantContextId, participantContextId, ParticipantContext.class)
                .orElseThrow(exceptionMapper(ParticipantContext.class, participantContextId));

        QuerySpec querySpec;
        if (querySpecJson == null) {
            querySpec = QuerySpec.Builder.newInstance().build();
        } else {
            validatorRegistry.validate(EDC_QUERY_SPEC_TYPE, querySpecJson).orElseThrow(ValidationFailureException::new);

            querySpec = transformerRegistry.transform(querySpecJson, QuerySpec.class)
                    .orElseThrow(InvalidRequestException::new);
        }

        var query = querySpec.toBuilder()
                .filter(filterByParticipantContextId(participantContextId))
                .build();

        return service.search(query).orElseThrow(exceptionMapper(ContractDefinition.class, null)).stream()
                .map(it -> transformerRegistry.transform(it, JsonObject.class))
                .peek(r -> r.onFailure(f -> monitor.warning(f.getFailureDetail())))
                .filter(Result::succeeded)
                .map(Result::getContent)
                .collect(toJsonArray());
    }

    public JsonObject getAgreementById(String participantContextId, String id, SecurityContext securityContext) {
        authorizationService.authorize(securityContext, participantContextId, id, ContractAgreement.class)
                .orElseThrow(exceptionMapper(ContractAgreement.class, id));

        return Optional.of(id)
                .map(service::findById)
                .map(it -> transformerRegistry.transform(it, JsonObject.class)
                        .orElseThrow(failure -> new EdcException(failure.getFailureDetail())))
                .orElseThrow(() -> new ObjectNotFoundException(ContractAgreement.class, id));
    }

    public JsonObject getNegotiationByAgreementId(String participantContextId, String id, SecurityContext securityContext) {
        authorizationService.authorize(securityContext, participantContextId, id, ContractAgreement.class)
                .orElseThrow(exceptionMapper(ContractAgreement.class, id));

        return Optional.of(id)
                .map(service::findNegotiation)
                .map(it -> transformerRegistry.transform(it, JsonObject.class)
                        .orElseThrow(failure -> new EdcException(failure.getFailureDetail())))
                .orElseThrow(() -> new ObjectNotFoundException(ContractAgreement.class, id));
    }

}
