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

package org.eclipse.edc.participantcontext.connector.webhook;

import org.eclipse.edc.protocol.spi.DataspaceProfileContext;
import org.eclipse.edc.protocol.spi.DataspaceProfileContextRegistry;
import org.eclipse.edc.protocol.spi.ProtocolWebhook;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ParticipantWebhookResolverImplTest {

    @Test
    void getWebhook_returnsFormattedUrlWhenProfileMatches() {
        var registry = mock(DataspaceProfileContextRegistry.class);
        var profile = mock(DataspaceProfileContext.class);
        var proto = mock(ProtocolWebhook.class);

        when(profile.name()).thenReturn("protocolA");
        when(profile.webhook()).thenReturn(proto);
        when(proto.url()).thenReturn("https://example/%s");
        when(registry.getProfiles()).thenReturn(List.of(profile));

        var resolver = new ParticipantWebhookResolverImpl(registry);

        var result = resolver.getWebhook("participant-1", "protocolA");

        assertNotNull(result);
        assertEquals("https://example/participant-1", result.url());
        verify(proto).url();
    }

    @Test
    void getWebhook_returnsNullWhenNoMatchingProfile() {
        var registry = mock(DataspaceProfileContextRegistry.class);
        var profile = mock(DataspaceProfileContext.class);
        when(profile.name()).thenReturn("other");
        when(registry.getProfiles()).thenReturn(List.of(profile));

        var resolver = new ParticipantWebhookResolverImpl(registry);

        var result = resolver.getWebhook("participant-1", "protocolA");

        assertNull(result);
    }
}

