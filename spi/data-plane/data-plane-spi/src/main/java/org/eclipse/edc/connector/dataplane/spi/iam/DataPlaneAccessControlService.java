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

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.Map;

/**
 * This service is used to grant or deny a request for data based on the token attached to the request, and some request data.
 * For example, an implementation could make that decision based on the URL pattern of the data request.
 * <p>
 * The default implementation provided by EDC always returns {@link Result#success()}.
 */
@ExtensionPoint
@FunctionalInterface
public interface DataPlaneAccessControlService {

    /**
     * Grants or denies access to a particular resource (DataAddress) based on the supplied credential (ClaimToken) and
     * additional request information (requestData).
     * <p>
     * Note that if access is denied ({@link Result#failed()}), it is advised <strong>not</strong> to forward the failure
     * detail to the requestor for security reasons.
     * <p>
     *
     * @param claimToken     The credential that was attached to the data request received by the data plane
     * @param address        The resource address of the data that is being requested
     * @param requestData    Additional information about the request, e.g. URL pattern, additional headers, query params, etc.
     * @param additionalData Additional information about the claim token, such as participant ID, agreement ID, transfer process ID etc.
     * @return success if access to the resource is granted, a failure otherwise.
     */
    Result<Void> checkAccess(ClaimToken claimToken, DataAddress address, Map<String, Object> requestData, Map<String, Object> additionalData);
}
