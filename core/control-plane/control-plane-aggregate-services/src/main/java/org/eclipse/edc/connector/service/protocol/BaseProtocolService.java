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

package org.eclipse.edc.connector.service.protocol;

import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.ServiceResult;

import java.util.function.Function;

/**
 * Base class for all protocol service implementation. This will contain common logic such as validating the JWT token
 * and extracting the {@link ClaimToken}
 */
public abstract class BaseProtocolService {

    private final IdentityService identityService;

    private final Monitor monitor;

    protected BaseProtocolService(IdentityService identityService, Monitor monitor) {
        this.identityService = identityService;
        this.monitor = monitor;
    }

    /**
     * Validate and extract the {@link ClaimToken} from the input {@link TokenRepresentation} by using the {@link IdentityService}
     *
     * @param tokenRepresentation The input {@link TokenRepresentation}
     * @param callback            The callback to invoke once the token has been validated and extracted
     * @return The result of the callback invocation
     */
    public <T> ServiceResult<T> withClaimToken(TokenRepresentation tokenRepresentation, Function<ClaimToken, ServiceResult<T>> callback) {
        // TODO: since we are pushing here the invocation of the IdentityService we don't know the audience here
        //  The audience removal will be tackle next. IdentityService that relies on this parameter would not work
        //  for the time being.
        var result = identityService.verifyJwtToken(tokenRepresentation, null);

        if (result.failed()) {
            monitor.debug(() -> "Unauthorized: %s".formatted(result.getFailureDetail()));
            return ServiceResult.unauthorized("Unauthorized");
        }
        return callback.apply(result.getContent());
    }
}
