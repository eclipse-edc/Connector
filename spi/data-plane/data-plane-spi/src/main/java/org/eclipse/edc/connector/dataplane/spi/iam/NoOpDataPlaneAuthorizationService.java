/*
 *  Copyright (c) 2024 Contributors to the Eclipse Foundation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Contributors to the Eclipse Foundation - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.spi.iam;

import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;

import java.util.Map;

/**
 * Placeholder class meant to permit a data-plane to run without a DataPlaneAuthorizationService if not needed, but that
 * will always return failure.
 */
public class NoOpDataPlaneAuthorizationService implements DataPlaneAuthorizationService {

    private static final Result<DataAddress> FAILURE = Result
            .failure("PULL transfers are not supported unless a proper DataPlaneAuthorizationService is registered");

    @Override
    public Result<DataAddress> createEndpointDataReference(DataFlowStartMessage message) {
        return FAILURE;
    }

    @Override
    public Result<DataAddress> authorize(String token, Map<String, Object> requestData) {
        return FAILURE;
    }

    @Override
    public ServiceResult<Void> revokeEndpointDataReference(String transferProcessId, String reason) {
        return ServiceResult.unexpected(FAILURE.getFailureDetail());
    }
}
