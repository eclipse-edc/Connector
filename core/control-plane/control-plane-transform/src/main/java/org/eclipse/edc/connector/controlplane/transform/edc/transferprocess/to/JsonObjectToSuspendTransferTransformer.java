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
import org.eclipse.edc.connector.controlplane.transfer.spi.types.SuspendTransfer;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.connector.controlplane.transfer.spi.types.SuspendTransfer.SUSPEND_TRANSFER_REASON;

public class JsonObjectToSuspendTransferTransformer extends AbstractJsonLdTransformer<JsonObject, SuspendTransfer> {

    public JsonObjectToSuspendTransferTransformer() {
        super(JsonObject.class, SuspendTransfer.class);
    }

    @Override
    public @Nullable SuspendTransfer transform(@NotNull JsonObject input, @NotNull TransformerContext context) {
        var reason = transformString(input.get(SUSPEND_TRANSFER_REASON), context);
        return new SuspendTransfer(reason);
    }

}
