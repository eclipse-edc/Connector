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

package org.eclipse.edc.catalog.cache;

import org.eclipse.edc.catalog.cache.crawler.CrawlerActionRegistryImpl;
import org.eclipse.edc.catalog.cache.query.QueryServiceImpl;
import org.eclipse.edc.catalog.crawler.RecurringExecutionPlan;
import org.eclipse.edc.catalog.directory.InMemoryNodeDirectory;
import org.eclipse.edc.catalog.spi.CatalogCrawlerConfiguration;
import org.eclipse.edc.catalog.spi.FederatedCatalogCache;
import org.eclipse.edc.catalog.spi.QueryService;
import org.eclipse.edc.catalog.store.InMemoryFederatedCatalogCache;
import org.eclipse.edc.crawler.spi.CrawlerActionRegistry;
import org.eclipse.edc.crawler.spi.TargetNodeDirectory;
import org.eclipse.edc.crawler.spi.model.ExecutionPlan;
import org.eclipse.edc.query.CriterionOperatorRegistryImpl;
import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.util.concurrency.LockManager;

import java.time.Duration;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Provides default service implementations for fallback
 * Omitted {@link org.eclipse.edc.runtime.metamodel.annotation.Extension since there this module already contains {@code FederatedCatalogCacheExtension} }
 */
public class FederatedCatalogDefaultServicesExtension implements ServiceExtension {

    public static final String NAME = "Federated Catalog Default Services";

    @Configuration
    private CatalogCrawlerConfiguration catalogCrawlerConfiguration;

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

    @Provider(isDefault = true)
    public ExecutionPlan createRecurringExecutionPlan(ServiceExtensionContext context) {
        var monitor = context.getMonitor();
        catalogCrawlerConfiguration.checkPeriodSeconds().ifPresent(monitor::warning);
        return new RecurringExecutionPlan(
                Duration.ofSeconds(catalogCrawlerConfiguration.periodSeconds()),
                Duration.ofSeconds(catalogCrawlerConfiguration.delaySeconds()),
                monitor);
    }
}
