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

import org.eclipse.edc.connector.controlplane.partner.spi.store.PartnerStore;
import org.eclipse.edc.connector.controlplane.partner.spi.testfixtures.store.PartnerStoreTestBase;
import org.eclipse.edc.query.CriterionOperatorRegistryImpl;

class InMemoryPartnerStoreTest extends PartnerStoreTestBase {

    private final PartnerStore store = new InMemoryPartnerStore(CriterionOperatorRegistryImpl.ofDefaults());

    @Override
    protected PartnerStore getStore() {
        return store;
    }
}
