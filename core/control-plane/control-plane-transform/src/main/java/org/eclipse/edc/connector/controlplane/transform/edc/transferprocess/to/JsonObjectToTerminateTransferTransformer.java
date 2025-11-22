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

package org.eclipse.edc.connector.controlplane.transform.edc.transferprocess.to;

import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TerminateTransfer;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TerminateTransfer.TERMINATE_TRANSFER_REASON;

public class JsonObjectToTerminateTransferTransformer extends AbstractJsonLdTransformer<JsonObject, TerminateTransfer> {

    public JsonObjectToTerminateTransferTransformer() {
        super(JsonObject.class, TerminateTransfer.class);
    }

    @Override
    public @Nullable TerminateTransfer transform(@NotNull JsonObject input, @NotNull TransformerContext context) {
        var reason = transformString(input.get(TERMINATE_TRANSFER_REASON), context);
        return new TerminateTransfer(reason);
    }

}
