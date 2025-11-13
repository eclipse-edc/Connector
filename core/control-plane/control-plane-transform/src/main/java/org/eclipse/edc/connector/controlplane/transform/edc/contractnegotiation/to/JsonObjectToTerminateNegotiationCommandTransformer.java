/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.controlplane.transform.edc.contractnegotiation.to;

import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.contract.spi.types.command.TerminateNegotiationCommand;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.connector.controlplane.contract.spi.types.command.TerminateNegotiationCommand.TERMINATE_NEGOTIATION_REASON;

public class JsonObjectToTerminateNegotiationCommandTransformer extends AbstractJsonLdTransformer<JsonObject, TerminateNegotiationCommand> {

    public JsonObjectToTerminateNegotiationCommandTransformer() {
        super(JsonObject.class, TerminateNegotiationCommand.class);
    }

    @Override
    public @Nullable TerminateNegotiationCommand transform(@NotNull JsonObject input, @NotNull TransformerContext context) {
        var id = nodeId(input);
        var reason = transformString(input.get(TERMINATE_NEGOTIATION_REASON), context);

        return new TerminateNegotiationCommand(id, reason);
    }
}
