/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.sample5.data.seeder;

import org.eclipse.dataspaceconnector.dataloading.AssetLoader;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

public class DataSeederServiceExtension implements ServiceExtension {

    private FakeSetup fakeSetup;
    @Inject
    private AssetLoader assetLoader;
    @Inject
    private ContractDefinitionStore contractDefinitionStore;

    @Override
    public String name() {
        return "Sample 5 Data Seeder";
    }

    @Override
    public void initialize(ServiceExtensionContext serviceExtensionContext) {
        var monitor = serviceExtensionContext.getMonitor();

        fakeSetup = new FakeSetup(monitor, assetLoader, contractDefinitionStore);
    }

    @Override
    public void start() {
        fakeSetup.setupAssets();
        fakeSetup.setupContractOffers();
    }

}
