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
     * @return The {@link ClaimToken} if success, failure otherwise
     */
    public ServiceResult<ClaimToken> verifyToken(TokenRepresentation tokenRepresentation) {
        // TODO: since we are pushing here the invocation of the IdentityService we don't know the audience here
        //  The audience removal will be tackle next. IdentityService that relies on this parameter would not work
        //  for the time being.
        var result = identityService.verifyJwtToken(tokenRepresentation, null);

        if (result.failed()) {
            monitor.debug(() -> "Unauthorized: %s".formatted(result.getFailureDetail()));
            return ServiceResult.unauthorized("Unauthorized");
        }
        return ServiceResult.success(result.getContent());
    }
}
