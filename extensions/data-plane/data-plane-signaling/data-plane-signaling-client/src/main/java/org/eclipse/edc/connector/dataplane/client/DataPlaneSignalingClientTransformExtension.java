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

import jakarta.json.Json;
import org.eclipse.edc.connector.api.signaling.transform.from.JsonObjectFromDataFlowStartMessageTransformer;
import org.eclipse.edc.connector.api.signaling.transform.from.JsonObjectFromDataFlowSuspendMessageTransformer;
import org.eclipse.edc.connector.api.signaling.transform.from.JsonObjectFromDataFlowTerminateMessageTransformer;
import org.eclipse.edc.connector.api.signaling.transform.to.JsonObjectToDataFlowResponseMessageTransformer;
import org.eclipse.edc.core.transform.dspace.from.JsonObjectFromDataAddressTransformer;
import org.eclipse.edc.core.transform.dspace.to.JsonObjectToDataAddressTransformer;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

import java.util.Map;

import static org.eclipse.edc.spi.CoreConstants.JSON_LD;

/**
 * This extension registers all the transformers relevant for the data plane signaling protocol
 */
@Extension(value = DataPlaneSignalingClientTransformExtension.NAME)
public class DataPlaneSignalingClientTransformExtension implements ServiceExtension {

    public static final String NAME = "Data Plane Signaling transform Client";

    @Inject
    private TypeTransformerRegistry transformerRegistry;

    @Inject
    private TypeManager typeManager;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var mapper = typeManager.getMapper(JSON_LD);
        var factory = Json.createBuilderFactory(Map.of());

        var signalingApiTransformerRegistry = transformerRegistry.forContext("signaling-api");
        signalingApiTransformerRegistry.register(new JsonObjectFromDataFlowStartMessageTransformer(factory, mapper));
        signalingApiTransformerRegistry.register(new JsonObjectFromDataFlowSuspendMessageTransformer(factory));
        signalingApiTransformerRegistry.register(new JsonObjectFromDataFlowTerminateMessageTransformer(factory));
        signalingApiTransformerRegistry.register(new JsonObjectToDataFlowResponseMessageTransformer());
        signalingApiTransformerRegistry.register(new JsonObjectToDataAddressTransformer());
        signalingApiTransformerRegistry.register(new JsonObjectFromDataAddressTransformer(factory, mapper));
    }
}


