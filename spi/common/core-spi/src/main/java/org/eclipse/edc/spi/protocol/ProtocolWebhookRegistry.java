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

package org.eclipse.edc.spi.protocol;

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.jetbrains.annotations.Nullable;

/**
 * A registry for protocol webhooks.
 */
@ExtensionPoint
public interface ProtocolWebhookRegistry {

    /**
     * Register a webhook for a protocol.
     *
     * @param protocol The protocol
     * @param webhook  The webhook
     */
    void registerWebhook(String protocol, ProtocolWebhook webhook);

    /**
     * Resolve a webhook for a protocol.
     *
     * @param protocol The protocol
     * @return The webhook for the protocol, or null if no webhook is registered for the protocol
     */
    @Nullable
    ProtocolWebhook resolve(String protocol);
}
