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

package org.eclipse.edc.protocol.dsp.controlplane.delegate;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractOfferRequest;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.jsonld.transformer.JsonLdKeywords;
import org.eclipse.edc.jsonld.transformer.JsonLdTransformerRegistry;
import org.eclipse.edc.jsonld.transformer.JsonLdTransformerRegistryImpl;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.Map;

import static org.eclipse.edc.protocol.dsp.transform.DspNamespaces.DSPACE_SCHEMA;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ContractRequestDelegateTest {

    private ContractRequestDelegate delegate;

    private JsonLdTransformerRegistry registry;

    private JsonBuilderFactory factory;

    @BeforeEach
    void setUp() {
        registry = new JsonLdTransformerRegistryImpl();
        delegate = new ContractRequestDelegate(new ObjectMapper(), registry);
        factory = Json.createBuilderFactory(Map.of());
    }

    @Test
    void getMessageType() {
        Assertions.assertEquals(ContractOfferRequest.class, delegate.getMessageType());
    }

    @Test
    void buildRequest() {
        var message = buildMessage(ContractOfferRequest.Type.INITIAL);
        when(registry.transform(message, any())).thenReturn(Result.success(getJson()));

        var result = delegate.buildRequest(message);

        Assertions.assertNotNull(result);

    }

    @Test
    void parseResponse() {
    }

    private ContractOfferRequest buildMessage(ContractOfferRequest.Type type) {
        return ContractOfferRequest.Builder.newInstance()
                .correlationId("correlationId")
                .type(type)
                .protocol("protocol")
                .connectorAddress("address")
                .connectorId("connectorId")
                .contractOffer(buildContractOffer())
                .build();
    }

    private ContractOffer buildContractOffer() {
        return ContractOffer.Builder.newInstance()
                .id("offerId")
                .contractEnd(ZonedDateTime.now())
                .contractStart(ZonedDateTime.now())
                .asset(Asset.Builder.newInstance().build())
                .policy(Policy.Builder.newInstance().build())
                .build();
    }

    private JsonObject getJson() {
        var builder = factory.createObjectBuilder();
        builder.add(JsonLdKeywords.ID, "negotiationId");
        builder.add(JsonLdKeywords.TYPE, DSPACE_SCHEMA + "ContractNegotiation");

        builder.add(DSPACE_SCHEMA + "correlationId", "correlationId");
        builder.add(DSPACE_SCHEMA + "state", ContractNegotiationStates.CONSUMER_REQUESTED.name());

        return builder.build();
    }
}