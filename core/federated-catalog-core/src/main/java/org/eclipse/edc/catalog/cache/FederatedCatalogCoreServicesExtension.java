/*
 *  Copyright (c) 2025 Think-it GmbH
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

package org.eclipse.edc.catalog.cache;

import org.eclipse.edc.catalog.spi.CatalogConstants;
import org.eclipse.edc.catalog.spi.CatalogCrawlerConfiguration;
import org.eclipse.edc.catalog.spi.FederatedCatalogCache;
import org.eclipse.edc.catalog.spi.model.CatalogUpdateResponse;
import org.eclipse.edc.connector.controlplane.catalog.spi.Catalog;
import org.eclipse.edc.crawler.spi.CrawlerActionRegistry;
import org.eclipse.edc.crawler.spi.TargetNodeDirectory;
import org.eclipse.edc.crawler.spi.TargetNodeFilter;
import org.eclipse.edc.crawler.spi.model.ExecutionPlan;
import org.eclipse.edc.crawler.spi.model.UpdateResponse;
import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.health.HealthCheckResult;
import org.eclipse.edc.spi.system.health.HealthCheckService;

import static java.util.Optional.ofNullable;

@Extension(value = FederatedCatalogCoreServicesExtension.NAME)
public class FederatedCatalogCoreServicesExtension implements ServiceExtension {

    public static final String NAME = "Federated Catalog Core";

    @Configuration
    private CatalogCrawlerConfiguration catalogCrawlerConfiguration;

    @Inject(required = false)
    private ExecutionPlan executionPlan;
    @Inject
    private FederatedCatalogCache store;
    @Inject
    private CrawlerActionRegistry crawlerActionRegistry;
    @Inject
    private TargetNodeDirectory directory;
    @Inject(required = false)
    private TargetNodeFilter nodeFilter;
    @Inject(required = false)
    private HealthCheckService healthCheckService;

    private Monitor monitor;
    private ExecutionManager executionManager;

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();

        if (healthCheckService != null) {
            healthCheckService.addReadinessProvider(() -> HealthCheckResult.Builder.newInstance().component("Crawler Subsystem").build());
        }

        nodeFilter = ofNullable(nodeFilter).orElse(node -> !node.name().equals(context.getRuntimeId()));

        executionManager = ExecutionManager.Builder.newInstance()
                .monitor(context.getMonitor().withPrefix("ExecutionManager"))
                .configuration(catalogCrawlerConfiguration)
                .preExecutionTask(() -> {
                    store.deleteExpired();
                    store.expireAll();
                })
                .nodeQueryAdapterRegistry(crawlerActionRegistry)
                .onSuccess(this::persist)
                .nodeDirectory(directory)
                .nodeFilterFunction(nodeFilter)
                .build();
    }

    @Override
    public void start() {
        executionManager.executePlan(executionPlan);
    }

    @Override
    public void shutdown() {
        executionManager.shutdownPlan(executionPlan);
    }

    /**
     * inserts a particular {@link Catalog} in the {@link FederatedCatalogCache}
     *
     * @param updateResponse The response that contains the catalog
     */
    private void persist(UpdateResponse updateResponse) {
        if (updateResponse instanceof CatalogUpdateResponse catalogUpdateResponse) {
            var catalog = catalogUpdateResponse.getCatalog();
            catalog.getProperties().put(CatalogConstants.PROPERTY_ORIGINATOR, updateResponse.getSource());
            store.save(catalog);
        } else {
            monitor.warning("Expected a response of type %s but got %s. Will discard".formatted(CatalogUpdateResponse.class, updateResponse.getClass()));
        }
    }
}
