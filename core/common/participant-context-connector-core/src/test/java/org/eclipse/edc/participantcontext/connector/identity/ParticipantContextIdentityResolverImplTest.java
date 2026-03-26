/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.participantcontext.connector.identity;

import org.eclipse.edc.participantcontext.spi.service.ParticipantContextService;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.ServiceResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ParticipantContextIdentityResolverImplTest {

    private ParticipantContextService service;
    private Monitor monitor;
    private ParticipantContextIdentityResolverImpl resolver;

    @BeforeEach
    void setUp() {
        service = mock(ParticipantContextService.class);
        monitor = mock(Monitor.class);
        resolver = new ParticipantContextIdentityResolverImpl(service, monitor);
    }

    @Test
    void getParticipantId_returnsIdentity_whenContextFound() {
        var ctx = mock(ParticipantContext.class);
        when(ctx.getIdentity()).thenReturn("participant-1");
        when(service.getParticipantContext("ctx-1")).thenReturn(ServiceResult.success(ctx));

        var id = resolver.getParticipantId("ctx-1", "protocol");

        assertEquals("participant-1", id);
        verify(monitor, never()).warning(anyString());
    }

    @Test
    void getParticipantId_returnsNull_andLogsWarning_whenServiceFails() {
        when(service.getParticipantContext("bad")).thenReturn(ServiceResult.notFound("not found"));

        var id = resolver.getParticipantId("bad", "protocol");

        assertNull(id);

        var captor = ArgumentCaptor.forClass(String.class);
        verify(monitor).warning(captor.capture());
        assertTrue(captor.getValue().contains("Failed to resolve participant identity for context id bad"));
    }
}