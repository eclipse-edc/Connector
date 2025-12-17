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

package org.eclipse.edc.signaling;

import jakarta.json.Json;
import org.eclipse.edc.connector.api.signaling.transform.from.JsonObjectFromDataFlowProvisionMessageTransformer;
import org.eclipse.edc.connector.api.signaling.transform.from.JsonObjectFromDataFlowStartMessageTransformer;
import org.eclipse.edc.connector.api.signaling.transform.from.JsonObjectFromDataFlowSuspendMessageTransformer;
import org.eclipse.edc.connector.api.signaling.transform.from.JsonObjectFromDataFlowTerminateMessageTransformer;
import org.eclipse.edc.connector.api.signaling.transform.to.JsonObjectToDataFlowResponseMessageTransformer;
import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneClientFactory;
import org.eclipse.edc.http.spi.ControlApiHttpClient;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.signaling.port.DataPlaneSignalingClient;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

import java.util.Map;

import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.signaling.DataPlaneSignalingClientExtension.NAME;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

@Extension(NAME)
public class DataPlaneSignalingClientExtension implements ServiceExtension {

    public static final String NAME = "Data Plane Signaling client";
    private static final String CONTROL_CLIENT_SCOPE = "CONTROL_CLIENT_SCOPE";

    @Inject
    private ControlApiHttpClient httpClient;
    @Inject
    private TypeTransformerRegistry transformerRegistry;
    @Inject
    private TypeManager typeManager;
    @Inject
    private JsonLd jsonLd;

    @Provider
    public DataPlaneClientFactory dataPlaneClientFactory() {
        jsonLd.registerNamespace(VOCAB, EDC_NAMESPACE, CONTROL_CLIENT_SCOPE);

        var factory = Json.createBuilderFactory(Map.of());
        var signalingApiTypeTransformerRegistry = transformerRegistry.forContext("signaling-api");

        signalingApiTypeTransformerRegistry.register(new JsonObjectFromDataFlowStartMessageTransformer(factory, typeManager, JSON_LD));
        signalingApiTypeTransformerRegistry.register(new JsonObjectFromDataFlowProvisionMessageTransformer(factory, typeManager, JSON_LD));
        signalingApiTypeTransformerRegistry.register(new JsonObjectFromDataFlowSuspendMessageTransformer(factory));
        signalingApiTypeTransformerRegistry.register(new JsonObjectFromDataFlowTerminateMessageTransformer(factory));
        signalingApiTypeTransformerRegistry.register(new JsonObjectToDataFlowResponseMessageTransformer());

        return dataPlane -> new DataPlaneSignalingClient(dataPlane, httpClient,
                () -> typeManager.getMapper(JSON_LD), signalingApiTypeTransformerRegistry, jsonLd,
                CONTROL_CLIENT_SCOPE);
    }
}
