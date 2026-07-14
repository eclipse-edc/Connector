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

package org.eclipse.edc.document.cache.store;

import org.eclipse.edc.document.cache.spi.store.CachedDocumentStore;
import org.eclipse.edc.document.cache.spi.store.testfixtures.CachedDocumentStoreTestBase;
import org.eclipse.edc.query.CriterionOperatorRegistryImpl;

class InMemoryCachedDocumentStoreTest extends CachedDocumentStoreTestBase {

    private final InMemoryCachedDocumentStore store = new InMemoryCachedDocumentStore(CriterionOperatorRegistryImpl.ofDefaults());

    @Override
    protected CachedDocumentStore getStore() {
        return store;
    }
}
