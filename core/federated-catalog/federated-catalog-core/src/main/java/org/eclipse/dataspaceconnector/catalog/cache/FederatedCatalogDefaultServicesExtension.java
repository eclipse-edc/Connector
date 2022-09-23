/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.dataspaceconnector.catalog.cache;

import org.eclipse.dataspaceconnector.catalog.directory.InMemoryNodeDirectory;
import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheNodeDirectory;
import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheStore;
import org.eclipse.dataspaceconnector.catalog.store.InMemoryFederatedCacheStore;
import org.eclipse.dataspaceconnector.common.concurrency.LockManager;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Provider;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Provides default service implementations for fallback
 */
public class FederatedCatalogDefaultServicesExtension implements ServiceExtension {

    @Override
    public String name() {
        return "Federated Catalog Default Services";
    }

    @Provider(isDefault = true)
    public FederatedCacheStore defaultCacheStore() {
        //todo: converts every criterion into a predicate that is always true. must be changed later!
        return new InMemoryFederatedCacheStore(criterion -> offer -> true, new LockManager(new ReentrantReadWriteLock()));
    }

    @Provider(isDefault = true)
    public FederatedCacheNodeDirectory defaultNodeDirectory() {
        return new InMemoryNodeDirectory();
    }
}
