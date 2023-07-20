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

package org.eclipse.edc.connector.transfer.command.handlers;

import org.eclipse.edc.connector.transfer.provision.ProvisionResponsesHandler;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionResponse;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.transfer.spi.types.command.AddProvisionedResourceCommand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.PROVISIONING;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AddProvisionedResourceCommandHandlerTest {

    private final ProvisionResponsesHandler provisionResponsesHandler = mock(ProvisionResponsesHandler.class);
    private final AddProvisionedResourceCommandHandler handler = new AddProvisionedResourceCommandHandler(mock(), provisionResponsesHandler);

    @Test
    void modify_shouldInvokeProvisionResultHandler() {
        var entity = TransferProcess.Builder.newInstance().state(PROVISIONING.code()).build();
        var provisionResponse = ProvisionResponse.Builder.newInstance().inProcess(true).build();
        var command = new AddProvisionedResourceCommand("id", provisionResponse);
        when(provisionResponsesHandler.handle(any(), any())).thenReturn(true);

        var result = handler.modify(entity, command);

        assertThat(result).isTrue();
        verify(provisionResponsesHandler).handle(eq(entity), argThat(it -> it.get(0).getContent().equals(provisionResponse)));
    }

    @Test
    void postActions_shouldInvokeProvisionResultHandler() {
        var entity = TransferProcess.Builder.newInstance().state(PROVISIONING.code()).build();
        var provisionResponse = ProvisionResponse.Builder.newInstance().inProcess(true).build();
        var command = new AddProvisionedResourceCommand("id", provisionResponse);

        handler.postActions(entity, command);

        verify(provisionResponsesHandler).postActions(eq(entity));
    }

    @Test
    void modify_shouldReturnFalse_whenProvisionResultHandlerCannotHandle() {
        var entity = TransferProcess.Builder.newInstance().state(PROVISIONING.code()).build();
        var provisionResponse = ProvisionResponse.Builder.newInstance().inProcess(true).build();
        var command = new AddProvisionedResourceCommand("id", provisionResponse);
        when(provisionResponsesHandler.handle(any(), any())).thenReturn(false);

        var result = handler.modify(entity, command);

        assertThat(result).isFalse();
    }
}
