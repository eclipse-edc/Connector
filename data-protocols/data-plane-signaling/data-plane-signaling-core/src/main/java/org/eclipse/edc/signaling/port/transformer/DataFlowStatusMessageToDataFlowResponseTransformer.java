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

package org.eclipse.edc.signaling.port.transformer;

import org.eclipse.edc.connector.controlplane.transfer.spi.types.DataFlowResponse;
import org.eclipse.edc.signaling.domain.DataFlowStatusMessage;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.eclipse.edc.transform.spi.TypeTransformer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DataFlowStatusMessageToDataFlowResponseTransformer implements TypeTransformer<DataFlowStatusMessage, DataFlowResponse> {

    @Override
    public Class<DataFlowStatusMessage> getInputType() {
        return DataFlowStatusMessage.class;
    }

    @Override
    public Class<DataFlowResponse> getOutputType() {
        return DataFlowResponse.class;
    }

    @Override
    public @Nullable DataFlowResponse transform(@NotNull DataFlowStatusMessage dataFlowStatusMessage, @NotNull TransformerContext context) {
        return DataFlowResponse.Builder.newInstance()
                .dataAddress(context.transform(dataFlowStatusMessage.getDataAddress(), DataAddress.class))
                .async(dataFlowStatusMessage.getState().endsWith("ING"))
                .build();
    }

}
