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

package org.eclipse.edc.protocol.dsp.transferprocess.transformer;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.document.JsonDocument;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsonp.JSONPModule;
import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferCompletionMessage;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferRequestMessage;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferStartMessage;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferTerminationMessage;
import org.eclipse.edc.jsonld.JsonLdKeywords;
import org.eclipse.edc.jsonld.transformer.JsonLdTransformerRegistryImpl;
import org.eclipse.edc.protocol.dsp.transferprocess.transformer.type.from.JsonObjectFromTransferCompletionMessageTransformer;
import org.eclipse.edc.protocol.dsp.transferprocess.transformer.type.from.JsonObjectFromTransferProcessTransformer;
import org.eclipse.edc.protocol.dsp.transferprocess.transformer.type.from.JsonObjectFromTransferRequestMessageTransformer;
import org.eclipse.edc.protocol.dsp.transferprocess.transformer.type.from.JsonObjectFromTransferStartMessageTransformer;
import org.eclipse.edc.protocol.dsp.transferprocess.transformer.type.from.JsonObjectFromTransferTerminationMessageTransformer;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspCatalogPropertyAndTypeNames.DCT_PREFIX;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspCatalogPropertyAndTypeNames.DCT_SCHEMA;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspCatalogPropertyAndTypeNames.DSPACE_PREFIX;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspCatalogPropertyAndTypeNames.DSPACE_SCHEMA;


class JsonObjectFromTransferProcessTransformerTest {

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
                .add(DCT_PREFIX, DCT_SCHEMA)
                .add(DSPACE_PREFIX, DSPACE_SCHEMA)
                .build();
        contextDocument = JsonDocument.of(builderFactory.createObjectBuilder()
                .add("@context", contextObject)
                .build());

        registry = new JsonLdTransformerRegistryImpl();

        // EDC model to JSON-LD transformers

        registry.register(new JsonObjectFromTransferCompletionMessageTransformer(builderFactory, mapper));
        registry.register(new JsonObjectFromTransferTerminationMessageTransformer(builderFactory, mapper));
        registry.register(new JsonObjectFromTransferProcessTransformer(builderFactory, mapper));
        registry.register(new JsonObjectFromTransferRequestMessageTransformer(builderFactory, mapper));
        registry.register(new JsonObjectFromTransferStartMessageTransformer(builderFactory, mapper));
    }

    @Test
    void transformTransferProcess() throws JsonLdError {
        var dataAddress = DataAddress.Builder.newInstance()
                .keyName("dataAddressId")
                .property("type", "TestValueProperty")
                .build();

        var dataRequest = DataRequest.Builder.newInstance()
                .id("dataRequestID")
                .dataDestination(dataAddress)
                .build();

        var transferProcess = TransferProcess.Builder.newInstance()
                .id("transferProcessID")
                .callbackAddresses(new ArrayList<>())
                .dataRequest(dataRequest)
                .type(TransferProcess.Type.PROVIDER)
                .contentDataAddress(dataAddress)
                .build();

        var result = registry.transform(transferProcess, JsonObject.class);

        Assertions.assertNotNull(result);

        var document = JsonDocument.of(result.getContent());
        var obj = JsonLd.compact(document, contextDocument).get();

        Assertions.assertNotNull(obj);
    }

    @Test
    void transformTransferCompletion() throws JsonLdError {
        var message = TransferCompletionMessage.Builder.newInstance()
                .processId("TestID")
                .connectorAddress("TestConnectorAddress")
                .protocol("dsp")
                .build();

        var result = registry.transform(message, JsonObject.class);

        Assertions.assertNotNull(result);

        var document = JsonDocument.of(result.getContent());
        var obj = JsonLd.compact(document, contextDocument).get();

        Assertions.assertNotNull(obj);
    }

    @Test
    void transformTransferRequest() throws JsonLdError {
        var properties = new HashMap<String, String>();
        properties.put("key", "value");

        var message = TransferRequestMessage.Builder.newInstance()
                .id("TestID")
                .assetId("TestAssetID")
                .properties(properties)
                .connectorAddress("TestConnectorAddress")
                .contractId("ContractID")
                .protocol("dsp")
                .dataDestination(buildTestDataAddress())
                .build();

        var result = registry.transform(message, JsonObject.class);

        Assertions.assertNotNull(result);

        var document = JsonDocument.of(result.getContent());
        var obj = JsonLd.compact(document, contextDocument).get();

        Assertions.assertNotNull(obj);
    }

    @Test
    void transformTransferStart() throws JsonLdError {
        var message = TransferStartMessage.Builder.newInstance()
                .processId("TestID")
                .connectorAddress("TestConnectorAddress")
                .protocol("dsp")
                .build();

        var result = registry.transform(message, JsonObject.class);

        Assertions.assertNotNull(result);

        var document = JsonDocument.of(result.getContent());
        var obj = JsonLd.compact(document, contextDocument).get();

        Assertions.assertNotNull(obj);
    }

    @Test
    void transformTransferTermination() throws JsonLdError {
        var message = TransferTerminationMessage.Builder.newInstance()
                .processId("TestID")
                .connectorAddress("TestConnectorAddress")
                .protocol("dsp")
                .build();

        var result = registry.transform(message, JsonObject.class);

        Assertions.assertNotNull(result);

        var document = JsonDocument.of(result.getContent());
        var obj = JsonLd.compact(document, contextDocument).get();

        Assertions.assertNotNull(obj);
    }


    private DataAddress buildTestDataAddress() {
        return DataAddress.Builder.newInstance()
                .keyName("dataAddressId")
                .property("type", "TestValueProperty")
                .build();
    }

    private JsonObject getJson() {
        var builder = builderFactory.createObjectBuilder();
        builder.add(JsonLdKeywords.ID, "negotiationId");
        builder.add(JsonLdKeywords.TYPE, DSPACE_SCHEMA + "ContractNegotiation");

        builder.add(DSPACE_SCHEMA + "correlationId", "correlationId");


        return builder.build();
    }
}
