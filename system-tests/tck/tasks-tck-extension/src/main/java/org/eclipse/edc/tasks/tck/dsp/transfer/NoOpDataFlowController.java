/*
 *  Copyright (c) 2026 Think-it GmbH
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

package org.eclipse.edc.tasks.tck.dsp.transfer;

import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.transfer.spi.flow.DataFlowController;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DataFlowResponse;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.response.StatusResult;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * DataFlowController that short-circuits Data Plane interactions, in order to be used for DSP TCK.
 */
class NoOpDataFlowController implements DataFlowController {

    @Override
    public boolean canHandle(TransferProcess transferProcess) {
        return true;
    }

    @Override
    public StatusResult<DataFlowResponse> prepare(TransferProcess transferProcess, Policy policy) {
        return StatusResult.success(DataFlowResponse.Builder.newInstance().build());
    }

    @Override
    public @NotNull StatusResult<DataFlowResponse> start(TransferProcess transferProcess, Policy policy) {
        return StatusResult.success(DataFlowResponse.Builder.newInstance().build());
    }

    @Override
    public StatusResult<Void> suspend(TransferProcess transferProcess) {
        return StatusResult.success();
    }

    @Override
    public StatusResult<DataFlowResponse> resume(TransferProcess transferProcess) {
        return StatusResult.success(DataFlowResponse.Builder.newInstance().build());
    }

    @Override
    public StatusResult<Void> terminate(TransferProcess transferProcess) {
        return StatusResult.success();
    }

    @Override
    public StatusResult<Void> started(TransferProcess transferProcess) {
        return StatusResult.success();
    }

    @Override
    public StatusResult<Void> completed(TransferProcess transferProcess) {
        return StatusResult.success();
    }

    @Override
    public Set<String> transferTypesFor(Asset asset) {
        return Set.of("HttpData-PULL", "NonFinite-PULL");
    }

    @Override
    public Set<String> transferTypesFor(String assetId) {
        return Set.of("HttpData-PULL", "NonFinite-PULL");
    }
}
