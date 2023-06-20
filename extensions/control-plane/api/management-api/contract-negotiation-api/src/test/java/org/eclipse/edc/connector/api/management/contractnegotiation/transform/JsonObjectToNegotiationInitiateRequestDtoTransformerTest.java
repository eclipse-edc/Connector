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

package org.eclipse.edc.connector.api.management.contractnegotiation.transform;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractOfferDescription;
import org.eclipse.edc.connector.api.management.contractnegotiation.model.NegotiationInitiateRequestDto;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractOfferDescription.ASSET_ID;
import static org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractOfferDescription.OFFER_ID;
import static org.eclipse.edc.connector.api.management.contractnegotiation.model.ContractOfferDescription.POLICY;
import static org.eclipse.edc.connector.api.management.contractnegotiation.model.NegotiationInitiateRequestDto.CALLBACK_ADDRESSES;
import static org.eclipse.edc.connector.api.management.contractnegotiation.model.NegotiationInitiateRequestDto.CONNECTOR_ADDRESS;
import static org.eclipse.edc.connector.api.management.contractnegotiation.model.NegotiationInitiateRequestDto.CONNECTOR_ID;
import static org.eclipse.edc.connector.api.management.contractnegotiation.model.NegotiationInitiateRequestDto.CONSUMER_ID;
import static org.eclipse.edc.connector.api.management.contractnegotiation.model.NegotiationInitiateRequestDto.OFFER;
import static org.eclipse.edc.connector.api.management.contractnegotiation.model.NegotiationInitiateRequestDto.PROTOCOL;
import static org.eclipse.edc.connector.api.management.contractnegotiation.model.NegotiationInitiateRequestDto.PROVIDER_ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_OBLIGATION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_PERMISSION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_POLICY_TYPE_SET;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_PROHIBITION_ATTRIBUTE;
import static org.eclipse.edc.spi.types.domain.callback.CallbackAddress.EVENTS;
import static org.eclipse.edc.spi.types.domain.callback.CallbackAddress.IS_TRANSACTIONAL;
import static org.eclipse.edc.spi.types.domain.callback.CallbackAddress.URI;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsonObjectToNegotiationInitiateRequestDtoTransformerTest {

    private final JsonLd jsonLd = new TitaniumJsonLd(mock(Monitor.class));
    private JsonObjectToNegotiationInitiateRequestDtoTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectToNegotiationInitiateRequestDtoTransformer();
    }

    @Test
    void transform() {
        var jsonObject = Json.createObjectBuilder()
                .add(TYPE, NegotiationInitiateRequestDto.TYPE)
                .add(CONNECTOR_ADDRESS, "test-address")
                .add(PROTOCOL, "test-protocol")
                .add(CONNECTOR_ID, "test-conn-id")
                .add(PROVIDER_ID, "test-provider-id")
                .add(CONSUMER_ID, "test-consumer-id")
                .add(CALLBACK_ADDRESSES, createCallbackAddress())
                .add(OFFER, Json.createObjectBuilder()
                        .add(OFFER_ID, "test-offer-id")
                        .add(ASSET_ID, "test-asset")
                        .add(POLICY, createPolicy())
                        .build())
                .build();

        var context = mock(TransformerContext.class);
        when(context.transform(any(JsonValue.class), eq(ContractOfferDescription.class))).thenReturn(ContractOfferDescription.Builder.newInstance().build());

        when(context.transform(any(JsonObject.class), eq(CallbackAddress.class))).thenReturn(CallbackAddress.Builder.newInstance()
                .uri("http://test.local")
                .events(Set.of("foo", "bar"))
                .transactional(true)
                .build());
        
        var dto = transformer.transform(jsonLd.expand(jsonObject).getContent(), context);

        assertThat(dto).isNotNull();
        assertThat(dto.getCallbackAddresses()).isNotEmpty();
        assertThat(dto.getProtocol()).isEqualTo("test-protocol");
        assertThat(dto.getConnectorAddress()).isEqualTo("test-address");
        assertThat(dto.getConnectorId()).isEqualTo("test-conn-id");
        assertThat(dto.getProviderId()).isEqualTo("test-provider-id");
        assertThat(dto.getConsumerId()).isEqualTo("test-consumer-id");
        assertThat(dto.getOffer()).isNotNull();

    }

    private JsonArrayBuilder createCallbackAddress() {
        var builder = Json.createArrayBuilder();
        return builder.add(Json.createObjectBuilder()
                .add(IS_TRANSACTIONAL, true)
                .add(URI, "http://test.local/")
                .add(EVENTS, Json.createArrayBuilder().build()));
    }

    private JsonObject createPolicy() {
        var permissionJson = getJsonObject("permission");
        var prohibitionJson = getJsonObject("prohibition");
        var dutyJson = getJsonObject("duty");
        return Json.createObjectBuilder()
                .add(TYPE, ODRL_POLICY_TYPE_SET)
                .add(ODRL_PERMISSION_ATTRIBUTE, permissionJson)
                .add(ODRL_PROHIBITION_ATTRIBUTE, prohibitionJson)
                .add(ODRL_OBLIGATION_ATTRIBUTE, dutyJson)
                .build();
    }

    private JsonObject getJsonObject(String type) {
        return Json.createObjectBuilder()
                .add(TYPE, type)
                .build();
    }
}
