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

package org.eclipse.edc.connector.controlplane.transform.edc.transferprocess.to;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferRequest;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.ProblemBuilder;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferRequest.TRANSFER_REQUEST_CONTRACT_ID;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferRequest.TRANSFER_REQUEST_COUNTER_PARTY_ADDRESS;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferRequest.TRANSFER_REQUEST_DATA_DESTINATION;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferRequest.TRANSFER_REQUEST_PRIVATE_PROPERTIES;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferRequest.TRANSFER_REQUEST_PROTOCOL;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferRequest.TRANSFER_REQUEST_TRANSFER_TYPE;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferRequest.TRANSFER_REQUEST_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JsonObjectToTransferRequestTransformerTest {

    private final JsonObjectToTransferRequestTransformer transformer = new JsonObjectToTransferRequestTransformer();
    private final TransformerContext context = mock(TransformerContext.class);

    @Test
    void types() {
        assertThat(transformer.getInputType()).isEqualTo(JsonObject.class);
        assertThat(transformer.getOutputType()).isEqualTo(TransferRequest.class);
    }

    @Test
    void transform() {
        var dataDestinationJson = Json.createObjectBuilder().build();
        var privatePropertiesJson = Json.createObjectBuilder().add("fooPrivate", "bar").build();
        var dataDestination = DataAddress.Builder.newInstance().type("type").build();
        var privateProperties = Map.of("fooPrivate", "bar");

        when(context.transform(any(), eq(DataAddress.class))).thenReturn(dataDestination);

        var json = Json.createObjectBuilder()
                .add(TYPE, TRANSFER_REQUEST_TYPE)
                .add(ID, "id")
                .add(TRANSFER_REQUEST_COUNTER_PARTY_ADDRESS, "address")
                .add(TRANSFER_REQUEST_CONTRACT_ID, "contractId")
                .add(TRANSFER_REQUEST_DATA_DESTINATION, dataDestinationJson)
                .add(TRANSFER_REQUEST_PRIVATE_PROPERTIES, privatePropertiesJson)
                .add(TRANSFER_REQUEST_TRANSFER_TYPE, "Http-Pull")
                .add(TRANSFER_REQUEST_PROTOCOL, "protocol")
                .build();

        var result = transformer.transform(json, context);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("id");
        assertThat(result.getCounterPartyAddress()).isEqualTo("address");
        assertThat(result.getContractId()).isEqualTo("contractId");
        assertThat(result.getDataDestination()).isSameAs(dataDestination);
        assertThat(result.getPrivateProperties()).containsAllEntriesOf(privateProperties);
        assertThat(result.getProtocol()).isEqualTo("protocol");
        assertThat(result.getTransferType()).isEqualTo("Http-Pull");
    }

    @Test
    void transform_error() {

        when(context.problem()).thenReturn(new ProblemBuilder(context));

        var json = Json.createObjectBuilder()
                .add(TYPE, TRANSFER_REQUEST_TYPE)
                .add(ID, "id")
                .add(TRANSFER_REQUEST_PRIVATE_PROPERTIES, 1)
                .build();

        var result = transformer.transform(json, context);

        assertThat(result).isNotNull();

        verify(context, times(1)).problem();
    }
}
