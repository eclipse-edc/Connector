/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.catalog;

import org.eclipse.edc.catalog.spi.DataService;
import org.eclipse.edc.catalog.spi.DistributionResolver;
import org.eclipse.edc.connector.dataplane.selector.spi.store.DataPlaneInstanceStore;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;

@Extension(value = CatalogDefaultServicesExtension.NAME)
public class CatalogDefaultServicesExtension implements ServiceExtension {

    public static final String NAME = "Catalog Default Services";

    @Inject
    private DataService dataService;

    @Inject
    private DataPlaneInstanceStore dataPlaneInstanceStore;

    @Override
    public String name() {
        return NAME;
    }

    @Provider(isDefault = true)
    public DistributionResolver distributionResolver() {
        return new DefaultDistributionResolver(dataService, dataPlaneInstanceStore);
    }
}
