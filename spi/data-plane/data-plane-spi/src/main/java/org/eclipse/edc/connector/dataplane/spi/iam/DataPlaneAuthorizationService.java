/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
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

package org.eclipse.edc.connector.dataplane.spi.iam;

import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;

import java.util.Map;

/**
 * This service issues tokens authorized to access a particular resource, identified by {@link DataFlowStartMessage#getSourceDataAddress()}. This
 * happens when the data transmission is initiated by the control plane.
 * <p>
 * When a data request is received, this service is used to restore the original {@link DataAddress} and claims from the token.
 */
public interface DataPlaneAuthorizationService {

    /**
     * Creates a DataAddress for a particular transfer request that contains the token and optionally an {@code authType}:
     * <pre>
     * {
     *   "dspace:dataAddress": {
     *     "@type": "dspace:DataAddress",
     *     "dspace:endpointType": "https://w3id.org/idsa/v4.1/HTTP",
     *     "dspace:endpoint": {
     *          "url": "http://example.com"
     *     }
     *     "dspace:endpointProperties": [
     *       {
     *         "@type": "dspace:EndpointProperty",
     *         "dspace:name": "authorization",
     *         "dspace:value": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
     *       },
     *       {
     *         "@type": "dspace:EndpointProperty",
     *         "dspace:name": "authType",
     *         "dspace:value": "bearer"
     *       }
     *     ]
     *   }
     * }
     * </pre>
     * <p>
     * In order to do so, the {@link DataPlaneAuthorizationService} delegates to the {@link DataPlaneAccessTokenService} to create the token.
     *
     * @param message The message that was received from the control plane to initiate the transfer.
     * @return A result containing the {@link DataAddress}, or a failure indicating the cause.
     */
    Result<DataAddress> createEndpointDataReference(DataFlowStartMessage message);

    /**
     * Restores the original resource context (= {@link DataAddress}) for which the token was issued. Note that this correlation
     * is solely based on the token itself, and because that process is delegated to the {@link DataPlaneAccessTokenService}, it is up
     * to (implementations of) that service how the correlation is actually done. For example, some implementations might use JWT tokens
     * and use the {@code jti} claim inside it.
     * <p>
     * In addition, this service checks whether the token has permission to access the data identified by the {@code requestData} parameter, which could be a URL,
     * or query params, etc.
     * <p>
     * The result (if successful) of this method is the original {@link DataAddress} that was passed to a previous invocation of
     * {@link DataPlaneAuthorizationService#createEndpointDataReference(DataFlowStartMessage)}, i.e. {@link DataFlowStartMessage#getSourceDataAddress()}.
     *
     * @param token       The raw, encoded token, e.g. serialized JWT
     * @param requestData Additional information about the request, such as a URL, query params, headers, etc.
     * @return The {@link DataAddress} that was encapsulated in the original {@link DataFlowStartMessage}
     */
    Result<DataAddress> authorize(String token, Map<String, Object> requestData);


    /**
     * Revokes the {@link DataAddress} created with {@link #createEndpointDataReference(DataFlowStartMessage)}
     *
     * @param transferProcessId The id of the transfer process associated to the {@link DataAddress}
     * @param reason            The reason of the revocation
     * @return Successful if revoked, fails otherwise
     */
    ServiceResult<Void> revokeEndpointDataReference(String transferProcessId, String reason);
}
