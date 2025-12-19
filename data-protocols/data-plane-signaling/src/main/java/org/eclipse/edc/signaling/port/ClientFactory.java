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
import org.eclipse.edc.http.spi.ControlApiHttpClient;

import java.util.function.Supplier;

public class ClientFactory {

    private final ControlApiHttpClient httpClient;
    private final Supplier<ObjectMapper> objectMapperSupplier;

    public ClientFactory(ControlApiHttpClient httpClient, Supplier<ObjectMapper> objectMapperSupplier) {
        this.httpClient = httpClient;
        this.objectMapperSupplier = objectMapperSupplier;
    }

    public DataPlaneSignalingClient createClient(DataPlaneInstance instance) {
        return new DataPlaneSignalingClient(instance, httpClient, objectMapperSupplier);
    }
}
