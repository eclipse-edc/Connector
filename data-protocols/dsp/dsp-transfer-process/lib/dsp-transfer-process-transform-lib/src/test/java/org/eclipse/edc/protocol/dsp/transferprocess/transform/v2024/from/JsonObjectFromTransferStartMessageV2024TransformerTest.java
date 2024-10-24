/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.transferprocess.transform.v2024.from;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferStartMessage;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;
import org.eclipse.edc.protocol.dsp.transferprocess.transform.type.v2024.from.JsonObjectFromTransferStartMessageV2024Transformer;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CONSUMER_PID_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_PROVIDER_PID_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_PROPERTY_DATA_ADDRESS_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_START_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.transferprocess.transform.v2024.from.TestFunctionV2024.toIri;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JsonObjectFromTransferStartMessageV2024TransformerTest {

    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private final TransformerContext context = mock();

    private final JsonObjectFromTransferStartMessageV2024Transformer transformer =
            new JsonObjectFromTransferStartMessageV2024Transformer(jsonFactory);

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
        assertThat(result.getString(JsonLdKeywords.TYPE)).isEqualTo(toIri(DSPACE_TYPE_TRANSFER_START_MESSAGE_TERM));
        assertThat(result.getJsonObject(toIri(DSPACE_PROPERTY_PROVIDER_PID_TERM)).getString(ID)).isEqualTo("providerPid");
        assertThat(result.getJsonObject(toIri(DSPACE_PROPERTY_CONSUMER_PID_TERM)).getString(ID)).isEqualTo("consumerPid");
        assertThat(result.getJsonObject(toIri(DSPACE_PROPERTY_DATA_ADDRESS_TERM))).isEqualTo(dataAddressJson);

        verify(context, times(1)).transform(dataAddress, JsonObject.class);
        verify(context, never()).reportProblem(anyString());
    }
}
