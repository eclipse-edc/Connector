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

package org.eclipse.edc.connector.controlplane.transform.edc.catalog.to;

import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.catalog.spi.DatasetRequest;
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import static org.eclipse.edc.connector.controlplane.catalog.spi.DatasetRequest.DATASET_REQUEST_COUNTER_PARTY_ADDRESS;
import static org.eclipse.edc.connector.controlplane.catalog.spi.DatasetRequest.DATASET_REQUEST_COUNTER_PARTY_ID;
import static org.eclipse.edc.connector.controlplane.catalog.spi.DatasetRequest.DATASET_REQUEST_PROTOCOL;

public class JsonObjectToDatasetRequestTransformer extends AbstractJsonLdTransformer<JsonObject, DatasetRequest> {

    public JsonObjectToDatasetRequestTransformer() {
        super(JsonObject.class, DatasetRequest.class);
    }

    @Override
    public @Nullable DatasetRequest transform(@NotNull JsonObject object, @NotNull TransformerContext context) {
        var builder = DatasetRequest.Builder.newInstance()
                .id(nodeId(object))
                .protocol(transformString(object.get(DATASET_REQUEST_PROTOCOL), context));

        var counterPartyAddress = transformString(object.get(DATASET_REQUEST_COUNTER_PARTY_ADDRESS), context);

        var counterPartyId = Optional.ofNullable(object.get(DATASET_REQUEST_COUNTER_PARTY_ID))
                .map(it -> transformString(it, context))
                .orElse(counterPartyAddress);

        return builder
                .counterPartyAddress(counterPartyAddress)
                .counterPartyId(counterPartyId)
                .build();
    }

}
