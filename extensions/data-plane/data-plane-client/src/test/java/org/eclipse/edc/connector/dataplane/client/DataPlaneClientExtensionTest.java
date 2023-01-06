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
import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneSelectorClient;
import org.eclipse.edc.connector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.injection.ObjectFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith(DependencyInjectionExtension.class)
class DataPlaneClientExtensionTest {

    @Test
    void verifyReturnEmbeddedClient(ServiceExtensionContext context, ObjectFactory factory) {
        context.registerService(DataPlaneManager.class, mock(DataPlaneManager.class));

        var extension = factory.constructInstance(DataPlaneClientExtension.class);

        var client = extension.dataPlaneClient(context);

        assertThat(client).isInstanceOf(EmbeddedDataPlaneClient.class);
    }

    @Test
    void verifyReturnRemoteClient(ServiceExtensionContext context, ObjectFactory factory) {
        context.registerService(EdcHttpClient.class, mock(EdcHttpClient.class));
        context.registerService(RetryPolicy.class, mock(RetryPolicy.class));
        context.registerService(DataPlaneSelectorClient.class, mock(DataPlaneSelectorClient.class));

        var extension = factory.constructInstance(DataPlaneClientExtension.class);

        var client = extension.dataPlaneClient(context);

        assertThat(client).isInstanceOf(RemoteDataPlaneClient.class);
    }
}
