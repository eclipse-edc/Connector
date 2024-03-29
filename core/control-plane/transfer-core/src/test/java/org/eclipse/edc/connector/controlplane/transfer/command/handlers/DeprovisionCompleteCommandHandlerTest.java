/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.controlplane.transfer.command.handlers;

import org.eclipse.edc.connector.controlplane.transfer.provision.DeprovisionResponsesHandler;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DeprovisionedResource;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.command.DeprovisionCompleteCommand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.DEPROVISIONING;
import static org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates.TERMINATED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeprovisionCompleteCommandHandlerTest {

    private final DeprovisionResponsesHandler deprovisionResponsesHandler = mock(DeprovisionResponsesHandler.class);
    private final DeprovisionCompleteCommandHandler handler = new DeprovisionCompleteCommandHandler(mock(), deprovisionResponsesHandler);

    @Test
    void modify_shouldInvokeDeprovisionResultHandler() {
        var entity = TransferProcess.Builder.newInstance().state(DEPROVISIONING.code()).build();
        var provisionResponse = DeprovisionedResource.Builder.newInstance().provisionedResourceId("provisionedResourceId").build();
        var command = new DeprovisionCompleteCommand("id", provisionResponse);
        when(deprovisionResponsesHandler.handle(any(), any())).thenReturn(true);

        var result = handler.modify(entity, command);

        assertThat(result).isTrue();
        verify(deprovisionResponsesHandler).handle(eq(entity), argThat(it -> it.get(0).getContent().equals(provisionResponse)));
    }

    @Test
    void postActions_shouldInvokeDeprovisionResultHandler() {
        var entity = TransferProcess.Builder.newInstance().state(DEPROVISIONING.code()).build();
        var provisionResponse = DeprovisionedResource.Builder.newInstance().provisionedResourceId("provisionedResourceId").build();
        var command = new DeprovisionCompleteCommand("id", provisionResponse);

        handler.postActions(entity, command);

        verify(deprovisionResponsesHandler).postActions(eq(entity));
    }

    @Test
    void modify_shouldReturnFalse_whenDeprovisionResultHandlerCannotHandle() {
        var entity = TransferProcess.Builder.newInstance().state(TERMINATED.code()).build();
        var provisionResponse = DeprovisionedResource.Builder.newInstance().provisionedResourceId("provisionedResourceId").build();
        var command = new DeprovisionCompleteCommand("id", provisionResponse);
        when(deprovisionResponsesHandler.handle(any(), any())).thenReturn(false);

        var result = handler.modify(entity, command);

        assertThat(result).isFalse();
    }
}
