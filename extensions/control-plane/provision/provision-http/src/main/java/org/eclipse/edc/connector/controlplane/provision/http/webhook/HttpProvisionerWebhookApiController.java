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
 *
 */

package org.eclipse.edc.connector.controlplane.provision.http.webhook;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.connector.controlplane.provision.http.impl.HttpProvisionedContentResource;
import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DeprovisionedResource;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.ProvisionResponse;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;
import org.eclipse.edc.validator.spi.Violation;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ValidationFailureException;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static org.eclipse.edc.web.spi.exception.ServiceResultHandler.exceptionMapper;


@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Path("/callback")
@Deprecated(since = "0.14.0")
public class HttpProvisionerWebhookApiController implements HttpProvisionerWebhookApi {
    private final TransferProcessService transferProcessService;
    private final Validator<ProvisionerWebhookRequest> provisionerWebhookRequestValidator = new ProvisionerWebhookRequestValidator();

    public HttpProvisionerWebhookApiController(TransferProcessService transferProcessService) {
        this.transferProcessService = transferProcessService;
    }

    @Override
    @POST
    @Path("/{processId}/provision")
    public void callProvisionWebhook(@PathParam("processId") String transferProcessId, ProvisionerWebhookRequest request) {
        provisionerWebhookRequestValidator.validate(request).orElseThrow(ValidationFailureException::new);

        var contentResource = HttpProvisionedContentResource.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .assetId(request.getAssetId())
                .dataAddress(request.getContentDataAddress())
                .resourceName(request.getResourceName())
                .transferProcessId(transferProcessId)
                .hasToken(request.hasToken())
                .resourceDefinitionId(request.getResourceDefinitionId())
                .build();

        var response = ProvisionResponse.Builder.newInstance()
                .resource(contentResource)
                .secretToken(new SimpleSecretToken(request.getApiKeyJwt()))
                .build();

        transferProcessService.addProvisionedResource(transferProcessId, response)
                .orElseThrow(exceptionMapper(TransferProcess.class, transferProcessId));
    }

    @Override
    @POST
    @Path("/{processId}/deprovision")
    public void callDeprovisionWebhook(@PathParam("processId") String transferProcessId, DeprovisionedResource resource) {
        if (resource == null || resource.getProvisionedResourceId() == null) {
            throw new InvalidRequestException("Request body cannot be null and it should provide a valid provisionedResourceId value");
        }

        transferProcessService.completeDeprovision(transferProcessId, resource)
                .orElseThrow(exceptionMapper(TransferProcess.class, transferProcessId));
    }

    private static class ProvisionerWebhookRequestValidator implements Validator<ProvisionerWebhookRequest> {
        @Override
        public ValidationResult validate(ProvisionerWebhookRequest input) {
            Map<String, Function<ProvisionerWebhookRequest, ?>> fieldsNotNull = Map.of(
                    "resourceDefinitionId", ProvisionerWebhookRequest::getResourceDefinitionId,
                    "assetId", ProvisionerWebhookRequest::getAssetId,
                    "resourceName", ProvisionerWebhookRequest::getResourceName,
                    "contentDataAddress", ProvisionerWebhookRequest::getContentDataAddress,
                    "apiKeyJwt", ProvisionerWebhookRequest::getApiKeyJwt
            );

            var violations = new ArrayList<Violation>();
            fieldsNotNull.forEach((fieldName, supplier) -> {
                if (supplier.apply(input) == null) {
                    violations.add(Violation.violation(fieldName + " cannot be null", fieldName));
                }
            });

            if (violations.isEmpty()) {
                return ValidationResult.success();
            } else {
                return ValidationResult.failure(violations);
            }
        }
    }

}
