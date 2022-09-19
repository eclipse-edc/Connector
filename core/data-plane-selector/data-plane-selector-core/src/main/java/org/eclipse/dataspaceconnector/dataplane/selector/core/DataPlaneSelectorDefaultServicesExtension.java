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

package org.eclipse.dataspaceconnector.dataplane.selector.core;

import org.eclipse.dataspaceconnector.dataplane.selector.store.DataPlaneInstanceStore;
import org.eclipse.dataspaceconnector.dataplane.selector.store.DefaultDataPlaneInstanceStore;
import org.eclipse.dataspaceconnector.spi.system.Provider;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;

/**
 * Provides default service implementations for fallback
 */
public class DataPlaneSelectorDefaultServicesExtension implements ServiceExtension {

    @Override
    public String name() {
        return "Data Plane Selector Default Services";
    }

    @Provider(isDefault = true)
    public DataPlaneInstanceStore instanceStore() {
        return new DefaultDataPlaneInstanceStore();
    }
}
