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

package org.eclipse.edc.jsonld.cache.store;

import org.eclipse.edc.jsonld.cache.spi.store.CachedJsonLdContextStore;
import org.eclipse.edc.jsonld.cache.spi.store.testfixtures.CachedJsonLdContextStoreTestBase;
import org.eclipse.edc.query.CriterionOperatorRegistryImpl;

class InMemoryCachedJsonLdContextStoreTest extends CachedJsonLdContextStoreTestBase {

    private final InMemoryCachedJsonLdContextStore store = new InMemoryCachedJsonLdContextStore(CriterionOperatorRegistryImpl.ofDefaults());

    @Override
    protected CachedJsonLdContextStore getStore() {
        return store;
    }
}
