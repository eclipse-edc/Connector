/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.catalog.crawler.store;


import org.eclipse.edc.catalog.spi.FederatedCatalogCache;
import org.eclipse.edc.catalog.spi.testfixtures.FederatedCatalogCacheTestBase;
import org.eclipse.edc.query.CriterionOperatorRegistryImpl;
import org.eclipse.edc.util.concurrency.LockManager;

import java.util.concurrent.locks.ReentrantReadWriteLock;

class InMemoryFederatedCatalogCacheTest extends FederatedCatalogCacheTestBase {

    private final InMemoryFederatedCatalogCache store = new InMemoryFederatedCatalogCache(new LockManager(new ReentrantReadWriteLock()), CriterionOperatorRegistryImpl.ofDefaults());
    
    @Override
    protected FederatedCatalogCache getStore() {
        return store;
    }
}
