/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.edc.connector.transfer.dataplane.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.edc.connector.transfer.dataplane.spi.security.DataEncrypter;
import org.eclipse.edc.jwt.spi.TokenValidationService;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.PublicKeyResolver;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.NotAuthorizedException;

import static java.lang.String.format;
import static org.eclipse.edc.connector.transfer.dataplane.spi.TransferDataPlaneConstants.DATA_ADDRESS;

@Path("/token")
public class ConsumerPullTransferTokenValidationApiController implements ConsumerPullTransferTokenValidationApi {
    private final TokenValidationService service;
    private final DataEncrypter dataEncrypter;
    private final TypeManager typeManager;
    private final PublicKeyResolver publicKeyResolver;

    public ConsumerPullTransferTokenValidationApiController(TokenValidationService service, DataEncrypter dataEncrypter, TypeManager typeManager, PublicKeyResolver publicKeyResolver) {
        this.service = service;
        this.dataEncrypter = dataEncrypter;
        this.typeManager = typeManager;
        this.publicKeyResolver = publicKeyResolver;
    }

    /**
     * Validate the token provided in input and decrypt the {@link DataAddress}
     * contained in its claims.
     *
     * @param token Input token.
     * @return Decrypted DataAddress contained in the input token claims.
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @Override
    public DataAddress validate(@HeaderParam(HttpHeaders.AUTHORIZATION) String token) {
        return service.validate(token, publicKeyResolver)
                .map(this::extractDataAddressClaim)
                .map(this::toDataAddress)
                .orElseThrow(failure -> new NotAuthorizedException("Token validation failed: " + failure.getFailureDetail()));
    }

    String extractDataAddressClaim(ClaimToken claims) {
        var claim = claims.getClaim(DATA_ADDRESS);
        if (!(claim instanceof String)) {
            throw new InvalidRequestException(format("Missing claim `%s` in token", DATA_ADDRESS));
        }
        return (String) claim;
    }

    private DataAddress toDataAddress(String claim) {
        return typeManager.readValue(dataEncrypter.decrypt(claim), DataAddress.class);
    }
}
