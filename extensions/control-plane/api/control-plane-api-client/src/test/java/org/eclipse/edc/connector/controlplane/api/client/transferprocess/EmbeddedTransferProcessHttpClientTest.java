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

package org.eclipse.edc.connector.controlplane.api.client.transferprocess;

import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.dataplane.spi.DataFlow;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmbeddedTransferProcessHttpClientTest {

    private final TransferProcessService service = mock();
    private final EmbeddedTransferProcessHttpClient client = new EmbeddedTransferProcessHttpClient(service);

    @Nested
    class Complete {
        @Test
        void shouldCallService() {
            var serviceResult = ServiceResult.<Void>success();
            when(service.complete(any())).thenReturn(serviceResult);
            var message = DataFlowStartMessage.Builder.newInstance().processId("any")
                    .sourceDataAddress(DataAddress.Builder.newInstance().type("any").build()).build();

            var result = client.completed(message);

            assertThat(result).isSucceeded();
            verify(service).complete(any());
        }
    }

    @Nested
    class Failed {
        @Test
        void shouldCallService() {
            var serviceResult = ServiceResult.<Void>success();
            when(service.terminate(any())).thenReturn(serviceResult);
            var message = DataFlowStartMessage.Builder.newInstance().processId("any")
                    .sourceDataAddress(DataAddress.Builder.newInstance().type("any").build()).build();

            var result = client.failed(message, "any");

            assertThat(result).isSucceeded();
            verify(service).terminate(any());
        }
    }

    @Nested
    class Provisioned {
        @Test
        void shouldCallService() {
            var serviceResult = ServiceResult.<Void>success();
            when(service.completeProvision(any())).thenReturn(serviceResult);

            var result = client.provisioned(DataFlow.Builder.newInstance().id("anyId").build());

            assertThat(result).isSucceeded();
            verify(service).completeProvision(any());
        }
    }

}
