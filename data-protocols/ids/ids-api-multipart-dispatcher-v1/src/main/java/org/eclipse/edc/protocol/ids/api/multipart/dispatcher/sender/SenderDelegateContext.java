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

package org.eclipse.edc.protocol.ids.api.multipart.dispatcher.sender;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.protocol.ids.spi.types.IdsId;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

/**
 * Contains services and context information required by sender delegate classes.
 */
public class SenderDelegateContext {
    private final IdsId connectorId;
    private final ObjectMapper objectMapper;
    private final TypeTransformerRegistry transformerRegistry;
    private final String idsWebhookAddress;

    public SenderDelegateContext(IdsId connectorId, ObjectMapper objectMapper,
                                 TypeTransformerRegistry transformerRegistry,
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

    public TypeTransformerRegistry getTransformerRegistry() {
        return transformerRegistry;
    }

    public String getIdsWebhookAddress() {
        return idsWebhookAddress;
    }
}
