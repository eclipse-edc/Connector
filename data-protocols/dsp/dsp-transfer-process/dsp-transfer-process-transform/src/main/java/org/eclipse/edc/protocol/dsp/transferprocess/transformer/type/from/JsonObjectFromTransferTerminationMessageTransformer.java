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
import org.eclipse.edc.jsonld.spi.transformer.AbstractJsonLdTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.dsp.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CODE;
import static org.eclipse.edc.protocol.dsp.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_PROCESS_ID;
import static org.eclipse.edc.protocol.dsp.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_REASON;
import static org.eclipse.edc.protocol.dsp.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_TERMINATION_MESSAGE;

public class JsonObjectFromTransferTerminationMessageTransformer extends AbstractJsonLdTransformer<TransferTerminationMessage, JsonObject> {

    private final JsonBuilderFactory jsonBuilderFactory;

    public JsonObjectFromTransferTerminationMessageTransformer(JsonBuilderFactory jsonBuilderFactory) {
        super(TransferTerminationMessage.class, JsonObject.class);
        this.jsonBuilderFactory = jsonBuilderFactory;
    }

    @Override
    public @Nullable JsonObject transform(@NotNull TransferTerminationMessage transferTerminationMessage, @NotNull TransformerContext context) {
        var builder = jsonBuilderFactory.createObjectBuilder();

        builder.add(ID, transferTerminationMessage.getId());
        builder.add(TYPE, DSPACE_TYPE_TRANSFER_TERMINATION_MESSAGE);
        builder.add(DSPACE_PROPERTY_PROCESS_ID, transferTerminationMessage.getProcessId());
        if (transferTerminationMessage.getCode() != null) {
            builder.add(DSPACE_PROPERTY_CODE, transferTerminationMessage.getCode());
        }
        if (transferTerminationMessage.getReason() != null) {
            builder.add(DSPACE_PROPERTY_REASON, transferTerminationMessage.getReason());
        }

        return builder.build();

    }
}
