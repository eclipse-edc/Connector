/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.connector.api.signaling.transform.to;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObjectBuilder;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowProvisionMessage;
import org.eclipse.edc.spi.types.domain.transfer.FlowType;
import org.eclipse.edc.spi.types.domain.transfer.TransferType;
import org.eclipse.edc.transform.spi.ProblemBuilder;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.api.signaling.transform.TestFunctions.getExpanded;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_PREFIX;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsonObjectToDataFlowProvisionMessageTransformerTest {

    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private final TransformerContext context = mock(TransformerContext.class);
    private JsonObjectToDataFlowProvisionMessageTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectToDataFlowProvisionMessageTransformer();
        when(context.transform(any(), eq(DataAddress.class))).thenReturn(DataAddress.Builder.newInstance().type("address-type").build());
        when(context.problem()).thenReturn(new ProblemBuilder(context));
    }

    @Test
    void transform() {
        var jsonObj = jsonFactory.createObjectBuilder()
                .add(CONTEXT, createContextBuilder().build())
                .add(TYPE, DataFlowProvisionMessage.EDC_DATA_FLOW_PROVISION_MESSAGE_TYPE)
                .add("processId", "processId")
                .add("agreementId", "agreementId")
                .add("datasetId", "datasetId")
                .add("participantId", "participantId")
                .add("transferTypeDestination", "transferTypeDestination")
                .add("flowType", "PULL")
                .add("responseChannel", "Websocket")
                .add("destination", jsonFactory.createObjectBuilder().add("type", "address-type"))
                .add("properties", jsonFactory.createObjectBuilder().add("foo", "bar"))
                .add("callbackAddress", "http://localhost")
                .build();

        var message = transformer.transform(getExpanded(jsonObj), context);

        assertThat(message).isNotNull();

        assertThat(message.getProcessId()).isEqualTo("processId");
        assertThat(message.getAssetId()).isEqualTo("datasetId");
        assertThat(message.getAgreementId()).isEqualTo("agreementId");
        assertThat(message.getParticipantId()).isEqualTo("participantId");
        assertThat(message.getTransferType()).isEqualTo(new TransferType("transferTypeDestination", FlowType.PULL, "Websocket"));
        assertThat(message.getDestination()).extracting(DataAddress::getType).isEqualTo("address-type");
        assertThat(message.getProperties()).containsEntry(EDC_NAMESPACE + "foo", "bar");
        assertThat(message.getCallbackAddress()).isEqualTo(URI.create("http://localhost"));
    }

    private JsonObjectBuilder createContextBuilder() {
        return jsonFactory.createObjectBuilder()
                .add(VOCAB, EDC_NAMESPACE)
                .add(EDC_PREFIX, EDC_NAMESPACE);
    }
}
