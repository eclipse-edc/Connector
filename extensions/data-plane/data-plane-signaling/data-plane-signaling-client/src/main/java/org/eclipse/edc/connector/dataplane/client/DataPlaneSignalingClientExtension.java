/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.client;

import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneClient;
import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneClientFactory;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

import static org.eclipse.edc.spi.CoreConstants.JSON_LD;

/**
 * This extension provides an implementation of {@link DataPlaneClient} compliant with the data plane signaling protocol
 */
@Extension(value = DataPlaneSignalingClientExtension.NAME)
public class DataPlaneSignalingClientExtension implements ServiceExtension {
    public static final String NAME = "Data Plane Signaling Client";

    @Inject
    private EdcHttpClient httpClient;

    @Inject
    private TypeManager typeManager;

    @Inject
    private TypeTransformerRegistry transformerRegistry;

    @Inject
    private JsonLd jsonLd;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public DataPlaneClientFactory dataPlaneClientFactory() {
        var mapper = typeManager.getMapper(JSON_LD);
        var signalingApiTypeTransformerRegistry = transformerRegistry.forContext("signaling-api");
        return instance -> new DataPlaneSignalingClient(httpClient, signalingApiTypeTransformerRegistry, jsonLd, mapper, instance);
    }
}


