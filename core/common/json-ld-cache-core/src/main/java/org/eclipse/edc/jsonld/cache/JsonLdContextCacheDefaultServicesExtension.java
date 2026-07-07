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

package org.eclipse.edc.jsonld.cache;

import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.jsonld.cache.resolver.HttpJsonLdContextResolver;
import org.eclipse.edc.jsonld.cache.spi.resolver.JsonLdContextResolver;
import org.eclipse.edc.jsonld.cache.spi.store.CachedJsonLdContextStore;
import org.eclipse.edc.jsonld.cache.store.InMemoryCachedJsonLdContextStore;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.system.ServiceExtension;

import static org.eclipse.edc.jsonld.cache.JsonLdContextCacheDefaultServicesExtension.NAME;

@Extension(NAME)
public class JsonLdContextCacheDefaultServicesExtension implements ServiceExtension {

    public static final String NAME = "JSON-LD Context Cache Default Services";

    @Inject
    private CriterionOperatorRegistry criterionOperatorRegistry;

    @Inject
    private EdcHttpClient httpClient;

    @Override
    public String name() {
        return NAME;
    }

    @Provider(isDefault = true)
    public CachedJsonLdContextStore cachedJsonLdContextStore() {
        return new InMemoryCachedJsonLdContextStore(criterionOperatorRegistry);
    }

    @Provider(isDefault = true)
    public JsonLdContextResolver jsonLdContextResolver() {
        return new HttpJsonLdContextResolver(httpClient);
    }
}
