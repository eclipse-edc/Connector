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
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.RejectNegotiation;
import org.eclipse.edc.jsonld.spi.transformer.JsonLdToModelTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JsonObjectToRejectNegotiationTransformer extends JsonLdToModelTransformer<JsonObject, RejectNegotiation> {

    public JsonObjectToRejectNegotiationTransformer() {
        super(JsonObject.class, RejectNegotiation.class);
    }

    @Override
    public @Nullable RejectNegotiation transform(@NotNull JsonObject input, @NotNull TransformerContext context) {
        return new RejectNegotiation();
    }

}
