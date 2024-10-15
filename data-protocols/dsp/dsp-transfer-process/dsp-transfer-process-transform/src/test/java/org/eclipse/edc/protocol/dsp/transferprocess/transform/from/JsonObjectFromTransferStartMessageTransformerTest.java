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

package org.eclipse.edc.protocol.dsp.transferprocess.transform.from;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferStartMessage;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.protocol.dsp.transferprocess.transform.type.from.JsonObjectFromTransferStartMessageTransformer;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CONSUMER_PID;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_PROVIDER_PID;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_PROPERTY_DATA_ADDRESS;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_START_MESSAGE_IRI;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JsonObjectFromTransferStartMessageTransformerTest {

    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private final TransformerContext context = mock();

    private final JsonObjectFromTransferStartMessageTransformer transformer =
            new JsonObjectFromTransferStartMessageTransformer(jsonFactory);

    @Test
    void transformTransferStartMessage() {
        var dataAddress = DataAddress.Builder.newInstance().type("type").build();
        var message = TransferStartMessage.Builder.newInstance()
                .consumerPid("consumerPid")
                .providerPid("providerPid")
                .protocol("testProtocol")
                .dataAddress(dataAddress)
                .build();

        var dataAddressJson = jsonFactory.createObjectBuilder().build();
        when(context.transform(dataAddress, JsonObject.class)).thenReturn(dataAddressJson);

        var result = transformer.transform(message, context);

        assertThat(result).isNotNull();
        assertThat(result.getString(JsonLdKeywords.TYPE)).isEqualTo(DSPACE_TYPE_TRANSFER_START_MESSAGE_IRI);
        assertThat(result.getString(DSPACE_PROPERTY_PROVIDER_PID)).isEqualTo("providerPid");
        assertThat(result.getString(DSPACE_PROPERTY_CONSUMER_PID)).isEqualTo("consumerPid");
        assertThat(result.getJsonObject(DSPACE_PROPERTY_DATA_ADDRESS)).isEqualTo(dataAddressJson);

        verify(context, times(1)).transform(dataAddress, JsonObject.class);
        verify(context, never()).reportProblem(anyString());
    }
}
