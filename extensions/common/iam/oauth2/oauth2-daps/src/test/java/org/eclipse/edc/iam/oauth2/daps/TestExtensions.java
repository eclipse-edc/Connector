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
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.edc.iam.oauth2.daps;

import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

public class TestExtensions {

    public static ServiceExtension mockHttpClient(OkHttpClient client) {
        return new MockHttpExtension(client);
    }

    @Provides(OkHttpClient.class)
    private static class MockHttpExtension implements ServiceExtension {
        private final OkHttpClient client;

        MockHttpExtension(OkHttpClient client) {
            this.client = client;
        }

        @Override
        public void initialize(ServiceExtensionContext context) {
            context.registerService(OkHttpClient.class, client);
        }
    }
}
