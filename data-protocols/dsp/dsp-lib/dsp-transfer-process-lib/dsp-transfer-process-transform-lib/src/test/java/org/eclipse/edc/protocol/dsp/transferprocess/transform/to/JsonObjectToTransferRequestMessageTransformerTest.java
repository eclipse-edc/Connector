/*
 *  Copyright (c) 2023 Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.transferprocess.transform.to;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.protocol.dsp.transferprocess.transform.type.to.JsonObjectToTransferRequestMessageTransformer;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.ProblemBuilder;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCT_FORMAT_ATTRIBUTE;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CALLBACK_ADDRESS_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CONSUMER_PID_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_PROPERTY_CONTRACT_AGREEMENT_ID_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_PROPERTY_DATA_ADDRESS_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_REQUEST_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.transferprocess.transform.to.TestInput.getExpanded;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JsonObjectToTransferRequestMessageTransformerTest {

    private static final JsonLdNamespace DSP_NAMESPACE = new JsonLdNamespace("http://www.w3.org/ns/dsp#");
    private final String callbackAddress = "https://callback.de";
    private final String contractId = "TestContreactID";
    private final String destinationType = "dspace:s3+push";

    private final TransformerContext context = mock();

    private final JsonObjectToTransferRequestMessageTransformer transformer =
            new JsonObjectToTransferRequestMessageTransformer(DSP_NAMESPACE);

    @Test
    void jsonObjectToTransferRequestWithoutDataAddress() {
        var json = Json.createObjectBuilder()
                .add(TYPE, DSP_NAMESPACE.toIri(DSPACE_TYPE_TRANSFER_REQUEST_MESSAGE_TERM))
                .add(DSP_NAMESPACE.toIri(DSPACE_PROPERTY_CONTRACT_AGREEMENT_ID_TERM), contractId)
                .add(DCT_FORMAT_ATTRIBUTE, destinationType)
                .add(DSP_NAMESPACE.toIri(DSPACE_PROPERTY_CALLBACK_ADDRESS_TERM), callbackAddress)
                .add(DSP_NAMESPACE.toIri(DSPACE_PROPERTY_CONSUMER_PID_TERM), "processId")
                .build();

        var result = transformer.transform(getExpanded(json), context);

        assertThat(result).isNotNull();
        assertThat(result.getContractId()).isEqualTo(contractId);
        assertThat(result.getTransferType()).isEqualTo(destinationType);
        assertThat(result.getDataAddress()).isNull();
        assertThat(result.getCallbackAddress()).isEqualTo(callbackAddress);
        assertThat(result.getConsumerPid()).isEqualTo("processId");

        verify(context, never()).reportProblem(anyString());
    }

    @Test
    void jsonObjectToTransferRequestWithDataAddress() {
        var dataDestination = DataAddress.Builder.newInstance().type("any").build();
        when(context.transform(any(), eq(DataAddress.class))).thenReturn(dataDestination);
        var json = Json.createObjectBuilder()
                .add(TYPE, DSP_NAMESPACE.toIri(DSPACE_TYPE_TRANSFER_REQUEST_MESSAGE_TERM))
                .add(DSP_NAMESPACE.toIri(DSPACE_PROPERTY_CONTRACT_AGREEMENT_ID_TERM), contractId)
                .add(DCT_FORMAT_ATTRIBUTE, destinationType)
                .add(DSP_NAMESPACE.toIri(DSPACE_PROPERTY_DATA_ADDRESS_TERM), createDataAddress())
                .add(DSP_NAMESPACE.toIri(DSPACE_PROPERTY_CALLBACK_ADDRESS_TERM), callbackAddress)
                .add(DSP_NAMESPACE.toIri(DSPACE_PROPERTY_CONSUMER_PID_TERM), "processId")
                .build();

        var result = transformer.transform(getExpanded(json), context);

        assertThat(result).isNotNull();
        assertThat(result.getContractId()).isEqualTo(contractId);
        assertThat(result.getTransferType()).isEqualTo(destinationType);
        assertThat(result.getCallbackAddress()).isEqualTo(callbackAddress);
        assertThat(result.getDataAddress()).isSameAs(dataDestination);
        verify(context).transform(any(), eq(DataAddress.class));
        verify(context, never()).reportProblem(anyString());
    }

    @Test
    void shouldReturnNullAndReportError_whenConsumerPidNotSet() {
        when(context.problem()).thenReturn(new ProblemBuilder(context));
        var json = Json.createObjectBuilder()
                .add(TYPE, DSP_NAMESPACE.toIri(DSPACE_TYPE_TRANSFER_REQUEST_MESSAGE_TERM))
                .add(DSP_NAMESPACE.toIri(DSPACE_PROPERTY_CONTRACT_AGREEMENT_ID_TERM), contractId)
                .add(DCT_FORMAT_ATTRIBUTE, destinationType)
                .add(DSP_NAMESPACE.toIri(DSPACE_PROPERTY_DATA_ADDRESS_TERM), Json.createObjectBuilder().build())
                .add(DSP_NAMESPACE.toIri(DSPACE_PROPERTY_CALLBACK_ADDRESS_TERM), callbackAddress)
                .build();

        var result = transformer.transform(getExpanded(json), context);

        assertThat(result).isNull();
        verify(context).reportProblem(anyString());
    }

    private JsonObject createDataAddress() {
        return Json.createObjectBuilder()
                .add(EDC_NAMESPACE + "accessKeyId", "TESTID")
                .add(EDC_NAMESPACE + "region", "eu-central-1")
                .build();
    }
}
