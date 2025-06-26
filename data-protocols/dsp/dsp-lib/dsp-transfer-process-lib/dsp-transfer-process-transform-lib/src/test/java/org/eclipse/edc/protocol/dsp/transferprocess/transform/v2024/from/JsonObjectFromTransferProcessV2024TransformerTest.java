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
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.protocol.dsp.transferprocess.transform.type.v2024.from.JsonObjectFromTransferProcessV2024Transformer;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.transform.spi.TransformerContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.COMPLETED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.COMPLETING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.COMPLETING_REQUESTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.DEPROVISIONED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.DEPROVISIONING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.DEPROVISIONING_REQUESTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.REQUESTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.REQUESTING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.RESUMED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.RESUMING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.STARTING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.SUSPENDED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.SUSPENDING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.SUSPENDING_REQUESTED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.TERMINATED;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.TERMINATING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.TERMINATING_REQUESTED;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_CONSUMER_PID_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_PROVIDER_PID_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_STATE_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_PROCESS_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_VALUE_TRANSFER_STATE_COMPLETED_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_VALUE_TRANSFER_STATE_REQUESTED_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_VALUE_TRANSFER_STATE_STARTED_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_VALUE_TRANSFER_STATE_SUSPENDED_TERM;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_VALUE_TRANSFER_STATE_TERMINATED_TERM;
import static org.eclipse.edc.protocol.dsp.transferprocess.transform.v2024.from.TestFunctionV2024.DSP_NAMESPACE;
import static org.eclipse.edc.protocol.dsp.transferprocess.transform.v2024.from.TestFunctionV2024.toIri;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;


class JsonObjectFromTransferProcessV2024TransformerTest {

    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(Map.of());
    private final TransformerContext context = mock();

    private final JsonObjectFromTransferProcessV2024Transformer transformer =
            new JsonObjectFromTransferProcessV2024Transformer(jsonFactory, DSP_NAMESPACE);


    @Test
    void transformTransferProcessProvider() {
        var dataAddress = DataAddress.Builder.newInstance()
                .keyName("dataAddressId")
                .property("type", "TestValueProperty")
                .build();

        var transferProcess = TransferProcess.Builder.newInstance()
                .id("providerPid")
                .callbackAddresses(new ArrayList<>())
                .correlationId("consumerPid")
                .dataDestination(dataAddress)
                .state(REQUESTED.code())
                .type(TransferProcess.Type.PROVIDER)
                .contentDataAddress(dataAddress)
                .build();

        var result = transformer.transform(transferProcess, context);

        assertThat(result).isNotNull();
        assertThat(result.getJsonString(TYPE).getString()).isEqualTo(toIri(DSPACE_TYPE_TRANSFER_PROCESS_TERM));
        assertThat(result.getJsonString(ID).getString()).isEqualTo("providerPid");
        assertThat(result.getJsonObject(toIri(DSPACE_PROPERTY_STATE_TERM)).getString(ID)).isEqualTo(toIri("REQUESTED"));
        assertThat(result.getJsonObject(toIri(DSPACE_PROPERTY_CONSUMER_PID_TERM)).getString(ID)).isEqualTo("consumerPid");
        assertThat(result.getJsonObject(toIri(DSPACE_PROPERTY_PROVIDER_PID_TERM)).getString(ID)).isEqualTo("providerPid");

        verify(context, never()).reportProblem(anyString());
    }

    @Test
    void transformTransferProcessConsumer() {
        var dataAddress = DataAddress.Builder.newInstance()
                .keyName("dataAddressId")
                .property("type", "TestValueProperty")
                .build();

        var transferProcess = TransferProcess.Builder.newInstance()
                .id("consumerPid")
                .callbackAddresses(new ArrayList<>())
                .correlationId("providerPid")
                .dataDestination(dataAddress)
                .state(REQUESTED.code())
                .type(TransferProcess.Type.CONSUMER)
                .contentDataAddress(dataAddress)
                .build();

        var result = transformer.transform(transferProcess, context);

        assertThat(result).isNotNull();
        assertThat(result.getJsonString(TYPE).getString()).isEqualTo(toIri(DSPACE_TYPE_TRANSFER_PROCESS_TERM));
        assertThat(result.getJsonString(ID).getString()).isEqualTo("consumerPid");
        assertThat(result.getJsonObject(toIri(DSPACE_PROPERTY_STATE_TERM)).getString(ID)).isEqualTo(toIri("REQUESTED"));
        assertThat(result.getJsonObject(toIri(DSPACE_PROPERTY_CONSUMER_PID_TERM)).getString(ID)).isEqualTo("consumerPid");
        assertThat(result.getJsonObject(toIri(DSPACE_PROPERTY_PROVIDER_PID_TERM)).getString(ID)).isEqualTo("providerPid");

        verify(context, never()).reportProblem(anyString());
    }

    @ParameterizedTest
    @ArgumentsSource(Status.class)
    void transform_status(TransferProcessStates inputState, String expectedDspState, String errorDetail) {
        var dataAddress = DataAddress.Builder.newInstance()
                .keyName("dataAddressId")
                .property("type", "TestValueProperty")
                .build();
        var transferProcess = TransferProcess.Builder.newInstance()
                .id("consumerPid")
                .callbackAddresses(new ArrayList<>())
                .correlationId("providerPid")
                .dataDestination(dataAddress)
                .state(inputState.code())
                .contentDataAddress(dataAddress)
                .errorDetail(errorDetail)
                .build();

        var result = transformer.transform(transferProcess, context);

        assertThat(result).isNotNull();
        assertThat(result.getJsonObject(toIri(DSPACE_PROPERTY_STATE_TERM)).getString(ID)).isEqualTo(expectedDspState);

        verify(context, never()).reportProblem(anyString());
    }

    public static class Status implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    arguments(REQUESTING, toIri(DSPACE_VALUE_TRANSFER_STATE_REQUESTED_TERM), null),
                    arguments(REQUESTED, toIri(DSPACE_VALUE_TRANSFER_STATE_REQUESTED_TERM), null),
                    arguments(STARTING, toIri(DSPACE_VALUE_TRANSFER_STATE_STARTED_TERM), null),
                    arguments(STARTED, toIri(DSPACE_VALUE_TRANSFER_STATE_STARTED_TERM), null),
                    arguments(SUSPENDING, toIri(DSPACE_VALUE_TRANSFER_STATE_SUSPENDED_TERM), null),
                    arguments(SUSPENDING_REQUESTED, toIri(DSPACE_VALUE_TRANSFER_STATE_SUSPENDED_TERM), null),
                    arguments(SUSPENDED, toIri(DSPACE_VALUE_TRANSFER_STATE_SUSPENDED_TERM), null),
                    arguments(RESUMING, toIri(DSPACE_VALUE_TRANSFER_STATE_SUSPENDED_TERM), null),
                    arguments(RESUMED, toIri(DSPACE_VALUE_TRANSFER_STATE_SUSPENDED_TERM), null),
                    arguments(COMPLETING, toIri(DSPACE_VALUE_TRANSFER_STATE_COMPLETED_TERM), null),
                    arguments(COMPLETING_REQUESTED, toIri(DSPACE_VALUE_TRANSFER_STATE_COMPLETED_TERM), null),
                    arguments(COMPLETED, toIri(DSPACE_VALUE_TRANSFER_STATE_COMPLETED_TERM), null),
                    arguments(DEPROVISIONING, toIri(DSPACE_VALUE_TRANSFER_STATE_COMPLETED_TERM), null),
                    arguments(DEPROVISIONING_REQUESTED, toIri(DSPACE_VALUE_TRANSFER_STATE_COMPLETED_TERM), null),
                    arguments(DEPROVISIONED, toIri(DSPACE_VALUE_TRANSFER_STATE_COMPLETED_TERM), null),
                    arguments(DEPROVISIONING, toIri(DSPACE_VALUE_TRANSFER_STATE_TERMINATED_TERM), "error`"),
                    arguments(DEPROVISIONING_REQUESTED, toIri(DSPACE_VALUE_TRANSFER_STATE_TERMINATED_TERM), "error"),
                    arguments(DEPROVISIONED, toIri(DSPACE_VALUE_TRANSFER_STATE_TERMINATED_TERM), "error"),
                    arguments(TERMINATING, toIri(DSPACE_VALUE_TRANSFER_STATE_TERMINATED_TERM), null),
                    arguments(TERMINATING_REQUESTED, toIri(DSPACE_VALUE_TRANSFER_STATE_TERMINATED_TERM), null),
                    arguments(TERMINATED, toIri(DSPACE_VALUE_TRANSFER_STATE_TERMINATED_TERM), null)
            );
        }
    }
}
