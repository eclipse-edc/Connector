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
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.samples.identity.did;

import org.eclipse.dataspaceconnector.iam.did.spi.store.DidStore;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

@Provides(DidStore.class)
public class DidDocumentStoreExtension implements ServiceExtension {
    @Override
    public void initialize(ServiceExtensionContext context) {
        var store = new InMemoryDidDocumentStore();
        context.registerService(DidStore.class, store);
    }
}
