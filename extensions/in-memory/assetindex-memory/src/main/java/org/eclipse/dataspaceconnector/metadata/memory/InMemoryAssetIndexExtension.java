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

import org.eclipse.dataspaceconnector.dataloading.AssetLoader;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.asset.DataAddressResolver;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.Set;

public class InMemoryAssetIndexExtension implements ServiceExtension {

    @Override
    public Set<String> provides() {
        return Set.of(AssetIndex.FEATURE, DataAddressResolver.FEATURE, AssetLoader.FEATURE);
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var service = new InMemoryAssetLoader(new CriterionToPredicateConverter());
        context.registerService(AssetIndex.class, service);
        context.registerService(AssetLoader.class, service);
        context.registerService(DataAddressResolver.class, service);
    }
}
