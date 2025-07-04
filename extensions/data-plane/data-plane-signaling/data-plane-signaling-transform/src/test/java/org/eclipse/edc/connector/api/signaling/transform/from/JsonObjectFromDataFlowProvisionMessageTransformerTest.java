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

package org.eclipse.edc.connector.api.signaling.transform.from;

import jakarta.json.Json;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowProvisionMessage;
import org.eclipse.edc.spi.types.domain.transfer.FlowType;
import org.eclipse.edc.spi.types.domain.transfer.TransferType;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.types.domain.transfer.DataFlowProvisionMessage.EDC_DATA_FLOW_PROVISION_MESSAGE_AGREEMENT_ID;
import static org.eclipse.edc.spi.types.domain.transfer.DataFlowProvisionMessage.EDC_DATA_FLOW_PROVISION_MESSAGE_CALLBACK_ADDRESS;
import static org.eclipse.edc.spi.types.domain.transfer.DataFlowProvisionMessage.EDC_DATA_FLOW_PROVISION_MESSAGE_DATASET_ID;
import static org.eclipse.edc.spi.types.domain.transfer.DataFlowProvisionMessage.EDC_DATA_FLOW_PROVISION_MESSAGE_DESTINATION;
import static org.eclipse.edc.spi.types.domain.transfer.DataFlowProvisionMessage.EDC_DATA_FLOW_PROVISION_MESSAGE_FLOW_TYPE;
import static org.eclipse.edc.spi.types.domain.transfer.DataFlowProvisionMessage.EDC_DATA_FLOW_PROVISION_MESSAGE_PARTICIPANT_ID;
import static org.eclipse.edc.spi.types.domain.transfer.DataFlowProvisionMessage.EDC_DATA_FLOW_PROVISION_MESSAGE_PROCESS_ID;
import static org.eclipse.edc.spi.types.domain.transfer.DataFlowProvisionMessage.EDC_DATA_FLOW_PROVISION_MESSAGE_TRANSFER_RESPONSE_CHANNEL;
import static org.eclipse.edc.spi.types.domain.transfer.DataFlowProvisionMessage.EDC_DATA_FLOW_PROVISION_MESSAGE_TRANSFER_TYPE_DESTINATION;
import static org.eclipse.edc.spi.types.domain.transfer.DataFlowProvisionMessage.EDC_DATA_FLOW_PROVISION_MESSAGE_TYPE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsonObjectFromDataFlowProvisionMessageTransformerTest {

    private final TypeManager typeManager = mock();
    private final TransformerContext context = mock();
    private JsonObjectFromDataFlowProvisionMessageTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new JsonObjectFromDataFlowProvisionMessageTransformer(Json.createBuilderFactory(Map.of()), typeManager, "test");
        when(context.transform(isA(DataAddress.class), any())).thenReturn(Json.createObjectBuilder().build());
        when(typeManager.getMapper("test")).thenReturn(JacksonJsonLd.createObjectMapper());
    }

    @Test
    void transform() {
        var message = DataFlowProvisionMessage.Builder.newInstance()
                .processId("processId")
                .assetId("assetId")
                .agreementId("agreementId")
                .participantId("participantId")
                .transferType(new TransferType("HttpData", FlowType.PUSH, "Websocket"))
                .callbackAddress(URI.create("http://localhost"))
                .destination(DataAddress.Builder.newInstance().type("destType").build())
                .build();

        var jsonObject = transformer.transform(message, context);

        assertThat(jsonObject).isNotNull();
        assertThat(jsonObject.getString(TYPE)).isEqualTo(EDC_DATA_FLOW_PROVISION_MESSAGE_TYPE);
        assertThat(jsonObject.getString(EDC_DATA_FLOW_PROVISION_MESSAGE_PROCESS_ID)).isEqualTo(message.getProcessId());
        assertThat(jsonObject.getString(EDC_DATA_FLOW_PROVISION_MESSAGE_DATASET_ID)).isEqualTo(message.getAssetId());
        assertThat(jsonObject.getString(EDC_DATA_FLOW_PROVISION_MESSAGE_AGREEMENT_ID)).isEqualTo(message.getAgreementId());
        assertThat(jsonObject.getString(EDC_DATA_FLOW_PROVISION_MESSAGE_PARTICIPANT_ID)).isEqualTo(message.getParticipantId());
        assertThat(jsonObject.getString(EDC_DATA_FLOW_PROVISION_MESSAGE_CALLBACK_ADDRESS)).isEqualTo(message.getCallbackAddress().toString());
        assertThat(jsonObject.getString(EDC_DATA_FLOW_PROVISION_MESSAGE_TRANSFER_TYPE_DESTINATION)).isEqualTo("HttpData");
        assertThat(jsonObject.getString(EDC_DATA_FLOW_PROVISION_MESSAGE_FLOW_TYPE)).isEqualTo(message.getTransferType().flowType().toString());
        assertThat(jsonObject.getString(EDC_DATA_FLOW_PROVISION_MESSAGE_TRANSFER_RESPONSE_CHANNEL)).isEqualTo("Websocket");
        assertThat(jsonObject.get(EDC_DATA_FLOW_PROVISION_MESSAGE_DESTINATION)).isNotNull();
    }

}
