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
import org.eclipse.edc.connector.api.signaling.transform.from.JsonObjectFromDataFlowProvisionMessageTransformer;
import org.eclipse.edc.connector.api.signaling.transform.from.JsonObjectFromDataFlowStartMessageTransformer;
import org.eclipse.edc.connector.api.signaling.transform.from.JsonObjectFromDataFlowSuspendMessageTransformer;
import org.eclipse.edc.connector.api.signaling.transform.from.JsonObjectFromDataFlowTerminateMessageTransformer;
import org.eclipse.edc.connector.api.signaling.transform.to.JsonObjectToDataFlowResponseMessageTransformer;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.transform.transformer.dspace.from.JsonObjectFromDataAddressDspaceTransformer;
import org.eclipse.edc.transform.transformer.dspace.to.JsonObjectToDataAddressDspaceTransformer;

import java.util.Map;

import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants.DSP_NAMESPACE_V_2025_1;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

/**
 * This extension registers all the transformers relevant for the data plane signaling protocol
 */
@Extension(value = DataPlaneSignalingClientTransformExtension.NAME)
public class DataPlaneSignalingClientTransformExtension implements ServiceExtension {

    public static final String NAME = "Legacy Data Plane Signaling transform Client";

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
        var factory = Json.createBuilderFactory(Map.of());

        var signalingApiTransformerRegistry = transformerRegistry.forContext("signaling-api");
        signalingApiTransformerRegistry.register(new JsonObjectFromDataFlowStartMessageTransformer(factory, typeManager, JSON_LD));
        signalingApiTransformerRegistry.register(new JsonObjectFromDataFlowProvisionMessageTransformer(factory, typeManager, JSON_LD));
        signalingApiTransformerRegistry.register(new JsonObjectFromDataFlowSuspendMessageTransformer(factory));
        signalingApiTransformerRegistry.register(new JsonObjectFromDataFlowTerminateMessageTransformer(factory));
        signalingApiTransformerRegistry.register(new JsonObjectToDataFlowResponseMessageTransformer());
        signalingApiTransformerRegistry.register(new JsonObjectToDataAddressDspaceTransformer(DSP_NAMESPACE_V_2025_1));
        signalingApiTransformerRegistry.register(new JsonObjectFromDataAddressDspaceTransformer(factory, typeManager, JSON_LD, DSP_NAMESPACE_V_2025_1));
    }
}


