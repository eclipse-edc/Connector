/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.transferprocess.transform.v2024.from;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonString;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferSuspensionMessage;
import org.eclipse.edc.protocol.dsp.transferprocess.transform.type.v2024.from.JsonObjectFromTransferSuspensionMessageV2024Transformer;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CODE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CONSUMER_PID_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_PROVIDER_PID_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_REASON_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_SUSPENSION_MESSAGE_TERM;
import static org.eclipse.edc.protocol.dsp.transferprocess.transform.v2024.from.TestFunctionV2024.toIri;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;


class JsonObjectFromTransferSuspensionMessageV2024TransformerTest {

    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private final TransformerContext context = mock();

    private final JsonObjectFromTransferSuspensionMessageV2024Transformer transformer =
            new JsonObjectFromTransferSuspensionMessageV2024Transformer(jsonFactory);

    @Test
    void transformTransferSuspensionMessage() {
        var message = TransferSuspensionMessage.Builder.newInstance()
                .consumerPid("consumerPid")
                .providerPid("providerPid")
                .protocol("dsp")
                .code("testCode")
                .reason("testReason")
                .build();

        var result = transformer.transform(message, context);

        assertThat(result).isNotNull();
        assertThat(result.getJsonString(TYPE).getString()).isEqualTo(toIri(DSPACE_TYPE_TRANSFER_SUSPENSION_MESSAGE_TERM));
        assertThat(result.getJsonObject(toIri(DSPACE_PROPERTY_CONSUMER_PID_TERM)).getString(ID)).isEqualTo("consumerPid");
        assertThat(result.getJsonObject(toIri(DSPACE_PROPERTY_PROVIDER_PID_TERM)).getString(ID)).isEqualTo("providerPid");
        assertThat(result.getJsonString(toIri(DSPACE_PROPERTY_CODE_TERM)).getString()).isEqualTo("testCode");
        assertThat(result.getJsonArray(toIri(DSPACE_PROPERTY_REASON_TERM))).hasSize(1).first()
                .isInstanceOfSatisfying(JsonString.class, reason -> assertThat(reason.getString()).isEqualTo("testReason"));

        verify(context, never()).reportProblem(anyString());
    }
}
