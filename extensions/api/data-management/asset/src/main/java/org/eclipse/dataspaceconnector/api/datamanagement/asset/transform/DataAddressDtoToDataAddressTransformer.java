/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.dataspaceconnector.api.datamanagement.asset.transform;

import org.eclipse.dataspaceconnector.api.datamanagement.asset.model.DataAddressDto;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformer;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DataAddressDtoToDataAddressTransformer implements DtoTransformer<DataAddressDto, DataAddress> {

    @Override
    public Class<DataAddressDto> getInputType() {
        return DataAddressDto.class;
    }

    @Override
    public Class<DataAddress> getOutputType() {
        return DataAddress.class;
    }

    @Override
    public @Nullable DataAddress transform(@Nullable DataAddressDto object, @NotNull TransformerContext context) {
        return DataAddress.Builder.newInstance().properties(object.getProperties()).build();
    }
}
