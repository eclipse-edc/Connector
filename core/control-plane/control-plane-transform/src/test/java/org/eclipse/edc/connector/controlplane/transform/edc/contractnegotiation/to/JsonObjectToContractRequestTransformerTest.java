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

package org.eclipse.edc.connector.controlplane.transform.edc.contractnegotiation.to;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequest;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.callback.CallbackAddress;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequest.CALLBACK_ADDRESSES;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequest.CONTRACT_REQUEST_COUNTER_PARTY_ADDRESS;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequest.POLICY;
import static org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequest.PROTOCOL;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_ASSIGNER_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_OBLIGATION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_PERMISSION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_POLICY_TYPE_SET;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_PROHIBITION_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_TARGET_ATTRIBUTE;
import static org.eclipse.edc.spi.types.domain.callback.CallbackAddress.EVENTS;
import static org.eclipse.edc.spi.types.domain.callback.CallbackAddress.IS_TRANSACTIONAL;
import static org.eclipse.edc.spi.types.domain.callback.CallbackAddress.URI;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsonObjectToContractRequestTransformerTest {

    private final JsonLd jsonLd = new TitaniumJsonLd(mock(Monitor.class));
    private final TransformerContext context = mock();
    private JsonObjectToContractRequestTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectToContractRequestTransformer();
    }

    @Test
    void transform() {
        var jsonObject = Json.createObjectBuilder()
                .add(TYPE, ContractRequest.CONTRACT_REQUEST_TYPE)
                .add(CONTRACT_REQUEST_COUNTER_PARTY_ADDRESS, "test-address")
                .add(PROTOCOL, "test-protocol")
                .add(CALLBACK_ADDRESSES, createCallbackAddress())
                .add(POLICY, createPolicy())
                .build();
        when(context.transform(any(JsonObject.class), eq(CallbackAddress.class))).thenReturn(CallbackAddress.Builder.newInstance()
                .uri("http://test.local")
                .events(Set.of("foo", "bar"))
                .transactional(true)
                .build());
        var offer = createContractOffer("test-provider-id");
        when(context.transform(any(JsonValue.class), eq(ContractOffer.class))).thenReturn(offer);

        var request = transformer.transform(jsonLd.expand(jsonObject).getContent(), context);

        assertThat(request).isNotNull();
        assertThat(request.getProviderId()).isEqualTo("test-provider-id");
        assertThat(request.getCallbackAddresses()).isNotEmpty();
        assertThat(request.getProtocol()).isEqualTo("test-protocol");
        assertThat(request.getCounterPartyAddress()).isEqualTo("test-address");
        assertThat(request.getContractOffer()).isSameAs(offer);
    }

    @Test
    void transform_shouldSetProviderIdAsCounterPartyAddress_whenProviderIdNotDefined() {
        var jsonObject = Json.createObjectBuilder()
                .add(TYPE, ContractRequest.CONTRACT_REQUEST_TYPE)
                .add(CONTRACT_REQUEST_COUNTER_PARTY_ADDRESS, "test-address")
                .add(PROTOCOL, "test-protocol")
                .add(POLICY, createPolicy())
                .build();
        when(context.transform(any(JsonValue.class), eq(ContractOffer.class))).thenReturn(createContractOffer("test-address"));

        var request = transformer.transform(jsonLd.expand(jsonObject).getContent(), context);

        assertThat(request).isNotNull();
        assertThat(request.getProviderId()).isEqualTo("test-address");
    }

    private ContractOffer createContractOffer(String providerId) {
        var policy = Policy.Builder.newInstance().target("test-asset").assigner(providerId).build();
        return ContractOffer.Builder.newInstance().id("offer-id").assetId("asset-id").policy(policy).build();
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
                .add(ID, "test-offer-id")
                .add(ODRL_TARGET_ATTRIBUTE, "test-asset")
                .add(ODRL_PERMISSION_ATTRIBUTE, permissionJson)
                .add(ODRL_PROHIBITION_ATTRIBUTE, prohibitionJson)
                .add(ODRL_OBLIGATION_ATTRIBUTE, dutyJson)
                .add(ODRL_ASSIGNER_ATTRIBUTE, "test-provider-id")
                .build();
    }

    private JsonObject getJsonObject(String type) {
        return Json.createObjectBuilder()
                .add(TYPE, type)
                .build();
    }
}
