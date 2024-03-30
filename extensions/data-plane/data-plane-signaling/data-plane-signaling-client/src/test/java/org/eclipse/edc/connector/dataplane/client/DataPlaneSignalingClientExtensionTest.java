/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.dataplane.client;

import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(DependencyInjectionExtension.class)
class DataPlaneSignalingClientExtensionTest {

    @Test
    void verifyDataPlaneClientFactory(ServiceExtensionContext context, ObjectFactory factory) {
        context.registerService(DataPlaneManager.class, null);
        var extension = factory.constructInstance(DataPlaneSignalingClientExtension.class);

        var client = extension.dataPlaneClientFactory(context).createClient(createDataPlaneInstance());

        assertThat(client).isInstanceOf(DataPlaneSignalingClient.class);
    }

    @Test
    void verifyDataPlaneClientFactory_withEmbedded(ServiceExtensionContext context, DataPlaneSignalingClientExtension extension) {
        var client = extension.dataPlaneClientFactory(context).createClient(createDataPlaneInstance());

        assertThat(client).isInstanceOf(EmbeddedDataPlaneClient.class);
    }


    private DataPlaneInstance createDataPlaneInstance() {
        return DataPlaneInstance.Builder.newInstance().url("http://any").build();
    }
}
