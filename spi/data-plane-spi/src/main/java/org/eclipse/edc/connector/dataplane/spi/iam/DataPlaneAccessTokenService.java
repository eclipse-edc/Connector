/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.dataplane.spi.iam;

import org.eclipse.edc.connector.dataplane.spi.AccessTokenData;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.Map;

/**
 * This service serves as facility to both create and validate access tokens.
 */
@ExtensionPoint
public interface DataPlaneAccessTokenService {

    /**
     * Takes a set of headers and claims (represented as {@link TokenParameters}) and creates an access token for a particular data resource.
     * The resulting token grants access to that resource, and that resource <em>only</em> and must enable restoring the original claims
     * and the data resource from it.
     * <p>
     * Common patterns include encoding that information directly in the token, or storing that information in a separate store and correlating it with the token ID.
     *
     * @param parameters          Headers and claims that are to be included in the token.
     * @param backendDataAddress  Information about the backend data resource for which the token is to be generated. May contain additional information about the token, such as an {@code authType}
     * @param additionalTokenData Additional data that further characterizes the token, but should not be included in the resulting {@link TokenRepresentation}.
     * @return The token representation
     */
    Result<TokenRepresentation> obtainToken(TokenParameters parameters, DataAddress backendDataAddress, Map<String, Object> additionalTokenData);

    /**
     * Takes a token and restores from it the original information that was used to create the token, most notably claims ({@link org.eclipse.edc.spi.iam.ClaimToken})
     * and a {@link DataAddress}. That information could be encoded directly on the token (e.g. in the form of a JWT/JWE), or it could be stored separately.
     *
     * @param token A string representation of the token.
     * @return A {@link AccessTokenData} that contains the original claims and the data resource ({@link DataAddress}. If the token could not be restored, a failure is returned.
     */
    Result<AccessTokenData> resolve(String token);

    /**
     * Revokes the {@link AccessTokenData} associated with the id
     *
     * @param transferProcessId The id of the {@link AccessTokenData}
     * @param reason            The reason for the revocation
     * @return Success if revoked, failure otherwise
     */
    ServiceResult<Void> revoke(String transferProcessId, String reason);

}
