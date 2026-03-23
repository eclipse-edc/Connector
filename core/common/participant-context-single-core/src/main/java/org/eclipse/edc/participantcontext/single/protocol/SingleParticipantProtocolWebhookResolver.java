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

package org.eclipse.edc.participantcontext.single.protocol;

import org.eclipse.edc.protocol.spi.DataspaceProfileContext;
import org.eclipse.edc.protocol.spi.DataspaceProfileContextRegistry;
import org.eclipse.edc.protocol.spi.ProtocolWebhook;
import org.eclipse.edc.protocol.spi.ProtocolWebhookResolver;
import org.jetbrains.annotations.Nullable;

public class SingleParticipantProtocolWebhookResolver implements ProtocolWebhookResolver {

    private final DataspaceProfileContextRegistry registry;

    public SingleParticipantProtocolWebhookResolver(DataspaceProfileContextRegistry registry) {
        this.registry = registry;
    }

    // we ignore the participantContextId since we only support a single participant context
    // TODO remove this once we have multiple participant context support in the protocol layer and remove the single participant protocol API
    @Override
    public @Nullable ProtocolWebhook getWebhook(String participantContextId, String protocol) {
        return registry.getProfiles().stream().filter(it -> it.name().equals(protocol))
                .map(DataspaceProfileContext::webhook).findAny().orElse(null);
    }
}
