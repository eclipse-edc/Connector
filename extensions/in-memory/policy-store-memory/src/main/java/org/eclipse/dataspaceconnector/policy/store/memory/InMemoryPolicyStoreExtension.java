/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.policy.store.memory;

import org.eclipse.dataspaceconnector.common.concurrency.LockManager;
import org.eclipse.dataspaceconnector.spi.policy.store.PolicyStore;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Provides an in-memory implementation of the {@link PolicyStore} for testing.
 */
@Provides(PolicyStore.class)
public class InMemoryPolicyStoreExtension implements ServiceExtension {

    @Override
    public String name() {
        return "In-Memory Policy Store";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        context.registerService(PolicyStore.class, new InMemoryPolicyStore(new LockManager(new ReentrantReadWriteLock(true))));
    }

}
