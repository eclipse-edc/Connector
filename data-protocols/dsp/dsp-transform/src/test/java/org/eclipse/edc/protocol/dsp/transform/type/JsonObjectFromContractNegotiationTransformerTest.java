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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsonp.JSONPModule;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.jsonld.transformer.JsonLdTransformerRegistryImpl;
import org.eclipse.edc.jsonld.transformer.Namespaces;
import org.eclipse.edc.protocol.dsp.transform.DspNamespaces;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

class JsonObjectFromContractNegotiationTransformerTest {

    private JsonLdTransformerRegistryImpl registry;

    private JsonDocument contextDocument;

    @BeforeEach
    void setUp() {
        var mapper = new ObjectMapper();
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

        var transformer = new JsonObjectFromContractNegotiationTransformer(builderFactory, mapper);

        registry = new JsonLdTransformerRegistryImpl();
        registry.register(transformer);
    }

    @Test
    void transform() throws JsonLdError {
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
}