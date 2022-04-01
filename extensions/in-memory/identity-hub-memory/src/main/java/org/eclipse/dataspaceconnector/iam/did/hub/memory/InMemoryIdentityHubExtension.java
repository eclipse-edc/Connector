/*
 *  Copyright (c) 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.iam.did.hub.memory;

import org.eclipse.dataspaceconnector.iam.did.spi.hub.IdentityHubStore;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;


@Provides(IdentityHubStore.class)
public class InMemoryIdentityHubExtension implements ServiceExtension {

    @Override
    public String name() {
        return "In-Memory Identity Hub";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var store = new InMemoryIdentityHubStore();
        context.registerService(IdentityHubStore.class, store);
    }
}
