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

package org.eclipse.edc.federatedcatalog.end2end;


import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneClientFactory;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.dataplane.selector.spi.store.DataPlaneInstanceStore;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

public class DataplaneInstanceRegistrationExtension implements ServiceExtension {

    @Inject
    private DataPlaneInstanceStore dataPlaneInstanceStore;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var dpi = DataPlaneInstance.Builder.newInstance()
                .id("test-instance")
                .allowedDestType("test-dest-type")
                .allowedSourceType("test-src-type")
                .url("http://test.local")
                .build();
        dataPlaneInstanceStore.save(dpi);
    }

    @Provider
    public DataPlaneClientFactory createDataPlaneClientFactory() {
        return dataPlaneInstance -> null;
    }
}
