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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - Initial implementation
 *
 */

package org.eclipse.edc.connector.dataplane.selector.core;

import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Extension;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.connector.dataplane.selector.spi.store.DataPlaneInstanceStore;
import org.eclipse.edc.connector.dataplane.selector.store.InMemoryDataPlaneInstanceStore;
import org.eclipse.edc.spi.system.ServiceExtension;

/**
 * Provides default service implementations for fallback
 * Omitted {@link Extension} since this module contains the extension {@link DataPlaneSelectorExtension}
 */
public class DataPlaneSelectorDefaultServicesExtension implements ServiceExtension {

    public static final String NAME = "Data Plane Selector Default Services";

    @Override
    public String name() {
        return NAME;
    }

    @Provider(isDefault = true)
    public DataPlaneInstanceStore instanceStore() {
        return new InMemoryDataPlaneInstanceStore();
    }
}
