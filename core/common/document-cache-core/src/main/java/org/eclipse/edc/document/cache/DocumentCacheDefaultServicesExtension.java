/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.document.cache;

import org.eclipse.edc.document.cache.resolver.HttpDocumentResolver;
import org.eclipse.edc.document.cache.spi.resolver.DocumentResolver;
import org.eclipse.edc.document.cache.spi.store.CachedDocumentStore;
import org.eclipse.edc.document.cache.store.InMemoryCachedDocumentStore;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.system.ServiceExtension;

import static org.eclipse.edc.document.cache.DocumentCacheDefaultServicesExtension.NAME;

@Extension(NAME)
public class DocumentCacheDefaultServicesExtension implements ServiceExtension {

    public static final String NAME = "Document Cache Default Services";

    @Inject
    private CriterionOperatorRegistry criterionOperatorRegistry;

    @Inject
    private EdcHttpClient httpClient;

    @Override
    public String name() {
        return NAME;
    }

    @Provider(isDefault = true)
    public CachedDocumentStore cachedDocumentStore() {
        return new InMemoryCachedDocumentStore(criterionOperatorRegistry);
    }

    @Provider(isDefault = true)
    public DocumentResolver documentResolver() {
        return new HttpDocumentResolver(httpClient);
    }
}
