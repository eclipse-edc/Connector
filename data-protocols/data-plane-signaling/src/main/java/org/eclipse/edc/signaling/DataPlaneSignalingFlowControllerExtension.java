/*
 *  Copyright (c) 2026 Think-it GmbH
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

import org.eclipse.edc.connector.controlplane.transfer.spi.flow.DataFlowController;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DataAddressStore;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.signaling.logic.DataPlaneSignalingFlowController;
import org.eclipse.edc.signaling.port.ClientFactory;
import org.eclipse.edc.signaling.port.transformer.DataAddressToDspDataAddressTransformer;
import org.eclipse.edc.signaling.port.transformer.DataFlowResponseMessageToDataFlowResponseTransformer;
import org.eclipse.edc.signaling.port.transformer.DspDataAddressToDataAddressTransformer;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.configuration.context.ControlApiUrl;

import static org.eclipse.edc.signaling.DataPlaneSignalingFlowControllerExtension.NAME;


@Extension(NAME)
public class DataPlaneSignalingFlowControllerExtension implements ServiceExtension {

    public static final String NAME = "Data Plane Signaling Api";

    @Inject
    private TypeTransformerRegistry transformerRegistry;
    @Inject
    private ControlApiUrl controlApiUrl;
    @Inject
    private ClientFactory clientFactory;
    @Inject
    private DataPlaneSelectorService dataPlaneSelectorService;
    @Inject
    private DataAddressStore dataAddressStore;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public DataFlowController dataFlowController() {
        var typeTransformerRegistry = transformerRegistry.forContext("signaling-api");
        typeTransformerRegistry.register(new DataAddressToDspDataAddressTransformer());
        typeTransformerRegistry.register(new DataFlowResponseMessageToDataFlowResponseTransformer());
        typeTransformerRegistry.register(new DspDataAddressToDataAddressTransformer());
        return new DataPlaneSignalingFlowController(controlApiUrl, dataPlaneSelectorService,
                typeTransformerRegistry, clientFactory, dataAddressStore);
    }
}
