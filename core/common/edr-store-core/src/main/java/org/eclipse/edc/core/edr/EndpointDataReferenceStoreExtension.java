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

package org.eclipse.edc.core.edr;

import org.eclipse.edc.core.edr.store.EndpointDataReferenceStoreImpl;
import org.eclipse.edc.edr.spi.store.EndpointDataReferenceCache;
import org.eclipse.edc.edr.spi.store.EndpointDataReferenceEntryIndex;
import org.eclipse.edc.edr.spi.store.EndpointDataReferenceStore;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.transaction.spi.TransactionContext;

import static org.eclipse.edc.core.edr.EndpointDataReferenceStoreExtension.NAME;

@Extension(NAME)
public class EndpointDataReferenceStoreExtension implements ServiceExtension {

    protected static final String NAME = "Endpoint Data Reference Core Extension";

    @Inject
    private EndpointDataReferenceEntryIndex edrIndex;

    @Inject
    private EndpointDataReferenceCache edrCache;

    @Inject
    private TransactionContext transactionContext;

    @Provider
    public EndpointDataReferenceStore endpointDataReferenceService() {
        return new EndpointDataReferenceStoreImpl(edrIndex, edrCache, transactionContext);
    }
}
