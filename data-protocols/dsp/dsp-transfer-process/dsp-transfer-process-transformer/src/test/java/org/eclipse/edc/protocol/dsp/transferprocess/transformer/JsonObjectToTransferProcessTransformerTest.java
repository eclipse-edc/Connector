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

import com.apicatalog.jsonld.document.JsonDocument;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsonp.JSONPModule;
import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferRequestMessage;
import org.eclipse.edc.jsonld.transformer.JsonLdTransformerRegistryImpl;
import org.eclipse.edc.protocol.dsp.transferprocess.transformer.type.to.JsonObjectToTransferRequestMessage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspCatalogPropertyAndTypeNames.DCT_FORMAT;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspCatalogPropertyAndTypeNames.DCT_PREFIX;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspCatalogPropertyAndTypeNames.DCT_SCHEMA;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspCatalogPropertyAndTypeNames.DSPACE_CALLBACKADDRESS_TYPE;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspCatalogPropertyAndTypeNames.DSPACE_CONTRACTAGREEMENT_TYPE;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspCatalogPropertyAndTypeNames.DSPACE_DATAADDRESS_TYPE;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspCatalogPropertyAndTypeNames.DSPACE_PREFIX;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspCatalogPropertyAndTypeNames.DSPACE_SCHEMA;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspCatalogPropertyAndTypeNames.DSPACE_TRANSFERPROCESS_REQUEST_TYPE;

public class JsonObjectToTransferProcessTransformerTest {

    private final String processId = "TestProcessID";

    private final String contractId = "TestContreactID";

    private final String destinationType = "dspace:s3+push";

    private final String callbackAddress = "https://callback.de";

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

        registry.register(new JsonObjectToTransferRequestMessage());

    }

    @Test
    void jsonObjectToTransferRequestWithoutDataAddress() {
        var json = createJsonTransferRequestWithoutDataAddress();

        var result = registry.transform(json, TransferRequestMessage.class);

        Assertions.assertNotNull(result.getContent());

        assertThat(result.getContent().getContractId()).isEqualTo(contractId);
        assertThat(result.getContent().getDataDestination().getType()).isEqualTo(destinationType);
        assertThat(result.getContent().getConnectorAddress()).isEqualTo(callbackAddress);
    }

    @Test
    void jsonObjectToTransferRequestWithDataAddress() {
        var json = createJsonTransferRequestWithDataAddress();

        var result = registry.transform(json, TransferRequestMessage.class);

        Assertions.assertNotNull(result.getContent());

        assertThat(result.getContent().getContractId()).isEqualTo(contractId);
        assertThat(result.getContent().getDataDestination().getType()).isEqualTo(destinationType);
        assertThat(result.getContent().getConnectorAddress()).isEqualTo(callbackAddress);

        assertThat(result.getContent().getDataDestination().getProperty("accessKeyId")).isEqualTo("TESTID");
        assertThat(result.getContent().getDataDestination().getProperty("region")).isEqualTo("eu-central-1");
    }

    private JsonObject createJsonTransferRequestWithoutDataAddress() {
        return Json.createObjectBuilder()
                .add(CONTEXT, DSPACE_SCHEMA)
                .add(TYPE, DSPACE_TRANSFERPROCESS_REQUEST_TYPE)
                .add(DSPACE_CONTRACTAGREEMENT_TYPE, contractId)
                .add(DCT_FORMAT, destinationType)
                .add(DSPACE_DATAADDRESS_TYPE, Json.createObjectBuilder().build())
                .add(DSPACE_CALLBACKADDRESS_TYPE, callbackAddress)
                .build();
    }

    private JsonObject createJsonTransferRequestWithDataAddress() {
        return Json.createObjectBuilder()
                .add(CONTEXT, DSPACE_SCHEMA)
                .add(TYPE, DSPACE_TRANSFERPROCESS_REQUEST_TYPE)
                .add(DSPACE_CONTRACTAGREEMENT_TYPE, contractId)
                .add(DCT_FORMAT, destinationType)
                .add(DSPACE_DATAADDRESS_TYPE, createDataAddress())
                .add(DSPACE_CALLBACKADDRESS_TYPE, callbackAddress)
                .build();
    }

    private JsonObject createDataAddress() {
        return Json.createObjectBuilder()
                .add("accessKeyId", "TESTID")
                .add("region", "eu-central-1")
                .build();
    }
}
