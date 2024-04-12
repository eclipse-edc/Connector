/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.client;

import dev.failsafe.RetryPolicy;
import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith(DependencyInjectionExtension.class)
class DataPlaneClientExtensionTest {

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        context.registerService(TypeManager.class, new JacksonTypeManager());
    }

    @Test
    void verifyReturnEmbeddedClient(ServiceExtensionContext context, ObjectFactory factory) {
        context.registerService(DataPlaneManager.class, mock(DataPlaneManager.class));

        var extension = factory.constructInstance(DataPlaneClientExtension.class);

        var client = extension.dataPlaneClientFactory(context).createClient(createDataPlaneInstance());

        assertThat(client).isInstanceOf(EmbeddedDataPlaneClient.class);
    }

    @Test
    void verifyReturnRemoteClient(ServiceExtensionContext context, ObjectFactory factory) {
        context.registerService(DataPlaneManager.class, null);
        context.registerService(EdcHttpClient.class, mock(EdcHttpClient.class));
        context.registerService(RetryPolicy.class, mock(RetryPolicy.class));

        var extension = factory.constructInstance(DataPlaneClientExtension.class);

        var client = extension.dataPlaneClientFactory(context).createClient(createDataPlaneInstance());

        assertThat(client).isInstanceOf(RemoteDataPlaneClient.class);
    }

    private DataPlaneInstance createDataPlaneInstance() {
        return DataPlaneInstance.Builder.newInstance().url("http://any").build();
    }
}
