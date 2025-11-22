/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.controlplane.transform.edc.transferprocess.from;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.DataplaneMetadata;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.TRANSFER_PROCESS_ASSET_ID;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.TRANSFER_PROCESS_CALLBACK_ADDRESSES;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.TRANSFER_PROCESS_CONTRACT_ID;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.TRANSFER_PROCESS_CORRELATION_ID;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.TRANSFER_PROCESS_DATAPLANE_METADATA;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.TRANSFER_PROCESS_DATA_DESTINATION;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.TRANSFER_PROCESS_ERROR_DETAIL;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.TRANSFER_PROCESS_STATE;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.TRANSFER_PROCESS_STATE_TIMESTAMP;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.TRANSFER_PROCESS_TRANSFER_TYPE;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.TRANSFER_PROCESS_TYPE;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.TRANSFER_PROCESS_TYPE_TYPE;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess.Type.CONSUMER;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsonObjectFromTransferProcessTransformerTest {

    private final JsonObjectFromTransferProcessTransformer transformer = new JsonObjectFromTransferProcessTransformer(Json.createBuilderFactory(emptyMap()));
    private final TransformerContext context = mock(TransformerContext.class);

    @Test
    void types() {
        assertThat(transformer.getInputType()).isEqualTo(TransferProcess.class);
        assertThat(transformer.getOutputType()).isEqualTo(JsonObject.class);
    }

    @Test
    void transform() {
        var dataDestinationJson = Json.createObjectBuilder().build();
        var callbackAddressJson = Json.createObjectBuilder().build();
        var dataplaneMetadataJson = Json.createObjectBuilder().build();
        when(context.transform(isA(DataAddress.class), any())).thenReturn(dataDestinationJson);
        when(context.transform(isA(CallbackAddress.class), any())).thenReturn(callbackAddressJson);
        when(context.transform(isA(DataplaneMetadata.class), any())).thenReturn(dataplaneMetadataJson);
        var input = TransferProcess.Builder.newInstance()
                .id("transferProcessId")
                .state(STARTED.code())
                .stateTimestamp(1234L)
                .privateProperties(Map.of("foo", "bar"))
                .transferType("transferType")
                .type(CONSUMER)
                .correlationId("correlationId")
                .assetId("assetId")
                .contractId("contractId")
                .dataDestination(DataAddress.Builder.newInstance().type("any").properties(Map.of("bar", "foo")).build())
                .callbackAddresses(List.of(CallbackAddress.Builder.newInstance().uri("http://any").events(emptySet()).build()))
                .errorDetail("an error")
                .dataplaneMetadata(DataplaneMetadata.Builder.newInstance().label("label").build())
                .build();

        var result = transformer.transform(input, context);

        assertThat(result).isNotNull();
        assertThat(result.getString(ID)).isEqualTo("transferProcessId");
        assertThat(result.getString(TYPE)).isEqualTo(TRANSFER_PROCESS_TYPE);
        assertThat(result.getString(TRANSFER_PROCESS_CORRELATION_ID)).isEqualTo("correlationId");
        assertThat(result.getString(TRANSFER_PROCESS_STATE)).isEqualTo(STARTED.name());
        assertThat(result.getJsonNumber(TRANSFER_PROCESS_STATE_TIMESTAMP).longValue()).isEqualTo(1234L);
        assertThat(result.getString(TRANSFER_PROCESS_ASSET_ID)).isEqualTo("assetId");
        assertThat(result.getString(TRANSFER_PROCESS_CONTRACT_ID)).isEqualTo("contractId");
        assertThat(result.getString(TRANSFER_PROCESS_TYPE_TYPE)).isEqualTo(CONSUMER.toString());
        assertThat(result.getString(TRANSFER_PROCESS_TRANSFER_TYPE)).isEqualTo("transferType");
        assertThat(result.getJsonObject(TRANSFER_PROCESS_DATA_DESTINATION)).isSameAs(dataDestinationJson);
        assertThat(result.getJsonArray(TRANSFER_PROCESS_CALLBACK_ADDRESSES).get(0)).isSameAs(callbackAddressJson);
        assertThat(result.getString(TRANSFER_PROCESS_ERROR_DETAIL)).isEqualTo("an error");
        assertThat(result.getJsonObject(TRANSFER_PROCESS_DATAPLANE_METADATA)).isSameAs(dataplaneMetadataJson);
    }

    @Test
    void shouldNotThrownException_whenBareboneTransferProcess() {
        var input = TransferProcess.Builder.newInstance()
                .build();

        var result = transformer.transform(input, context);

        assertThat(result).isNotNull();
    }
}
