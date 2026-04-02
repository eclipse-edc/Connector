/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.transform.edc.contractnegotiation.to;

import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.TerminateNegotiation;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.TerminateNegotiation.TERMINATE_NEGOTIATION_REASON;

public class JsonObjectToTerminateNegotiationTransformer extends AbstractJsonLdTransformer<JsonObject, TerminateNegotiation> {

    public JsonObjectToTerminateNegotiationTransformer() {
        super(JsonObject.class, TerminateNegotiation.class);
    }

    @Override
    public @Nullable TerminateNegotiation transform(@NotNull JsonObject input, @NotNull TransformerContext context) {
        var reason = transformString(input.get(TERMINATE_NEGOTIATION_REASON), context);

        return new TerminateNegotiation(reason);
    }
}
