/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.dataspaceconnector.test.e2e;

import org.eclipse.dataspaceconnector.dataloading.AssetLoader;
import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;

public class ControlPlaneTestExtension implements ServiceExtension {
    @Inject
    private WebService webService;

    @Inject
    private AssetLoader assetLoader;

    @Inject
    private ContractDefinitionStore contractDefinitionStore;

    @Inject
    private TransferProcessStore transferProcessStore;

    @Override
    public String name() {
        return "[TEST] Control Plane";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        webService.registerResource(new ControlPlaneTestController(context.getMonitor(), assetLoader, contractDefinitionStore, transferProcessStore));
    }
}
