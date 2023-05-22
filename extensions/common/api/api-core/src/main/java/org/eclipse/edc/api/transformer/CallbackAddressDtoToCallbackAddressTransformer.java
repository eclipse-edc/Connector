/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.edc.api.transformer;

import org.eclipse.edc.api.model.CallbackAddressDto;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CallbackAddressDtoToCallbackAddressTransformer implements DtoTransformer<CallbackAddressDto, CallbackAddress> {

    @Override
    public Class<CallbackAddressDto> getInputType() {
        return CallbackAddressDto.class;
    }

    @Override
    public Class<CallbackAddress> getOutputType() {
        return CallbackAddress.class;
    }

    @Override
    public @Nullable CallbackAddress transform(@NotNull CallbackAddressDto object, @NotNull TransformerContext context) {
        return CallbackAddress.Builder.newInstance()
                .uri(object.getUri())
                .events(object.getEvents())
                .transactional(object.isTransactional())
                .authCodeId(object.getAuthCodeId())
                .authKey(object.getAuthKey())
                .build();
    }
}
