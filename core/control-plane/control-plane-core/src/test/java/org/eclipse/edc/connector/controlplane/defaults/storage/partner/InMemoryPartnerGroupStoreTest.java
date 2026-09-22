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

package org.eclipse.edc.connector.controlplane.defaults.storage.partner;

import org.eclipse.edc.connector.controlplane.partner.spi.store.PartnerGroupStore;
import org.eclipse.edc.connector.controlplane.partner.spi.testfixtures.store.PartnerGroupStoreTestBase;
import org.eclipse.edc.query.CriterionOperatorRegistryImpl;

class InMemoryPartnerGroupStoreTest extends PartnerGroupStoreTestBase {

    private final PartnerGroupStore store = new InMemoryPartnerGroupStore(CriterionOperatorRegistryImpl.ofDefaults());

    @Override
    protected PartnerGroupStore getStore() {
        return store;
    }
}
