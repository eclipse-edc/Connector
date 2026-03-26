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
import org.eclipse.edc.protocol.spi.ProtocolWebhookResolver;
import org.jetbrains.annotations.Nullable;

public class ParticipantWebhookResolverImpl implements ProtocolWebhookResolver {

    private final DataspaceProfileContextRegistry registry;

    public ParticipantWebhookResolverImpl(DataspaceProfileContextRegistry registry) {
        this.registry = registry;
    }

    @Override
    public @Nullable ProtocolWebhook getWebhook(String participantContextId, String protocol) {
        return registry.getProfiles().stream().filter(it -> it.name().equals(protocol))
                .map(DataspaceProfileContext::webhook)
                .map(protocolWebhook -> wrap(participantContextId, protocolWebhook))
                .findAny().orElse(null);
    }

    private ProtocolWebhook wrap(String participantContextId, ProtocolWebhook protocolWebhook) {
        return () -> protocolWebhook.url().formatted(participantContextId);

    }

}
