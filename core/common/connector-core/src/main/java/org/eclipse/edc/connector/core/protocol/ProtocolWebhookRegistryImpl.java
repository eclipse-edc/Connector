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

package org.eclipse.edc.connector.core.protocol;

import org.eclipse.edc.spi.protocol.ProtocolWebhook;
import org.eclipse.edc.spi.protocol.ProtocolWebhookRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class ProtocolWebhookRegistryImpl implements ProtocolWebhookRegistry {
    private final Map<String, ProtocolWebhook> webhooks = new HashMap<>();

    @Override
    public void registerWebhook(String protocol, ProtocolWebhook webhook) {
        webhooks.put(protocol, webhook);
    }

    @Override
    public @Nullable ProtocolWebhook resolve(String protocol) {
        return webhooks.get(protocol);
    }
}
