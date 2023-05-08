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

package org.eclipse.edc.connector.api.management.transferprocess.transform;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.api.model.CallbackAddressDto;
import org.eclipse.edc.api.model.DataAddressDto;
import org.eclipse.edc.connector.api.management.transferprocess.model.DataRequestDto;
import org.eclipse.edc.connector.api.management.transferprocess.model.TransferProcessDto;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.api.management.transferprocess.model.TransferProcessDto.EDC_TRANSFER_PROCESS_DTO_CALLBACK_ADDRESSES;
import static org.eclipse.edc.connector.api.management.transferprocess.model.TransferProcessDto.EDC_TRANSFER_PROCESS_DTO_DATA_DESTINATION;
import static org.eclipse.edc.connector.api.management.transferprocess.model.TransferProcessDto.EDC_TRANSFER_PROCESS_DTO_DATA_REQUEST;
import static org.eclipse.edc.connector.api.management.transferprocess.model.TransferProcessDto.EDC_TRANSFER_PROCESS_DTO_ERROR_DETAIL;
import static org.eclipse.edc.connector.api.management.transferprocess.model.TransferProcessDto.EDC_TRANSFER_PROCESS_DTO_STATE;
import static org.eclipse.edc.connector.api.management.transferprocess.model.TransferProcessDto.EDC_TRANSFER_PROCESS_DTO_STATE_TIMESTAMP;
import static org.eclipse.edc.connector.api.management.transferprocess.model.TransferProcessDto.EDC_TRANSFER_PROCESS_DTO_TYPE;
import static org.eclipse.edc.connector.api.management.transferprocess.model.TransferProcessDto.EDC_TRANSFER_PROCESS_DTO_TYPE_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.util.JacksonJsonLd.createObjectMapper;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsonObjectFromTransferProcessDtoTransformerTest {

    private final JsonObjectFromTransferProcessDtoTransformer transformer = new JsonObjectFromTransferProcessDtoTransformer(Json.createBuilderFactory(emptyMap()), createObjectMapper());
    private final TransformerContext context = mock(TransformerContext.class);

    @Test
    void types() {
        assertThat(transformer.getInputType()).isEqualTo(TransferProcessDto.class);
        assertThat(transformer.getOutputType()).isEqualTo(JsonObject.class);
    }

    @Test
    void transform() {
        var dataDestinationJson = Json.createObjectBuilder().build();
        var dataRequestJson = Json.createObjectBuilder().build();
        var callbackAddresJson = Json.createObjectBuilder().build();
        when(context.transform(isA(DataAddressDto.class), any())).thenReturn(dataDestinationJson);
        when(context.transform(isA(DataRequestDto.class), any())).thenReturn(dataRequestJson);
        when(context.transform(isA(CallbackAddressDto.class), any())).thenReturn(callbackAddresJson);
        var input = TransferProcessDto.Builder.newInstance()
                .id("transferProcessId")
                .state("STATE")
                .stateTimestamp(1234L)
                .properties(Map.of("foo", "bar"))
                .type("CONSUMER")
                .dataDestination(DataAddressDto.Builder.newInstance().properties(Map.of("bar", "foo")).build())
                .dataRequest(DataRequestDto.Builder.newInstance().build())
                .callbackAddresses(List.of(CallbackAddressDto.Builder.newInstance().uri("http://any").events(emptySet()).build()))
                .errorDetail("an error")
                .build();

        var result = transformer.transform(input, context);

        assertThat(result).isNotNull();
        assertThat(result.getString(ID)).isEqualTo("transferProcessId");
        assertThat(result.getString(TYPE)).isEqualTo(EDC_TRANSFER_PROCESS_DTO_TYPE);
        assertThat(result.getString(EDC_TRANSFER_PROCESS_DTO_STATE)).isEqualTo("STATE");
        assertThat(result.getJsonNumber(EDC_TRANSFER_PROCESS_DTO_STATE_TIMESTAMP).longValue()).isEqualTo(1234L);
        assertThat(result.getString("foo")).isEqualTo("bar");
        assertThat(result.getString(EDC_TRANSFER_PROCESS_DTO_TYPE_TYPE)).isEqualTo("CONSUMER");
        assertThat(result.getJsonObject(EDC_TRANSFER_PROCESS_DTO_DATA_DESTINATION)).isSameAs(dataDestinationJson);
        assertThat(result.getJsonObject(EDC_TRANSFER_PROCESS_DTO_DATA_REQUEST)).isSameAs(dataRequestJson);
        assertThat(result.getJsonArray(EDC_TRANSFER_PROCESS_DTO_CALLBACK_ADDRESSES).get(0)).isSameAs(callbackAddresJson);
        assertThat(result.getString(EDC_TRANSFER_PROCESS_DTO_ERROR_DETAIL)).isEqualTo("an error");
    }

}
