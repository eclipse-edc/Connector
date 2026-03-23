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

package org.eclipse.edc.protocol.spi;

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves a {@link ProtocolWebhook} for a given participant context and protocol.
 */
@ExtensionPoint
public interface ProtocolWebhookResolver {

    /**
     * Resolves a {@link ProtocolWebhook} for a given participant context and protocol.
     *
     * @param participantContextId the participant context id
     * @param protocol             the protocol
     * @return the resolved {@link ProtocolWebhook}, or null if no webhook is found for the given participant context and protocol
     */
    @Nullable
    ProtocolWebhook getWebhook(String participantContextId, String protocol);
}
