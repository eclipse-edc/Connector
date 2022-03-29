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

package org.eclipse.dataspaceconnector.iam.registrationservice.crawler;

import org.eclipse.dataspaceconnector.iam.did.spi.resolution.DidResolverRegistry;
import org.eclipse.dataspaceconnector.iam.did.spi.store.DidStore;
import org.eclipse.dataspaceconnector.iam.registrationservice.events.CrawlerEventPublisher;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;

/**
 * Stores parameters (such as the DID Type) and necessary objects (such as the IonClient or the DidStore)
 * for the crawler, so it is essentially a Holder which is passed through to the {@link CrawlerJob} by Quartz
 */
public class CrawlerContext {
    public static final String KEY = "edc:ion-crawler:config";
    private DidStore didStore;
    private Monitor monitor;
    private CrawlerEventPublisher publisher;
    private String ionHost;
    private String didTypes;
    private DidResolverRegistry resolverRegistry;
    private TypeManager typeManager;

    public DidStore getDidStore() {
        return didStore;
    }

    public Monitor getMonitor() {
        return monitor;
    }

    public CrawlerEventPublisher getPublisher() {
        return publisher;
    }

    public String getIonHost() {
        return ionHost;
    }

    public String getDidTypes() {
        return didTypes;
    }

    public DidResolverRegistry getResolverRegistry() {
        return resolverRegistry;
    }

    public TypeManager getTypeManager() {
        return typeManager;
    }

    public static final class Builder {
        private DidStore didStore;
        private Monitor monitor;
        private CrawlerEventPublisher publisher;
        private String ionHost;
        private String didTypes;
        private DidResolverRegistry resolverRegistry;
        private TypeManager typeManager;

        private Builder() {
        }

        public static Builder create() {
            return new Builder();
        }

        public Builder didStore(DidStore didStore) {
            this.didStore = didStore;
            return this;
        }

        public Builder monitor(Monitor monitor) {
            this.monitor = monitor;
            return this;
        }

        public Builder publisher(CrawlerEventPublisher publisher) {
            this.publisher = publisher;
            return this;
        }

        public Builder ionHost(String ionHost) {
            this.ionHost = ionHost;
            return this;
        }

        public Builder didTypes(String didTypes) {
            this.didTypes = didTypes;
            return this;
        }

        public Builder resolverRegistry(DidResolverRegistry resolverRegistry) {
            this.resolverRegistry = resolverRegistry;
            return this;
        }

        public Builder typeManager(TypeManager typeManager) {
            this.typeManager = typeManager;
            return this;
        }

        public CrawlerContext build() {
            CrawlerContext crawlerConfig = new CrawlerContext();
            crawlerConfig.didTypes = didTypes;
            crawlerConfig.ionHost = ionHost;
            crawlerConfig.publisher = publisher;
            crawlerConfig.typeManager = typeManager;
            crawlerConfig.didStore = didStore;
            crawlerConfig.monitor = monitor;
            crawlerConfig.resolverRegistry = resolverRegistry;
            return crawlerConfig;
        }
    }
}
