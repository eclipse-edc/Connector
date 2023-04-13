/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.spi.catalog;

import org.eclipse.edc.catalog.spi.Catalog;
import org.eclipse.edc.catalog.spi.protocol.CatalogRequestMessage;
import org.eclipse.edc.service.spi.result.ServiceResult;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.jetbrains.annotations.NotNull;

/**
 * Mediates access to and modification of {@link Catalog}es on protocol messages reception.
 */
public interface CatalogProtocolService {

    /**
     * Returns a catalog given a {@link CatalogRequestMessage} and a {@link ClaimToken}
     *
     * @param message the request message.
     * @param token the claim token.
     * @return succeeded result with the {@link Catalog}, failed result otherwise.
     */
    @NotNull
    ServiceResult<Catalog> getCatalog(CatalogRequestMessage message, ClaimToken token);
}
