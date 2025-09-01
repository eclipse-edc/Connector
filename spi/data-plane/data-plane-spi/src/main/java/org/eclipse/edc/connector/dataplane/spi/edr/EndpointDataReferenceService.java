/*
 *  Copyright (c) 2025 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.spi.edr;

import org.eclipse.edc.connector.dataplane.spi.DataFlow;
import org.eclipse.edc.connector.dataplane.spi.iam.DataPlaneAccessTokenService;
import org.eclipse.edc.connector.dataplane.spi.iam.DataPlaneAuthorizationService;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.types.domain.DataAddress;

// TODO: document
public interface EndpointDataReferenceService {

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
     * @param dataFlow the data flow.
     * @return A result containing the {@link DataAddress}, or a failure indicating the cause.
     */
    Result<DataAddress> createEndpointDataReference(DataFlow dataFlow);

    /**
     * Revokes the {@link DataAddress} created with {@link #createEndpointDataReference(DataFlow)}
     *
     * @param transferProcessId The id of the transfer process associated to the {@link DataAddress}
     * @param reason            The reason of the revocation
     * @return Successful if revoked, fails otherwise
     */
    ServiceResult<Void> revokeEndpointDataReference(String transferProcessId, String reason);

}
