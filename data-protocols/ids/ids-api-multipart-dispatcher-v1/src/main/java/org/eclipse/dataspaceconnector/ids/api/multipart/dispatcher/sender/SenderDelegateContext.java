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
import org.eclipse.dataspaceconnector.ids.spi.types.IdsId;

/**
 * Contains services and context information required by sender delegate classes.
 */
public class SenderDelegateContext {
    private final IdsId connectorId;
    private final ObjectMapper objectMapper;
    private final IdsTransformerRegistry transformerRegistry;
    private final String idsWebhookAddress;

    public SenderDelegateContext(IdsId connectorId, ObjectMapper objectMapper,
                                 IdsTransformerRegistry transformerRegistry,
                                 String idsWebhookAddress) {
        this.connectorId = connectorId;
        this.objectMapper = objectMapper;
        this.transformerRegistry = transformerRegistry;
        this.idsWebhookAddress = idsWebhookAddress;
    }

    public IdsId getConnectorId() {
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
