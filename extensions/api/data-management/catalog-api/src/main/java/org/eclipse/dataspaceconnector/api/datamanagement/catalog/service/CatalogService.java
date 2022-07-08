/*
 *  Copyright (c) 2020 - 2022 Bayerische Motoren Werke Aktiengesellschaft
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.api.datamanagement.catalog.service;

import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.types.domain.catalog.Catalog;

import java.util.concurrent.CompletableFuture;

public interface CatalogService {
    /**
     * Return the catalog of the passed provider url
     *
     * @param providerUrl the url of the provider
     * @return the provider's catalog
     */
    CompletableFuture<Catalog> getByProviderUrl(String providerUrl, QuerySpec spec);
}
