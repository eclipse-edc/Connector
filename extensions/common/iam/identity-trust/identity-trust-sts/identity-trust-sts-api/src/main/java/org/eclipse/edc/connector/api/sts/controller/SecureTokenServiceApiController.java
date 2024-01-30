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

package org.eclipse.edc.connector.api.sts.controller;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.connector.api.sts.SecureTokenServiceApi;
import org.eclipse.edc.connector.api.sts.model.StsTokenRequest;
import org.eclipse.edc.connector.api.sts.model.StsTokenResponse;
import org.eclipse.edc.iam.identitytrust.sts.model.StsClientTokenAdditionalParams;
import org.eclipse.edc.iam.identitytrust.sts.service.StsClientService;
import org.eclipse.edc.iam.identitytrust.sts.service.StsClientTokenGeneratorService;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.validator.spi.Validator;

import static org.eclipse.edc.connector.api.sts.exception.StsTokenException.tokenException;
import static org.eclipse.edc.connector.api.sts.exception.StsTokenException.validationException;

@Path("/")
public class SecureTokenServiceApiController implements SecureTokenServiceApi {

    private final StsClientService clientService;

    private final StsClientTokenGeneratorService tokenService;

    private final Validator<StsTokenRequest> tokenRequestValidator;

    public SecureTokenServiceApiController(StsClientService clientService, StsClientTokenGeneratorService tokenService, Validator<StsTokenRequest> tokenRequestValidator) {
        this.clientService = clientService;
        this.tokenService = tokenService;
        this.tokenRequestValidator = tokenRequestValidator;
    }

    @Consumes({ MediaType.APPLICATION_FORM_URLENCODED })
    @Produces({ MediaType.APPLICATION_JSON })
    @Path("token")
    @POST
    @Override
    public StsTokenResponse token(@BeanParam StsTokenRequest request) {
        tokenRequestValidator.validate(request).orElseThrow(validationException(request.getClientId()));
        return clientService.findByClientId(request.getClientId())
                .compose(client -> clientService.authenticate(client, request.getClientSecret()))
                .compose(client -> tokenService.tokenFor(client, additionalParams(request)))
                .map(this::mapToken)
                .orElseThrow(tokenException(request.getClientId()));

    }

    private StsClientTokenAdditionalParams additionalParams(StsTokenRequest request) {
        return StsClientTokenAdditionalParams.Builder.newInstance()
                .audience(request.getAudience())
                .accessToken(request.getAccessToken())
                .bearerAccessScope(request.getBearerAccessScope())
                .build();
    }

    private StsTokenResponse mapToken(TokenRepresentation tokenRepresentation) {
        return new StsTokenResponse(tokenRepresentation.getToken(), tokenRepresentation.getExpiresIn());
    }
}
