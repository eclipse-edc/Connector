/*
 *  Copyright (c) 2025 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.signaling.port;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.signaling.spi.authorization.SignalingAuthorizationRegistry;

import java.util.function.Supplier;

public class ClientFactory {

    private final EdcHttpClient httpClient;
    private final Supplier<ObjectMapper> objectMapperSupplier;
    private final SignalingAuthorizationRegistry signalingAuthorizationRegistry;

    public ClientFactory(EdcHttpClient httpClient, Supplier<ObjectMapper> objectMapperSupplier, SignalingAuthorizationRegistry signalingAuthorizationRegistry) {
        this.httpClient = httpClient;
        this.objectMapperSupplier = objectMapperSupplier;
        this.signalingAuthorizationRegistry = signalingAuthorizationRegistry;
    }

    public DataPlaneSignalingClient createClient(DataPlaneInstance instance) {
        return new DataPlaneSignalingClient(instance, httpClient, objectMapperSupplier, signalingAuthorizationRegistry);
    }
}
