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

package org.eclipse.edc.protocol.dsp.transferprocess.dispatcher.delegate;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import okhttp3.Request;
import okhttp3.Response;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferStartMessage;
import org.eclipse.edc.protocol.dsp.spi.dispatcher.DspHttpDispatcherDelegate;
import org.eclipse.edc.protocol.dsp.spi.serialization.JsonLdRemoteMessageSerializer;

import java.util.function.Function;

import static org.eclipse.edc.jsonld.spi.Namespaces.ODRL_PREFIX;
import static org.eclipse.edc.jsonld.spi.Namespaces.ODRL_SCHEMA;
import static org.eclipse.edc.protocol.dsp.transferprocess.spi.TransferProcessApiPaths.BASE_PATH;
import static org.eclipse.edc.protocol.dsp.transferprocess.spi.TransferProcessApiPaths.TRANSFER_START;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspTransferProcessPropertyAndTypeNames.DSPACE_PREFIX;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspTransferProcessPropertyAndTypeNames.DSPACE_SCHEMA;

public class TransferStartDelegate extends DspHttpDispatcherDelegate<TransferStartMessage, JsonObject> {

    public TransferStartDelegate(JsonLdRemoteMessageSerializer serializer) {
        super(serializer);
    }

    @Override
    public Class<TransferStartMessage> getMessageType() {
        return TransferStartMessage.class;
    }

    @Override
    public Request buildRequest(TransferStartMessage message) {
        return buildRequest(message, BASE_PATH + message.getProcessId() + TRANSFER_START, jsonLdContext());
    }

    @Override
    public Function<Response, JsonObject> parseResponse() {
        return response -> null;
    }

    // TODO refactor according to https://github.com/eclipse-edc/Connector/issues/2763
    private JsonObject jsonLdContext() {
        return Json.createObjectBuilder()
                .add(DSPACE_PREFIX, DSPACE_SCHEMA)
                .add(ODRL_PREFIX, ODRL_SCHEMA)
                .build();
    }
}
