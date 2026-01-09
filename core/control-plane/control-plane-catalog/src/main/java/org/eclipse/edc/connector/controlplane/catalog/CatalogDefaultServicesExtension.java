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

package org.eclipse.edc.connector.controlplane.catalog;

import org.eclipse.edc.connector.controlplane.catalog.spi.DataServiceRegistry;
import org.eclipse.edc.connector.controlplane.catalog.spi.DistributionResolver;
import org.eclipse.edc.connector.controlplane.transfer.spi.flow.DataFlowController;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

@Extension(value = CatalogDefaultServicesExtension.NAME)
public class CatalogDefaultServicesExtension implements ServiceExtension {

    public static final String NAME = "Catalog Default Services";

    @Inject
    private DataFlowController dataFlowController;

    private DataServiceRegistry dataServiceRegistry;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        dataServiceRegistry = new DataServiceRegistryImpl();
    }

    @Provider
    public DataServiceRegistry dataServiceRegistry() {
        return dataServiceRegistry;
    }

    @Provider(isDefault = true)
    public DistributionResolver distributionResolver() {
        return new DefaultDistributionResolver(dataServiceRegistry, dataFlowController);
    }

}
