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
import okhttp3.Request;
import okhttp3.Response;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferRequestMessage;
import org.eclipse.edc.protocol.dsp.spi.dispatcher.DspHttpDispatcherDelegate;
import org.eclipse.edc.protocol.dsp.spi.serialization.JsonLdRemoteMessageSerializer;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

import java.io.IOException;
import java.util.function.Function;

import static org.eclipse.edc.jsonld.spi.Namespaces.ODRL_PREFIX;
import static org.eclipse.edc.jsonld.spi.Namespaces.ODRL_SCHEMA;
import static org.eclipse.edc.jsonld.util.JsonLdUtil.expand;
import static org.eclipse.edc.protocol.dsp.transferprocess.dispatcher.TransferProcessApiPaths.BASE_PATH;
import static org.eclipse.edc.protocol.dsp.transferprocess.dispatcher.TransferProcessApiPaths.TRANSFER_INITIAL_REQUEST;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspTransferProcessPropertyAndTypeNames.DSPACE_PREFIX;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspTransferProcessPropertyAndTypeNames.DSPACE_SCHEMA;

public class TransferRequestDelegate extends DspHttpDispatcherDelegate<TransferRequestMessage, TransferProcess> {

    private ObjectMapper mapper;

    private TypeTransformerRegistry registry;

    public TransferRequestDelegate(JsonLdRemoteMessageSerializer serializer, ObjectMapper mapper, TypeTransformerRegistry registry) {
        super(serializer);
        this.mapper = mapper;
        this.registry = registry;
    }

    @Override
    public Class<TransferRequestMessage> getMessageType() {
        return TransferRequestMessage.class;
    }

    @Override
    public Request buildRequest(TransferRequestMessage message) {
        return buildRequest(message, BASE_PATH + TRANSFER_INITIAL_REQUEST, jsonLdContext());
    }

    @Override
    public Function<Response, TransferProcess> parseResponse() {
        return response -> {
            try {
                var jsonObject = mapper.readValue(response.body().bytes(), JsonObject.class);
                var result = registry.transform(expand(jsonObject).get(0), TransferProcess.class);

                if (result.succeeded()) {
                    return result.getContent();
                } else {
                    throw new EdcException("Failed to read response body from transfer request");
                }

            } catch (RuntimeException | IOException e) {
                throw new EdcException("Failed to read response body from contract request.", e);
            }
        };
    }

    // TODO refactor according to https://github.com/eclipse-edc/Connector/issues/2763
    private JsonObject jsonLdContext() {
        return Json.createObjectBuilder()
                .add(DSPACE_PREFIX, DSPACE_SCHEMA)
                .add(ODRL_PREFIX, ODRL_SCHEMA)
                .build();
    }
}
