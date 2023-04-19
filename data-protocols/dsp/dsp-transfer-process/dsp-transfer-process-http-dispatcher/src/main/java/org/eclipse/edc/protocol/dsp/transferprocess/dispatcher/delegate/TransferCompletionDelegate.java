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
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferCompletionMessage;
import org.eclipse.edc.jsonld.transformer.JsonLdTransformerRegistry;
import org.eclipse.edc.protocol.dsp.spi.dispatcher.DspHttpDispatcherDelegate;
import org.eclipse.edc.spi.EdcException;

import java.util.function.Function;

import static org.eclipse.edc.protocol.dsp.transferprocess.dispatcher.DelegateUtil.toCompactedJson;
import static org.eclipse.edc.protocol.dsp.transferprocess.spi.TransferProcessApiPaths.BASE_PATH;
import static org.eclipse.edc.protocol.dsp.transferprocess.spi.TransferProcessApiPaths.TRANSFER_COMPLETION;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspCatalogPropertyAndTypeNames.DCT_PREFIX;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspCatalogPropertyAndTypeNames.DCT_SCHEMA;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspCatalogPropertyAndTypeNames.DSPACE_PREFIX;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspCatalogPropertyAndTypeNames.DSPACE_SCHEMA;

public class TransferCompletionDelegate implements DspHttpDispatcherDelegate<TransferCompletionMessage, JsonObject> {

    private final ObjectMapper mapper;

    private final JsonLdTransformerRegistry registry;

    public TransferCompletionDelegate(ObjectMapper mapper, JsonLdTransformerRegistry registry) {
        this.mapper = mapper;
        this.registry = registry;
    }

    @Override
    public Class<TransferCompletionMessage> getMessageType() {
        return TransferCompletionMessage.class;
    }

    @Override
    public Request buildRequest(TransferCompletionMessage message) {
        var completion = registry.transform(message, JsonObject.class);

        if (completion.failed()) {
            throw new EdcException("Failed to create request body for transfer completion message");
        }
        
        var requestBody = RequestBody.create(toCompactedJson(completion.getContent(), jsonLdContext(), mapper),
                MediaType.get(jakarta.ws.rs.core.MediaType.APPLICATION_JSON));

        return new Request.Builder()
                .url(message.getCallbackAddress() + BASE_PATH + message.getProcessId() + TRANSFER_COMPLETION)
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
