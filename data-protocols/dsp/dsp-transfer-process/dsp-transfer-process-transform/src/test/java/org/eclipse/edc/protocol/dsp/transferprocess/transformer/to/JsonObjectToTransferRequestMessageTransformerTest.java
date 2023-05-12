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
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCT_FORMAT_ATTRIBUTE;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspTransferProcessPropertyAndTypeNames.DSPACE_CALLBACK_ADDRESS;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspTransferProcessPropertyAndTypeNames.DSPACE_CONTRACT_AGREEMENT_ID;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspTransferProcessPropertyAndTypeNames.DSPACE_DATA_ADDRESS;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspTransferProcessPropertyAndTypeNames.DSPACE_PROCESS_ID;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.DspTransferProcessPropertyAndTypeNames.DSPACE_TRANSFER_PROCESS_REQUEST_TYPE;
import static org.eclipse.edc.protocol.dsp.transferprocess.transformer.to.TestInput.getExpanded;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class JsonObjectToTransferRequestMessageTransformerTest {

    private final String callbackAddress = "https://callback.de";
    private final String contractId = "TestContreactID";
    private final String destinationType = "dspace:s3+push";

    private final TransformerContext context = mock(TransformerContext.class);

    private JsonObjectToTransferRequestMessageTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectToTransferRequestMessageTransformer();
    }

    @Test
    void jsonObjectToTransferRequestWithoutDataAddress() {
        var json = Json.createObjectBuilder()
                .add(TYPE, DSPACE_TRANSFER_PROCESS_REQUEST_TYPE)
                .add(DSPACE_CONTRACT_AGREEMENT_ID, contractId)
                .add(DCT_FORMAT_ATTRIBUTE, destinationType)
                .add(DSPACE_DATA_ADDRESS, Json.createObjectBuilder().build())
                .add(DSPACE_CALLBACK_ADDRESS, callbackAddress)
                .add(DSPACE_PROCESS_ID, "processId")
                .build();

        var result = transformer.transform(getExpanded(json), context);

        assertThat(result).isNotNull();
        assertThat(result.getContractId()).isEqualTo(contractId);
        assertThat(result.getDataDestination().getType()).isEqualTo(destinationType);
        assertThat(result.getCallbackAddress()).isEqualTo(callbackAddress);
        assertThat(result.getProcessId()).isEqualTo("processId");

        verify(context, never()).reportProblem(anyString());
    }

    @Test
    void jsonObjectToTransferRequestWithDataAddress() {
        var json = Json.createObjectBuilder()
                .add(TYPE, DSPACE_TRANSFER_PROCESS_REQUEST_TYPE)
                .add(DSPACE_CONTRACT_AGREEMENT_ID, contractId)
                .add(DCT_FORMAT_ATTRIBUTE, destinationType)
                .add(DSPACE_DATA_ADDRESS, createDataAddress())
                .add(DSPACE_CALLBACK_ADDRESS, callbackAddress)
                .add(DSPACE_PROCESS_ID, "processId")
                .build();

        var result = transformer.transform(getExpanded(json), context);

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
                .add(EDC_NAMESPACE + "accessKeyId", "TESTID")
                .add(EDC_NAMESPACE + "region", "eu-central-1")
                .build();
    }
}
