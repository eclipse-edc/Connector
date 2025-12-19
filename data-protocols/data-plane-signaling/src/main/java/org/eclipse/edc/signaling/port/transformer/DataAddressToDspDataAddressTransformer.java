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

public class DataAddressToDspDataAddressTransformer implements TypeTransformer<DataAddress, DspDataAddress> {
    @Override
    public Class<DataAddress> getInputType() {
        return DataAddress.class;
    }

    @Override
    public Class<DspDataAddress> getOutputType() {
        return DspDataAddress.class;
    }

    @Override
    public @Nullable DspDataAddress transform(@NotNull DataAddress dataAddress, @NotNull TransformerContext context) {
        var builder = DspDataAddress.Builder
                .newInstance()
                .endpointType(dataAddress.getType())
                .endpoint(dataAddress.getStringProperty(DSP_DATA_ADDRESS_ENDPOINT));

        dataAddress.getProperties().forEach((key, value) -> builder.property(key, value.toString()));

        return builder.build();
    }
}
