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

package org.eclipse.dataspaceconnector.iam.daps;

import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.system.CoreServicesExtension;

import java.util.Set;

public class TestExtensions {

    public static ServiceExtension mockHttpClient(OkHttpClient client) {
        return new ServiceExtension() {
            @Override
            public Set<String> provides() {
                return Set.of(CoreServicesExtension.FEATURE_HTTP_CLIENT);
            }

            @Override
            public void initialize(ServiceExtensionContext context) {
                context.registerService(OkHttpClient.class, client);
            }
        };
    }

}
