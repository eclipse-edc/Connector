/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
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

package org.eclipse.edc.iam.decentralizedclaims.core.scope.defaults;

import org.eclipse.edc.iam.decentralizedclaims.spi.scope.store.DcpScopeStore;
import org.eclipse.edc.iam.decentralizedclaims.spi.scope.store.DcpScopeStoreTestBase;
import org.eclipse.edc.query.CriterionOperatorRegistryImpl;


public class InMemoryDcpScopeStoreTest extends DcpScopeStoreTestBase {

    private final InMemoryDcpScopeStore store = new InMemoryDcpScopeStore(CriterionOperatorRegistryImpl.ofDefaults());

    @Override
    protected DcpScopeStore getStore() {
        return store;
    }
}
