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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.api.management.transferprocess.transform;

import jakarta.json.JsonObject;
import org.eclipse.edc.connector.api.management.transferprocess.model.TerminateTransferDto;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.connector.api.management.transferprocess.model.TerminateTransferDto.EDC_TERMINATE_TRANSFER_REASON;

public class JsonObjectToTerminateTransferDtoTransformer extends AbstractJsonLdTransformer<JsonObject, TerminateTransferDto> {

    public JsonObjectToTerminateTransferDtoTransformer() {
        super(JsonObject.class, TerminateTransferDto.class);
    }

    @Override
    public @Nullable TerminateTransferDto transform(@NotNull JsonObject input, @NotNull TransformerContext context) {
        var builder = TerminateTransferDto.Builder.newInstance();

        visitProperties(input, key -> {
            if (key.equals(EDC_TERMINATE_TRANSFER_REASON)) {
                return v -> builder.reason(transformString(v, context));
            }
            return doNothing();
        });

        return builder.build();
    }

}
