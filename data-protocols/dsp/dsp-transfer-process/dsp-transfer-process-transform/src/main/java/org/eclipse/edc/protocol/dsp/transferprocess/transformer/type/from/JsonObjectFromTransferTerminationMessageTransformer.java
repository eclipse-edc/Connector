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

import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferTerminationMessage;
import org.eclipse.edc.jsonld.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

import static org.eclipse.edc.jsonld.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspCatalogPropertyAndTypeNames.DSPACE_PROCESSID_TYPE;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspCatalogPropertyAndTypeNames.DSPACE_TRANSFER_TERMINATION_TYPE;

public class JsonObjectFromTransferTerminationMessageTransformer extends AbstractJsonLdTransformer<TransferTerminationMessage, JsonObject> {

    private final JsonBuilderFactory jsonBuilderFactory;

    public JsonObjectFromTransferTerminationMessageTransformer(JsonBuilderFactory jsonBuilderFactory) {
        super(TransferTerminationMessage.class, JsonObject.class);
        this.jsonBuilderFactory = jsonBuilderFactory;
    }
    
    @Override
    public @Nullable JsonObject transform(@NotNull TransferTerminationMessage transferTerminationMessage, @NotNull TransformerContext context) {
        var builder = jsonBuilderFactory.createObjectBuilder();

        builder.add(ID, String.valueOf(UUID.randomUUID()));
        builder.add(TYPE, DSPACE_TRANSFER_TERMINATION_TYPE);
        builder.add(DSPACE_PROCESSID_TYPE, transferTerminationMessage.getProcessId());

        //TODO Add field when Message is evolved (code, reason)

        return builder.build();

    }
}
