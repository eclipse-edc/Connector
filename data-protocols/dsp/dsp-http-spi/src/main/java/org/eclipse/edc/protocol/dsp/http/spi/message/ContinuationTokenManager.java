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

package org.eclipse.edc.protocol.dsp.http.spi.message;

import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.catalog.spi.Catalog;
import org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequestMessage;
import org.eclipse.edc.spi.result.Result;

/**
 * Provides functionality to manage the continuation token for resource pagination.
 */
public interface ContinuationTokenManager {

    /**
     * Apply the continuation token on the request message.
     *
     * @param requestMessage the request message.
     * @param continuationToken the continuation token.
     * @return the enriched {@link JsonObject} if operation succeeded, failure otherwise.
     */
    Result<JsonObject> applyQueryFromToken(JsonObject requestMessage, String continuationToken);

    /**
     * Create response decorator for specified request url.
     *
     * @param requestUrl the request url.
     * @return the {@link ResponseDecorator} for the url.
     */
    ResponseDecorator<CatalogRequestMessage, Catalog> createResponseDecorator(String requestUrl);
}
