/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.transferprocess.transformer.type.from;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferCompletionMessage;
import org.eclipse.edc.jsonld.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

import static org.eclipse.edc.jsonld.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspCatalogPropertyAndTypeNames.DSPACE_PROCESSID_TYPE;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspCatalogPropertyAndTypeNames.DSPACE_TRANSFER_COMPLETION_TYPE;


public class JsonObjectFromTransferCompletionMessageTransformer extends AbstractJsonLdTransformer<TransferCompletionMessage, JsonObject> {

    private final JsonBuilderFactory jsonBuilderFactory;

    private final ObjectMapper mapper;

    public JsonObjectFromTransferCompletionMessageTransformer(JsonBuilderFactory jsonBuilderFactory, ObjectMapper mapper) {
        super(TransferCompletionMessage.class, JsonObject.class);
        this.jsonBuilderFactory = jsonBuilderFactory;
        this.mapper = mapper;
    }

    @Override
    public @Nullable JsonObject transform(@Nullable TransferCompletionMessage transferCompletionMessage, @NotNull TransformerContext context) {
        if (transferCompletionMessage == null) {
            return null;
        }

        var builder = jsonBuilderFactory.createObjectBuilder();

        builder.add(ID, String.valueOf(UUID.randomUUID()));
        builder.add(TYPE, DSPACE_TRANSFER_COMPLETION_TYPE);
        builder.add(DSPACE_PROCESSID_TYPE, transferCompletionMessage.getProcessId());

        return builder.build();
    }
}
