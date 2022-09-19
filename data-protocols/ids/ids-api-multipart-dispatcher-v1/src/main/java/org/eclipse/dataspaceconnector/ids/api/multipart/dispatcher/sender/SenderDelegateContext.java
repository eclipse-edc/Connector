/*
 *  Copyright (c) 2022 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTransformerRegistry;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.Objects;

/**
 * Contains services and context information required by sender delegate classes.
 */
public class SenderDelegateContext {
    private final URI connectorId;
    private final ObjectMapper objectMapper;
    private final IdsTransformerRegistry transformerRegistry;
    private final String idsWebhookAddress;

    public SenderDelegateContext(@NotNull URI connectorId, @NotNull ObjectMapper objectMapper,
                                 @NotNull IdsTransformerRegistry transformerRegistry,
                                 @NotNull String idsWebhookAddress) {
        this.connectorId = Objects.requireNonNull(connectorId);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.transformerRegistry = Objects.requireNonNull(transformerRegistry);
        this.idsWebhookAddress = Objects.requireNonNull(idsWebhookAddress);
    }

    public URI getConnectorId() {
        return connectorId;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public IdsTransformerRegistry getTransformerRegistry() {
        return transformerRegistry;
    }

    public String getIdsWebhookAddress() {
        return idsWebhookAddress;
    }
}
