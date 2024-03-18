/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.framework.registry;

import org.eclipse.edc.connector.dataplane.spi.pipeline.TransferService;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class TransferServiceSelectionStrategyTest {

    TransferServiceSelectionStrategy strategy = TransferServiceSelectionStrategy.selectFirst();
    DataFlowStartMessage request = createRequest().build();
    TransferService service1 = mock();
    TransferService service2 = mock();

    @Test
    void selectFirst_withNoItems() {
        assertThat(strategy.chooseTransferService(request, Stream.of())).isNull();
    }

    @Test
    void selectFirst_withOneItem() {
        assertThat(strategy.chooseTransferService(request, Stream.of(service1))).isEqualTo(service1);
    }

    @Test
    void selectFirst_withMultipleItems() {
        assertThat(strategy.chooseTransferService(request, Stream.of(service1, service2))).isEqualTo(service1);
    }

    private DataFlowStartMessage.Builder createRequest() {
        return DataFlowStartMessage.Builder.newInstance()
                .id("1")
                .processId("1")
                .sourceDataAddress(DataAddress.Builder.newInstance().type("any").build())
                .destinationDataAddress(DataAddress.Builder.newInstance().type("any").build());
    }
}
