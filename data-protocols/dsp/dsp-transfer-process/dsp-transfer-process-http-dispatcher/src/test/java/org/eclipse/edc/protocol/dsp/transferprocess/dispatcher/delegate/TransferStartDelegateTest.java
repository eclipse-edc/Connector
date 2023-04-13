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
import org.eclipse.edc.catalog.spi.CatalogRequest;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferStartMessage;
import org.eclipse.edc.jsonld.transformer.JsonLdTransformerRegistry;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.protocol.dsp.transferprocess.spi.TransferProcessApiPaths.BASE_PATH;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

public class TransferStartDelegateTest {

    private ObjectMapper mapper = mock(ObjectMapper.class);
    private JsonLdTransformerRegistry registry = mock(JsonLdTransformerRegistry.class);

    private TransferStartDelegate startDelegate;

    @BeforeEach
    void setUp() {
        startDelegate = new TransferStartDelegate(mapper, registry);
    }

    @Test
    void getMessageType_returnCatalogRequest() {
        assertThat(startDelegate.getMessageType()).isEqualTo(TransferStartMessage.class);
    }

    @Test
    void buildRequest_returnRequest() throws IOException {
        var jsonObject = getJsonObject();
        var serializedBody = "catalog request";

        when(registry.transform(isA(TransferStartMessage.class), eq(JsonObject.class)))
                .thenReturn(Result.success(jsonObject));
        when(mapper.writeValueAsString(jsonObject)).thenReturn(serializedBody);

        var message = getTransferStartMessage();
        var httpRequest = startDelegate.buildRequest(message);

        assertThat(httpRequest.url().url()).hasToString(message.getConnectorAddress() + BASE_PATH + message.getProcessId() + "/start");
//        assertThat(readRequestBody(httpRequest)).isEqualTo(serializedBody);
//
//        verify(registry, times(1))
//                .transform(argThat(requestMessage -> ((CatalogRequestMessage) requestMessage).getFilter().equals(message.getQuerySpec())), eq(JsonObject.class));
//        verify(mapper, times(1)).writeValueAsString(jsonObject);
    }

    private TransferStartMessage getTransferStartMessage() {
        return TransferStartMessage.Builder.newInstance()
                .processId("testId")
                .protocol("dataspace-protocol")
                .connectorAddress("testConnectorAddress")
                .build();
    }

    private JsonObject getJsonObject() {
        return Json.createObjectBuilder()
                .add(CONTEXT, Json.createObjectBuilder().add("prefix", "http://schema").build())
                .add("prefix:key", "value")
                .build();
    }

}
