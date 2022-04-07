/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.catalog.cache;

import org.eclipse.dataspaceconnector.catalog.spi.CachedAsset;
import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheStore;
import org.eclipse.dataspaceconnector.catalog.spi.Loader;
import org.eclipse.dataspaceconnector.catalog.spi.model.UpdateResponse;

import java.util.Collection;

public class LoaderImpl implements Loader {
    private final FederatedCacheStore store;

    public LoaderImpl(FederatedCacheStore store) {
        this.store = store;
    }

    @Override
    public void load(Collection<UpdateResponse> responses) {

        for (var response : responses) {
            var catalog = response.getCatalog();
            catalog.getContractOffers().forEach(offer -> {
                offer.getAsset().getProperties().put(CachedAsset.PROPERTY_ORIGINATOR, response.getSource());
                store.save(offer);
            });
        }
    }
}
