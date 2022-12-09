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
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.web.spi.exception.NotAuthorizedException;

import static java.lang.String.format;
import static java.lang.String.join;
import static org.eclipse.edc.connector.transfer.dataplane.spi.TransferDataPlaneConstants.DATA_ADDRESS;

@Path("/token")
public class ConsumerPullTransferTokenValidationApiController implements ConsumerPullTransferTokenValidationApi {
    private final TokenValidationService service;
    private final DataEncrypter dataEncrypter;
    private final TypeManager typeManager;

    public ConsumerPullTransferTokenValidationApiController(TokenValidationService service, DataEncrypter dataEncrypter, TypeManager typeManager) {
        this.service = service;
        this.dataEncrypter = dataEncrypter;
        this.typeManager = typeManager;
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
        var result = service.validate(token);
        if (result.failed()) {
            throw new NotAuthorizedException("Token validation failed: " + join(", ", result.getFailureMessages()));
        }

        var obj = result.getContent().getClaim(DATA_ADDRESS);
        if (!(obj instanceof String)) {
            throw new IllegalArgumentException(format("Missing claim `%s` in token", DATA_ADDRESS));
        }

        return typeManager.readValue(dataEncrypter.decrypt((String) obj), DataAddress.class);
    }
}
