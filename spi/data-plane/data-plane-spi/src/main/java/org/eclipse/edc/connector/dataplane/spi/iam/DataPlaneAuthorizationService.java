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

import org.eclipse.edc.connector.dataplane.spi.DataFlow;
import org.eclipse.edc.spi.result.Result;
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
     * Restores the original resource context (= {@link DataAddress}) for which the token was issued. Note that this correlation
     * is solely based on the token itself, and because that process is delegated to the {@link DataPlaneAccessTokenService}, it is up
     * to (implementations of) that service how the correlation is actually done. For example, some implementations might use JWT tokens
     * and use the {@code jti} claim inside it.
     * <p>
     * In addition, this service checks whether the token has permission to access the data identified by the {@code requestData} parameter, which could be a URL,
     * or query params, etc.
     * <p>
     * The result (if successful) of this method is the original {@link DataAddress} that was passed to a previous invocation of
     * {@link DataPlaneAuthorizationService#createEndpointDataReference(DataFlow)}, i.e. {@link DataFlowStartMessage#getSourceDataAddress()}.
     *
     * @param token       The raw, encoded token, e.g. serialized JWT
     * @param requestData Additional information about the request, such as a URL, query params, headers, etc.
     * @return The {@link DataAddress} that was encapsulated in the original {@link DataFlowStartMessage}
     */
    Result<DataAddress> authorize(String token, Map<String, Object> requestData);

}
