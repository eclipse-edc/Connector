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

package org.eclipse.edc.controlplane.tasks.defaults;

import org.eclipse.edc.controlplane.tasks.store.TaskStore;
import org.eclipse.edc.controlplane.tasks.store.TaskStoreTestBase;
import org.eclipse.edc.query.CriterionOperatorRegistryImpl;

class InMemoryTaskStoreTest extends TaskStoreTestBase {

    private final InMemoryTaskStore store = new InMemoryTaskStore(CriterionOperatorRegistryImpl.ofDefaults());

    @Override
    protected TaskStore getStore() {
        return store;
    }
}
