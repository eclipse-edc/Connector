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

package org.eclipse.edc.signaling.port;

import okhttp3.Request;
import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneClient;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.http.spi.ControlApiHttpClient;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowProvisionMessage;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowResponseMessage;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;

import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;

/**
 * Client that implements the Data Plane Signaling spec
 */
public class DataPlaneSignalingClient implements DataPlaneClient {

    private final DataPlaneInstance dataPlane;
    private final ControlApiHttpClient httpClient;

    public DataPlaneSignalingClient(DataPlaneInstance dataPlane, ControlApiHttpClient httpClient) {
        this.dataPlane = dataPlane;
        this.httpClient = httpClient;
    }

    @Override
    public StatusResult<DataFlowResponseMessage> prepare(DataFlowProvisionMessage request) {
        return StatusResult.failure(ResponseStatus.FATAL_ERROR, "not implemented");
    }

    @Override
    public StatusResult<DataFlowResponseMessage> start(DataFlowStartMessage request) {
        return StatusResult.failure(ResponseStatus.FATAL_ERROR, "not implemented");
    }

    @Override
    public StatusResult<Void> suspend(String transferProcessId) {
        return StatusResult.failure(ResponseStatus.FATAL_ERROR, "not implemented");
    }

    @Override
    public StatusResult<Void> terminate(String transferProcessId) {
        return StatusResult.failure(ResponseStatus.FATAL_ERROR, "not implemented");
    }

    @Override
    public StatusResult<Void> checkAvailability() {
        var requestBuilder = new Request.Builder().get().url(dataPlane.getUrl() + "/");
        return httpClient.request(requestBuilder)
                .flatMap(result -> result.map(it -> StatusResult.success()).orElse(failure ->
                        StatusResult.failure(FATAL_ERROR, "Communication with data-plane %s failed: %s".formatted(dataPlane.getId(), failure.getFailureDetail()))));
    }

}
