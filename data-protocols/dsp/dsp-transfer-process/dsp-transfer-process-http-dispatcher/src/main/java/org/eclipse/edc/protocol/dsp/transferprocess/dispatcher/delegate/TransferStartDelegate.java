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

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferStartMessage;
import org.eclipse.edc.jsonld.transformer.JsonLdTransformerRegistry;
import org.eclipse.edc.protocol.dsp.spi.dispatcher.DspHttpDispatcherDelegate;
import org.eclipse.edc.spi.EdcException;

import java.util.function.Function;

import static org.eclipse.edc.protocol.dsp.transferprocess.dispatcher.DelegateUtil.toCompactedJson;
import static org.eclipse.edc.protocol.dsp.transferprocess.spi.TransferProcessApiPaths.BASE_PATH;
import static org.eclipse.edc.protocol.dsp.transferprocess.spi.TransferProcessApiPaths.TRANSFER_START;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspTransferProcessPropertyAndTypeNames.DSPACE_PREFIX;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspTransferProcessPropertyAndTypeNames.DSPACE_SCHEMA;
import static org.eclipse.edc.protocol.dsp.transform.transformer.Namespaces.DCT_PREFIX;
import static org.eclipse.edc.protocol.dsp.transform.transformer.Namespaces.DCT_SCHEMA;

public class TransferStartDelegate implements DspHttpDispatcherDelegate<TransferStartMessage, JsonObject> {

    private final ObjectMapper mapper;

    private final JsonLdTransformerRegistry registry;

    public TransferStartDelegate(ObjectMapper mapper, JsonLdTransformerRegistry registry) {
        this.mapper = mapper;
        this.registry = registry;
    }

    @Override
    public Class<TransferStartMessage> getMessageType() {
        return TransferStartMessage.class;
    }

    @Override
    public Request buildRequest(TransferStartMessage message) {
        var start = registry.transform(message, JsonObject.class);
        if (start.failed()) {
            throw new EdcException("Failed to create request body for transfer start message.");
        }
    
        var requestBody = RequestBody.create(toCompactedJson(start.getContent(), jsonLdContext(), mapper),
                MediaType.get(jakarta.ws.rs.core.MediaType.APPLICATION_JSON));

        return new Request.Builder()
                .url(message.getCallbackAddress() + BASE_PATH + message.getProcessId() + TRANSFER_START)
                .post(requestBody)
                .build();
    }

    @Override
    public Function<Response, JsonObject> parseResponse() {
        return response -> null;
    }

    private JsonObject jsonLdContext() {
        return Json.createObjectBuilder()
                .add(DCT_PREFIX, DCT_SCHEMA)
                .add(DSPACE_PREFIX, DSPACE_SCHEMA)
                .build();
    }
}
