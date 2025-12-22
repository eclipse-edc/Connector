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

import org.eclipse.edc.signaling.domain.DspDataAddress;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.eclipse.edc.transform.spi.TypeTransformer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.signaling.domain.DspDataAddress.DSP_DATA_ADDRESS_ENDPOINT;

public class DspDataAddressToDataAddressTransformer implements TypeTransformer<DspDataAddress, DataAddress> {
    @Override
    public Class<DspDataAddress> getInputType() {
        return DspDataAddress.class;
    }

    @Override
    public Class<DataAddress> getOutputType() {
        return DataAddress.class;
    }

    @Override
    public @Nullable DataAddress transform(@NotNull DspDataAddress dataAddress, @NotNull TransformerContext context) {
        var builder = DataAddress.Builder.newInstance()
                .type(dataAddress.getEndpointType());

        dataAddress.getEndpointProperties().forEach(property -> builder.property(property.getName(), property.getValue()));

        return builder
                .property(DSP_DATA_ADDRESS_ENDPOINT, dataAddress.getEndpoint())
                .build();
    }
}
