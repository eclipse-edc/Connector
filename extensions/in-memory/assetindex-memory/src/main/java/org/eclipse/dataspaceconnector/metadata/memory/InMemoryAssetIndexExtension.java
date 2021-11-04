/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.metadata.memory;

import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndexLoader;
import org.eclipse.dataspaceconnector.spi.asset.DataAddressResolver;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.Set;

public class InMemoryAssetIndexExtension implements ServiceExtension {
    private Monitor monitor;

    @Override
    public Set<String> provides() {
        return Set.of(AssetIndex.FEATURE, DataAddressResolver.FEATURE, AssetIndexLoader.FEATURE);
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();

        var service = new InMemoryAssetIndex(new CriterionToPredicateConverter());
        context.registerService(AssetIndex.class, service);
        context.registerService(AssetIndexLoader.class, service);
        context.registerService(DataAddressResolver.class, service);

        monitor.info("Initialized In-Memory Asset Index extension");
    }

    @Override
    public void start() {
        monitor.info("Started In-Memory Asset Index extension");
    }

    @Override
    public void shutdown() {
        monitor.info("Shutdown In-Memory Asset Index extension");
    }
}
