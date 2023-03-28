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

package org.eclipse.edc.protocol.dsp.transform.type;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.document.JsonDocument;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsonp.JSONPModule;
import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractOfferRequest;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.jsonld.transformer.JsonLdKeywords;
import org.eclipse.edc.jsonld.transformer.JsonLdTransformerRegistryImpl;
import org.eclipse.edc.jsonld.transformer.Namespaces;
import org.eclipse.edc.jsonld.transformer.from.JsonObjectFromCatalogTransformer;
import org.eclipse.edc.jsonld.transformer.from.JsonObjectFromDataServiceTransformer;
import org.eclipse.edc.jsonld.transformer.from.JsonObjectFromDatasetTransformer;
import org.eclipse.edc.jsonld.transformer.from.JsonObjectFromDistributionTransformer;
import org.eclipse.edc.jsonld.transformer.from.JsonObjectFromPolicyTransformer;
import org.eclipse.edc.jsonld.transformer.to.JsonValueToGenericTypeTransformer;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.protocol.dsp.spi.controlplane.type.ContractNegotiationError;
import org.eclipse.edc.protocol.dsp.transform.DspNamespaces;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static org.eclipse.edc.protocol.dsp.transform.DspNamespaces.DSPACE_SCHEMA;

class JsonObjectFromTransformerTest {

    private JsonLdTransformerRegistryImpl registry;

    private JsonDocument contextDocument;

    private JsonBuilderFactory builderFactory;

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JSONPModule());
        var module = new SimpleModule() {
            @Override
            public void setupModule(SetupContext context) {
                super.setupModule(context);
            }
        };
        mapper.registerModule(module);

        var builderFactory = Json.createBuilderFactory(Map.of());
        var contextObject = builderFactory
                .createObjectBuilder()
                .add(Namespaces.DCAT_PREFIX, Namespaces.DCAT_SCHEMA)
                .add(Namespaces.ODRL_PREFIX, Namespaces.ODRL_SCHEMA)
                .add(Namespaces.DCT_PREFIX, Namespaces.DCT_SCHEMA)
                .add(DspNamespaces.DSPACE_PREFIX, DspNamespaces.DSPACE_SCHEMA)
                .build();
        contextDocument = JsonDocument.of(builderFactory.createObjectBuilder()
                .add("@context", contextObject)
                .build());

        registry = new JsonLdTransformerRegistryImpl();

        // EDC model to JSON-LD transformers
        registry.register(new JsonObjectFromCatalogTransformer(builderFactory, mapper));
        registry.register(new JsonObjectFromDatasetTransformer(builderFactory, mapper));
        registry.register(new JsonObjectFromPolicyTransformer(builderFactory, mapper));
        registry.register(new JsonObjectFromDistributionTransformer(builderFactory, mapper));
        registry.register(new JsonObjectFromDataServiceTransformer(builderFactory, mapper));
        registry.register(new JsonValueToGenericTypeTransformer(mapper));

        // DSP-specific transformers
        registry.register(new JsonObjectFromContractRequestTransformer(builderFactory, mapper));
        registry.register(new JsonObjectFromContractNegotiationTransformer(builderFactory));
        registry.register(new JsonObjectFromContractNegotiationErrorTransformer(builderFactory, mapper));
    }

    @Test
    void transformContractNegotiation() throws JsonLdError {
        var negotiation = ContractNegotiation.Builder.newInstance()
                .id("negotiationId")
                .correlationId("correlationId")
                .counterPartyId("counterPartyId")
                .counterPartyAddress("counterPartyAddress")
                .protocol("protocol")
                .state(ContractNegotiationStates.CONSUMER_REQUESTED.code())
                .build();

        var result = registry.transform(negotiation, JsonObject.class);

        Assertions.assertNotNull(result);

        var document = JsonDocument.of(result.getContent());
        var obj = JsonLd.compact(document, contextDocument).get();

        Assertions.assertNotNull(obj);
    }

    @Test
    void transformContractRequest() throws JsonLdError {
        var message = ContractOfferRequest.Builder.newInstance()
                .correlationId("correlationId")
                .type(ContractOfferRequest.Type.INITIAL)
                .protocol("protocol")
                .connectorAddress("address")
                .connectorId("connectorId")
                .contractOffer(buildContractOffer())
                .build();

        var result = registry.transform(message, JsonObject.class);

        Assertions.assertNotNull(result);

        var document = JsonDocument.of(result.getContent());
        var obj = JsonLd.compact(document, contextDocument).get();

        Assertions.assertNotNull(obj);
    }

    @Test
    void transformContractNegotiationError() throws JsonLdError, JsonProcessingException {
        var error = ContractNegotiationError.Builder.newInstance()
                .code("400")
                .processId("processId")
                .reasons(List.of("reason1", "reason2"))
                .build();

        var result = registry.transform(error, JsonObject.class);

        Assertions.assertNotNull(result);

        var document = JsonDocument.of(result.getContent());
        var obj = JsonLd.compact(document, contextDocument).get();
        var s = mapper.writeValueAsString(obj);

        Assertions.assertNotNull(obj);

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
        var builder = builderFactory.createObjectBuilder();
        builder.add(JsonLdKeywords.ID, "negotiationId");
        builder.add(JsonLdKeywords.TYPE, DSPACE_SCHEMA + "ContractNegotiation");

        builder.add(DSPACE_SCHEMA + "correlationId", "correlationId");
        builder.add(DSPACE_SCHEMA + "state", ContractNegotiationStates.CONSUMER_REQUESTED.name());

        return builder.build();
    }
}