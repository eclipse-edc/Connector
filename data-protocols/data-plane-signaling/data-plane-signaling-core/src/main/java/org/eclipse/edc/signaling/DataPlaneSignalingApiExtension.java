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

import org.eclipse.edc.api.auth.spi.AuthorizationService;
import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.participantcontext.single.spi.SingleParticipantContextSupplier;
import org.eclipse.edc.participantcontext.spi.types.ParticipantResource;
import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.signaling.port.api.DataPlaneRegistrationApiV4Controller;
import org.eclipse.edc.signaling.port.api.DataPlaneTransferApiController;
import org.eclipse.edc.signaling.port.api.DataPlaneTransferAuthorizationFilter;
import org.eclipse.edc.signaling.port.api.v5.DataPlaneRegistrationApiV5Controller;
import org.eclipse.edc.signaling.port.transformer.DataAddressToDspDataAddressTransformer;
import org.eclipse.edc.signaling.port.transformer.DataFlowStatusMessageToDataFlowResponseTransformer;
import org.eclipse.edc.signaling.port.transformer.DspDataAddressToDataAddressTransformer;
import org.eclipse.edc.signaling.spi.authorization.SignalingAuthorizationRegistry;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.apiversion.ApiVersionService;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;
import org.eclipse.edc.web.spi.configuration.PortMapping;
import org.eclipse.edc.web.spi.configuration.PortMappingRegistry;

import java.io.IOException;

import static org.eclipse.edc.participantcontext.spi.types.ParticipantResource.queryByParticipantContextId;
import static org.eclipse.edc.signaling.DataPlaneSignalingApiExtension.NAME;

@Extension(NAME)
public class DataPlaneSignalingApiExtension implements ServiceExtension {

    public static final String NAME = "Data Plane Signaling Api";

    private static final String API_VERSION_JSON_FILE = "signaling-api-version.json";

    @Configuration
    private SignalingApiConfiguration apiConfiguration;

    @Inject
    private PortMappingRegistry portMappingRegistry;
    @Inject
    private DataPlaneSelectorService dataPlaneSelectorService;
    @Inject
    private WebService webService;
    @Inject
    private TransferProcessService transferProcessService;
    @Inject
    private TypeTransformerRegistry transformerRegistry;
    @Inject
    private ApiVersionService apiVersionService;
    @Inject
    private SignalingAuthorizationRegistry signalingAuthorizationRegistry;

    @Inject(required = false)
    private SingleParticipantContextSupplier participantContextSupplier;

    @Inject(required = false)
    private AuthorizationService authorizationService;

    @Inject
    private Monitor monitor;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var portMapping = new PortMapping(ApiContext.SIGNALING, apiConfiguration.port(), apiConfiguration.path());
        portMappingRegistry.register(portMapping);

        var typeTransformerRegistry = transformerRegistry.forContext("signaling-api");
        typeTransformerRegistry.register(new DataAddressToDspDataAddressTransformer());
        typeTransformerRegistry.register(new DataFlowStatusMessageToDataFlowResponseTransformer());
        typeTransformerRegistry.register(new DspDataAddressToDataAddressTransformer());

        // if running in single participant mode, the DataPlaneRegistrationApiV4Controller will be registered
        if (participantContextSupplier != null) {
            webService.registerResource(ApiContext.MANAGEMENT, new DataPlaneRegistrationApiV4Controller(dataPlaneSelectorService, participantContextSupplier));
        } else {
            // if multi participants mode the authorization service should be present
            if (authorizationService != null) {
                authorizationService.addLookupFunction(DataPlaneInstance.class, this::findDataPlaneInstance);
                webService.registerResource(ApiContext.MANAGEMENT, new DataPlaneRegistrationApiV5Controller(dataPlaneSelectorService, authorizationService));
            } else {
                throw new EdcException("Missing AuthorizationService which is required when running in multi participant mode.");
            }
        }

        webService.registerResource(ApiContext.SIGNALING, new DataPlaneTransferAuthorizationFilter(signalingAuthorizationRegistry, transferProcessService, dataPlaneSelectorService));
        webService.registerResource(ApiContext.SIGNALING, new DataPlaneTransferApiController(transferProcessService,
                typeTransformerRegistry, context.getMonitor().withPrefix("DataPlaneTransferApiController")));

        try (var versionContent = getClass().getClassLoader().getResourceAsStream(API_VERSION_JSON_FILE)) {
            apiVersionService.registerVersionInfo(ApiContext.SIGNALING, versionContent);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private ParticipantResource findDataPlaneInstance(String ownerId, String dataplaneId) {
        return dataPlaneSelectorService.search(queryByParticipantContextId(ownerId)
                        .filter(new Criterion("id", "=", dataplaneId))
                        .build()
                )
                .map(it -> it.stream().findFirst().orElse(null))
                .orElse(f -> null);
    }

}
