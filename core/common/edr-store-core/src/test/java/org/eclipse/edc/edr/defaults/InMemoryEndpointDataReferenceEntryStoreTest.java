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

package org.eclipse.edc.edr.defaults;

import org.eclipse.edc.edr.spi.store.EndpointDataReferenceEntryIndex;
import org.eclipse.edc.edr.spi.store.EndpointDataReferenceEntryIndexTestBase;
import org.eclipse.edc.query.CriterionOperatorRegistryImpl;

public class InMemoryEndpointDataReferenceEntryStoreTest extends EndpointDataReferenceEntryIndexTestBase {

    private final InMemoryEndpointDataReferenceEntryIndex store = new InMemoryEndpointDataReferenceEntryIndex(CriterionOperatorRegistryImpl.ofDefaults());

    @Override
    protected EndpointDataReferenceEntryIndex getStore() {
        return store;
    }
}
