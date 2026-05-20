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

package org.eclipse.edc.catalog.crawler;

import org.eclipse.edc.catalog.crawler.cache.query.QueryServiceImpl;
import org.eclipse.edc.catalog.crawler.logic.CrawlerActionRegistryImpl;
import org.eclipse.edc.catalog.crawler.store.InMemoryFederatedCatalogCache;
import org.eclipse.edc.catalog.crawler.store.InMemoryNodeDirectory;
import org.eclipse.edc.catalog.spi.FederatedCatalogCache;
import org.eclipse.edc.catalog.spi.QueryService;
import org.eclipse.edc.crawler.spi.CrawlerActionRegistry;
import org.eclipse.edc.crawler.spi.TargetNodeDirectory;
import org.eclipse.edc.query.CriterionOperatorRegistryImpl;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.util.concurrency.LockManager;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Provides default service implementations for fallback
 * Omitted {@link org.eclipse.edc.runtime.metamodel.annotation.Extension since there this module already contains {@code FederatedCatalogCacheExtension} }
 */
public class CatalogCrawlerDefaultServicesExtension implements ServiceExtension {

    public static final String NAME = "Federated Catalog Default Services";

    @Inject
    private FederatedCatalogCache store;

    @Override
    public String name() {
        return NAME;
    }

    @Provider(isDefault = true)
    public FederatedCatalogCache defaultCacheStore() {
        return new InMemoryFederatedCatalogCache(new LockManager(new ReentrantReadWriteLock()), CriterionOperatorRegistryImpl.ofDefaults());
    }

    @Provider(isDefault = true)
    public TargetNodeDirectory defaultNodeDirectory() {
        return new InMemoryNodeDirectory();
    }

    @Provider
    public QueryService defaultQueryEngine() {
        return new QueryServiceImpl(store);
    }

    @Provider
    public CrawlerActionRegistry crawlerActionRegistry() {
        return new CrawlerActionRegistryImpl();
    }

}
