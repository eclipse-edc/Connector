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

package org.eclipse.edc.api.iam.identitytrust.sts.controller;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.api.iam.identitytrust.sts.SecureTokenServiceApi;
import org.eclipse.edc.api.iam.identitytrust.sts.exception.StsTokenException;
import org.eclipse.edc.api.iam.identitytrust.sts.model.StsTokenRequest;
import org.eclipse.edc.api.iam.identitytrust.sts.model.StsTokenResponse;
import org.eclipse.edc.iam.identitytrust.sts.spi.model.StsAccountTokenAdditionalParams;
import org.eclipse.edc.iam.identitytrust.sts.spi.service.StsAccountService;
import org.eclipse.edc.iam.identitytrust.sts.spi.service.StsClientTokenGeneratorService;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.validator.spi.Validator;

@Path("/")
public class SecureTokenServiceApiController implements SecureTokenServiceApi {

    private final StsAccountService clientService;

    private final StsClientTokenGeneratorService tokenService;

    private final Validator<StsTokenRequest> tokenRequestValidator;

    public SecureTokenServiceApiController(StsAccountService clientService, StsClientTokenGeneratorService tokenService, Validator<StsTokenRequest> tokenRequestValidator) {
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
        tokenRequestValidator.validate(request).orElseThrow(StsTokenException.validationException(request.getClientId()));
        return clientService.findByClientId(request.getClientId())
                .compose(client -> clientService.authenticate(client, request.getClientSecret()))
                .compose(client -> tokenService.tokenFor(client, additionalParams(request)))
                .map(this::mapToken)
                .orElseThrow(StsTokenException.tokenException(request.getClientId()));

    }

    private StsAccountTokenAdditionalParams additionalParams(StsTokenRequest request) {
        return StsAccountTokenAdditionalParams.Builder.newInstance()
                .audience(request.getAudience())
                .accessToken(request.getToken())
                .bearerAccessScope(request.getBearerAccessScope())
                .build();
    }

    private StsTokenResponse mapToken(TokenRepresentation tokenRepresentation) {
        return new StsTokenResponse(tokenRepresentation.getToken(), tokenRepresentation.getExpiresIn());
    }
}
