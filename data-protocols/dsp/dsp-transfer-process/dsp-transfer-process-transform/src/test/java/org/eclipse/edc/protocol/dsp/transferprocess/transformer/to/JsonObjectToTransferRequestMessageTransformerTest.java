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

package org.eclipse.edc.protocol.dsp.transferprocess.transformer.to;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.protocol.dsp.transferprocess.transformer.type.to.JsonObjectToTransferRequestMessageTransformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCT_FORMAT_ATTRIBUTE;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspTransferProcessPropertyAndTypeNames.DSPACE_CALLBACKADDRESS_TYPE;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspTransferProcessPropertyAndTypeNames.DSPACE_CONTRACTAGREEMENT_TYPE;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspTransferProcessPropertyAndTypeNames.DSPACE_DATAADDRESS_TYPE;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspTransferProcessPropertyAndTypeNames.DSPACE_SCHEMA;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspTransferProcessPropertyAndTypeNames.DSPACE_TRANSFERPROCESS_REQUEST_TYPE;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class JsonObjectToTransferRequestMessageTransformerTest {

    private final String callbackAddress = "https://callback.de";
    private final String contractId = "TestContreactID";
    private final String destinationType = "dspace:s3+push";

    private TransformerContext context = mock(TransformerContext.class);

    private JsonObjectToTransferRequestMessageTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectToTransferRequestMessageTransformer();
    }

    @Test
    void jsonObjectToTransferRequestWithoutDataAddress() {
        var json = Json.createObjectBuilder()
                .add(CONTEXT, DSPACE_SCHEMA)
                .add(TYPE, DSPACE_TRANSFERPROCESS_REQUEST_TYPE)
                .add(DSPACE_CONTRACTAGREEMENT_TYPE, contractId)
                .add(DCT_FORMAT_ATTRIBUTE, destinationType)
                .add(DSPACE_DATAADDRESS_TYPE, Json.createObjectBuilder().build())
                .add(DSPACE_CALLBACKADDRESS_TYPE, callbackAddress)
                .build();

        var result = transformer.transform(json, context);

        assertThat(result).isNotNull();
        assertThat(result.getContractId()).isEqualTo(contractId);
        assertThat(result.getDataDestination().getType()).isEqualTo(destinationType);
        assertThat(result.getCallbackAddress()).isEqualTo(callbackAddress);

        verify(context, never()).reportProblem(anyString());
    }

    @Test
    void jsonObjectToTransferRequestWithDataAddress() {
        var json = Json.createObjectBuilder()
                .add(CONTEXT, DSPACE_SCHEMA)
                .add(TYPE, DSPACE_TRANSFERPROCESS_REQUEST_TYPE)
                .add(DSPACE_CONTRACTAGREEMENT_TYPE, contractId)
                .add(DCT_FORMAT_ATTRIBUTE, destinationType)
                .add(DSPACE_DATAADDRESS_TYPE, createDataAddress())
                .add(DSPACE_CALLBACKADDRESS_TYPE, callbackAddress)
                .build();

        var result = transformer.transform(json, context);

        assertThat(result).isNotNull();
        assertThat(result.getContractId()).isEqualTo(contractId);
        assertThat(result.getDataDestination().getType()).isEqualTo(destinationType);
        assertThat(result.getCallbackAddress()).isEqualTo(callbackAddress);
        assertThat(result.getDataDestination().getProperty("accessKeyId")).isEqualTo("TESTID");
        assertThat(result.getDataDestination().getProperty("region")).isEqualTo("eu-central-1");

        verify(context, never()).reportProblem(anyString());
    }

    private JsonObject createDataAddress() {
        return Json.createObjectBuilder()
                .add("accessKeyId", "TESTID")
                .add("region", "eu-central-1")
                .build();
    }
}
