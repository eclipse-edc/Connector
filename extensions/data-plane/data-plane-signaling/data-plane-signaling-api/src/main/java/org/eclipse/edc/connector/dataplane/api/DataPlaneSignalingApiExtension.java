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

package org.eclipse.edc.connector.dataplane.api;

import jakarta.json.Json;
import org.eclipse.edc.connector.api.signaling.transform.from.JsonObjectFromDataFlowResponseMessageTransformer;
import org.eclipse.edc.connector.api.signaling.transform.to.JsonObjectToDataFlowProvisionMessageTransformer;
import org.eclipse.edc.connector.api.signaling.transform.to.JsonObjectToDataFlowStartMessageTransformer;
import org.eclipse.edc.connector.api.signaling.transform.to.JsonObjectToDataFlowSuspendMessageTransformer;
import org.eclipse.edc.connector.api.signaling.transform.to.JsonObjectToDataFlowTerminateMessageTransformer;
import org.eclipse.edc.connector.dataplane.api.controller.v1.DataPlaneSignalingApiController;
import org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.transform.transformer.dspace.from.JsonObjectFromDataAddressDspaceTransformer;
import org.eclipse.edc.transform.transformer.dspace.to.JsonObjectToDataAddressDspaceTransformer;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;

import java.util.Map;

import static org.eclipse.edc.connector.dataplane.api.DataPlaneSignalingApiExtension.NAME;
import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants.DSP_NAMESPACE_V_2025_1;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

@Extension(NAME)
public class DataPlaneSignalingApiExtension implements ServiceExtension {

    public static final String NAME = "DataPlane Signaling API extension";

    @Inject
    private WebService webService;
    @Inject
    private TypeTransformerRegistry transformerRegistry;
    @Inject
    private DataPlaneManager dataPlaneManager;
    @Inject
    private TypeManager typeManager;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var factory = Json.createBuilderFactory(Map.of());

        var signalingApiTypeTransformerRegistry = transformerRegistry.forContext("signaling-api");
        signalingApiTypeTransformerRegistry.register(new JsonObjectToDataFlowStartMessageTransformer());
        signalingApiTypeTransformerRegistry.register(new JsonObjectToDataFlowProvisionMessageTransformer());
        signalingApiTypeTransformerRegistry.register(new JsonObjectToDataFlowSuspendMessageTransformer());
        signalingApiTypeTransformerRegistry.register(new JsonObjectToDataFlowTerminateMessageTransformer());
        signalingApiTypeTransformerRegistry.register(new JsonObjectToDataAddressDspaceTransformer(DSP_NAMESPACE_V_2025_1));
        signalingApiTypeTransformerRegistry.register(new JsonObjectFromDataFlowResponseMessageTransformer(factory));
        signalingApiTypeTransformerRegistry.register(new JsonObjectFromDataAddressDspaceTransformer(factory, typeManager, JSON_LD, DSP_NAMESPACE_V_2025_1));

        var controller = new DataPlaneSignalingApiController(signalingApiTypeTransformerRegistry,
                dataPlaneManager, context.getMonitor().withPrefix("SignalingAPI"));

        webService.registerResource(ApiContext.CONTROL, controller);
    }

}
