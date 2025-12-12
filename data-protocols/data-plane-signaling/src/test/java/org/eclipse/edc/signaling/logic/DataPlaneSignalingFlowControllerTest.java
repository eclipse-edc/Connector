/*
 *  Copyright (c) 2025 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.signaling.logic;

import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.transfer.spi.flow.TransferTypeParser;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.FlowType;
import org.eclipse.edc.spi.types.domain.transfer.TransferType;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class DataPlaneSignalingFlowControllerTest {

    private final DataPlaneSelectorService dataPlaneSelectorService = Mockito.mock();
    private final TransferTypeParser transferTypeParser = Mockito.mock();
    private final DataPlaneSignalingFlowController controller = new DataPlaneSignalingFlowController(
            dataPlaneSelectorService, transferTypeParser);

    @Nested
    class CanHandle {
        @Test
        void shouldReturnTrue_whenFlowTypeIsValid() {
            when(transferTypeParser.parse(any())).thenReturn(Result.success(new TransferType("Valid", FlowType.PUSH)));
            var transferProcess = TransferProcess.Builder.newInstance()
                    .transferType("Valid-PUSH")
                    .dataDestination(DataAddress.Builder.newInstance().type("Custom").build())
                    .build();

            var result = controller.canHandle(transferProcess);

            assertThat(result).isTrue();
        }

        @Test
        void shouldReturnFalse_whenFlowTypeIsNotValid() {
            when(transferTypeParser.parse(any())).thenReturn(Result.failure("cannot parse"));
            var transferProcess = TransferProcess.Builder.newInstance()
                    .transferType("Invalid-ANY")
                    .dataDestination(DataAddress.Builder.newInstance().type("Custom").build())
                    .build();

            var result = controller.canHandle(transferProcess);

            assertThat(result).isFalse();
        }
    }

    @Nested
    class TransferTypes {

        @Test
        void shouldReturnSupportedTransferTypes() {
            var assetNoResponse = Asset.Builder.newInstance().dataAddress(DataAddress.Builder.newInstance().type("TargetSrc").build()).build();
            when(transferTypeParser.parse(any()))
                    .thenReturn(Result.success(new TransferType("any", FlowType.PUSH)))
                    .thenReturn(Result.success(new TransferType("any", FlowType.PULL)));

            when(dataPlaneSelectorService.getAll()).thenReturn(ServiceResult.success(List.of(
                    dataPlaneInstanceBuilder().allowedTransferType("Custom-PUSH").build(),
                    dataPlaneInstanceBuilder().allowedTransferType("Custom-PULL").build()
            )));

            var transferTypes = controller.transferTypesFor(assetNoResponse);

            assertThat(transferTypes).containsExactly("Custom-PUSH", "Custom-PULL");
        }

        @Test
        void shouldReturnEmptyList_whenCannotGetDataplaneInstances() {
            when(dataPlaneSelectorService.getAll()).thenReturn(ServiceResult.unexpected("error"));
            var asset = Asset.Builder.newInstance().dataAddress(DataAddress.Builder.newInstance().type("TargetSrc").build()).build();

            var transferTypes = controller.transferTypesFor(asset);

            assertThat(transferTypes).isEmpty();
        }
    }

    @NotNull
    private DataPlaneInstance.Builder dataPlaneInstanceBuilder() {
        return DataPlaneInstance.Builder.newInstance().url("http://any");
    }
}
