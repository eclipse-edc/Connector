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
import jakarta.json.JsonObject;
import okhttp3.Request;
import okhttp3.Response;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferRequestMessage;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.protocol.dsp.spi.dispatcher.DspHttpDispatcherDelegate;
import org.eclipse.edc.protocol.dsp.spi.serialization.JsonLdRemoteMessageSerializer;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

import java.io.IOException;
import java.util.function.Function;

import static org.eclipse.edc.protocol.dsp.transferprocess.dispatcher.TransferProcessApiPaths.BASE_PATH;
import static org.eclipse.edc.protocol.dsp.transferprocess.dispatcher.TransferProcessApiPaths.TRANSFER_INITIAL_REQUEST;

public class TransferRequestDelegate extends DspHttpDispatcherDelegate<TransferRequestMessage, TransferProcess> {

    private final ObjectMapper mapper;
    private final TypeTransformerRegistry registry;
    private final JsonLd jsonLdService;

    public TransferRequestDelegate(JsonLdRemoteMessageSerializer serializer, ObjectMapper mapper, TypeTransformerRegistry registry, JsonLd jsonLdService) {
        super(serializer);
        this.mapper = mapper;
        this.registry = registry;
        this.jsonLdService = jsonLdService;
    }

    @Override
    public Class<TransferRequestMessage> getMessageType() {
        return TransferRequestMessage.class;
    }

    @Override
    public Request buildRequest(TransferRequestMessage message) {
        return buildRequest(message, BASE_PATH + TRANSFER_INITIAL_REQUEST);
    }

    @Override
    public Function<Response, TransferProcess> parseResponse() {
        return response -> {
            try {
                var jsonObject = mapper.readValue(response.body().bytes(), JsonObject.class);
                var expansionResult = jsonLdService.expand(jsonObject);
                var tp = expansionResult
                        .map(jo -> registry.transform(jo, TransferProcess.class))
                        .orElseThrow(f -> new EdcException("Failed to read response body from transfer request: " + f.getFailureDetail()));
                return tp.orElseThrow(f -> new EdcException("Failed to transform response body from transfer request: " + f.getFailureDetail()));
            } catch (RuntimeException | IOException e) {
                throw new EdcException("Failed to read response body from contract request.", e);
            }
        };
    }

}
