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

import org.eclipse.edc.catalog.crawler.cache.query.DspCatalogFetcher;
import org.eclipse.edc.catalog.crawler.logic.CatalogCrawlerManager;
import org.eclipse.edc.catalog.spi.CatalogCrawlerConfiguration;
import org.eclipse.edc.catalog.spi.FederatedCatalogCache;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.ProtocolRemoteMessageDispatcher;
import org.eclipse.edc.crawler.spi.TargetNodeDirectory;
import org.eclipse.edc.crawler.spi.TargetNodeFilter;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.participantcontext.spi.store.ParticipantContextStore;
import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.health.HealthCheckResult;
import org.eclipse.edc.spi.system.health.HealthCheckService;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

import static java.util.Optional.ofNullable;
import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants.DSP_TRANSFORMER_CONTEXT_V_2025_1;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

@Extension(value = CatalogCrawlerCoreExtension.NAME)
public class CatalogCrawlerCoreExtension implements ServiceExtension {

    public static final String NAME = "Catalog Crawler Core";

    @Configuration
    private CatalogCrawlerConfiguration catalogCrawlerConfiguration;

    @Inject
    private FederatedCatalogCache store;
    @Inject
    private TargetNodeDirectory directory;
    @Inject
    private ParticipantContextStore participantContextStore;
    @Inject
    private ProtocolRemoteMessageDispatcher messageDispatcher;
    @Inject
    private TypeManager typeManager;
    @Inject
    private TypeTransformerRegistry transformerRegistry;
    @Inject
    private JsonLd jsonLdService;
    @Inject(required = false)
    private TargetNodeFilter nodeFilter;
    @Inject(required = false)
    private HealthCheckService healthCheckService;

    private CatalogCrawlerManager manager;

    @Override
    public void initialize(ServiceExtensionContext context) {
        if (healthCheckService != null) {
            healthCheckService.addReadinessProvider(() -> HealthCheckResult.Builder.newInstance().component("Crawler Subsystem").build());
        }

        nodeFilter = ofNullable(nodeFilter).orElse(node -> !node.name().equals(context.getRuntimeId()));

        var monitor = context.getMonitor().withPrefix(CatalogCrawlerManager.class.getSimpleName());

        var catalogFetcher = new DspCatalogFetcher(messageDispatcher, monitor, typeManager.getMapper(JSON_LD),
                transformerRegistry.forContext(DSP_TRANSFORMER_CONTEXT_V_2025_1), jsonLdService);

        manager = CatalogCrawlerManager.Builder.newInstance()
                .monitor(monitor)
                .configuration(catalogCrawlerConfiguration)
                .store(store)
                .catalogFetcher(catalogFetcher)
                .nodeDirectory(directory)
                .nodeFilterFunction(nodeFilter)
                .participantContextStore(participantContextStore)
                .build();
    }

    @Override
    public void start() {
        manager.start();
    }

    @Override
    public void shutdown() {
        manager.stop();
    }

}
