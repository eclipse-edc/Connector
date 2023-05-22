/*
 *  Copyright (c) 2022 - 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.api.transformer;

import org.eclipse.edc.api.model.DataAddressDto;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DataAddressToDataAddressDtoTransformer implements DtoTransformer<DataAddress, DataAddressDto> {

    @Override
    public Class<DataAddress> getInputType() {
        return DataAddress.class;
    }

    @Override
    public Class<DataAddressDto> getOutputType() {
        return DataAddressDto.class;
    }

    @Override
    public @Nullable DataAddressDto transform(@NotNull DataAddress object, @NotNull TransformerContext context) {
        return DataAddressDto.Builder.newInstance().properties(object.getProperties()).build();
    }

}
