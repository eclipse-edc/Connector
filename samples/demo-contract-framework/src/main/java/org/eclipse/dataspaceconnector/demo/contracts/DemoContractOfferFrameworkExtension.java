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
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.demo.contracts;

import org.eclipse.dataspaceconnector.contract.ContractOfferServiceImpl;
import org.eclipse.dataspaceconnector.metadata.memory.InMemoryAssetIndex;
import org.eclipse.dataspaceconnector.metadata.memory.InMemoryAssetIndexWriter;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.contract.ContractOfferFramework;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.protocol.web.WebService;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.Set;

public class DemoContractOfferFrameworkExtension implements ServiceExtension {
    private static final String NAME = "Demo Contract Offer Framework Extension";
    private static final String[] PROVIDES = {
            ContractOfferFramework.class.getName()
    };

    private Monitor monitor;

    @Override
    public final Set<String> provides() {
        return Set.of(PROVIDES);
    }

    @Override
    public Set<String> requires() {
        return Set.of(AssetIndex.FEATURE, "edc:webservice");
    }

    @Override
    public void initialize(final ServiceExtensionContext context) {
        monitor = context.getMonitor();

        var contractOfferFramework = new PublicContractOfferFramework();
        context.registerService(ContractOfferFramework.class, contractOfferFramework);

        var assetIndex = context.getService(AssetIndex.class);
        var webService = context.getService(WebService.class);
        var assetIndexWriter = new InMemoryAssetIndexWriter((InMemoryAssetIndex) assetIndex);
        var contractOfferService = new ContractOfferServiceImpl(contractOfferFramework, assetIndex);

        webService.registerController(new AssetIndexController(assetIndexWriter));
        webService.registerController(new ContractOfferController(contractOfferService));

        monitor.info(String.format("Initialized %s", NAME));
    }

    @Override
    public void start() {
        monitor.info(String.format("Started %s", NAME));
    }

    @Override
    public void shutdown() {
        monitor.info(String.format("Shutdown %s", NAME));
    }
}
