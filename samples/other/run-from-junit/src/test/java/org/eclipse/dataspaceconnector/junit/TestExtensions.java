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
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.junit;

import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

public class TestExtensions {

    public static ServiceExtension mockIamExtension(IdentityService identityService) {
        return new MockIamExtension(identityService);
    }

    @Provides(IdentityService.class)
    private static class MockIamExtension implements ServiceExtension {
        private final IdentityService identityService;

        MockIamExtension(IdentityService identityService) {
            this.identityService = identityService;
        }

        @Override
        public void initialize(ServiceExtensionContext context) {
            context.registerService(IdentityService.class, identityService);
        }
    }
}
