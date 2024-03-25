/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.core.edr.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.boot.vault.InMemoryVault;
import org.eclipse.edc.connector.core.store.CriterionOperatorRegistryImpl;
import org.eclipse.edc.core.edr.defaults.InMemoryEndpointDataReferenceEntryIndex;
import org.eclipse.edc.core.edr.defaults.VaultEndpointDataReferenceCache;
import org.eclipse.edc.edr.spi.store.EndpointDataReferenceStore;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.eclipse.tractusx.edc.edr.spi.store.EndpointDataReferenceStoreTestBase;

import static org.mockito.Mockito.mock;

public class EndpointDataReferenceStoreImplTest extends EndpointDataReferenceStoreTestBase {

    private final InMemoryEndpointDataReferenceEntryIndex store = new InMemoryEndpointDataReferenceEntryIndex(CriterionOperatorRegistryImpl.ofDefaults());
    private final VaultEndpointDataReferenceCache cache = new VaultEndpointDataReferenceCache(new InMemoryVault(mock()), "", new ObjectMapper());
    private final EndpointDataReferenceStoreImpl endpointDataReferenceService = new EndpointDataReferenceStoreImpl(store, cache, new NoopTransactionContext());

    @Override
    protected EndpointDataReferenceStore getStore() {
        return endpointDataReferenceService;
    }
}
