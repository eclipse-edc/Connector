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

import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.controlplane.transfer.spi.flow.DataFlowManager;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.signaling.logic.DataPlaneSignalingFlowController;
import org.eclipse.edc.signaling.port.ClientFactory;
import org.eclipse.edc.signaling.port.api.DataPlaneRegistrationApiController;
import org.eclipse.edc.signaling.port.api.DataPlaneTransferApiController;
import org.eclipse.edc.signaling.port.transformer.DataAddressToDspDataAddressTransformer;
import org.eclipse.edc.signaling.port.transformer.DataFlowResponseMessageToDataFlowResponseTransformer;
import org.eclipse.edc.signaling.port.transformer.DspDataAddressToDataAddressTransformer;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;
import org.eclipse.edc.web.spi.configuration.context.ControlApiUrl;

import static org.eclipse.edc.signaling.DataPlaneSignalingExtension.NAME;

@Extension(NAME)
public class DataPlaneSignalingExtension implements ServiceExtension {

    public static final String NAME = "Data Plane Signaling";
    private static final String DEFAULT_DATAPLANE_SELECTOR_STRATEGY = "random";

    @Setting(
            description = "Defines strategy for Data Plane instance selection in case Data Plane is not embedded in current runtime",
            defaultValue = DEFAULT_DATAPLANE_SELECTOR_STRATEGY,
            key = "edc.dataplane.client.selector.strategy"
    )
    private String selectionStrategy;

    @Inject
    private TypeTransformerRegistry transformerRegistry;
    @Inject
    private DataFlowManager dataFlowManager;
    @Inject
    private ControlApiUrl controlApiUrl;
    @Inject
    private DataPlaneSelectorService dataPlaneSelectorService;
    @Inject
    private WebService webService;
    @Inject
    private ClientFactory clientFactory;
    @Inject
    private TransferProcessService transferProcessService;

    @Override
    public void initialize(ServiceExtensionContext context) {
        webService.registerResource(ApiContext.CONTROL, new DataPlaneRegistrationApiController(dataPlaneSelectorService));
        webService.registerResource(ApiContext.CONTROL, new DataPlaneTransferApiController(transferProcessService));
        var typeTransformerRegistry = transformerRegistry.forContext("signaling-api");
        typeTransformerRegistry.register(new DataAddressToDspDataAddressTransformer());
        typeTransformerRegistry.register(new DataFlowResponseMessageToDataFlowResponseTransformer());
        typeTransformerRegistry.register(new DspDataAddressToDataAddressTransformer());
        dataFlowManager.register(10, new DataPlaneSignalingFlowController(controlApiUrl, dataPlaneSelectorService, selectionStrategy, typeTransformerRegistry, clientFactory));
    }

}
