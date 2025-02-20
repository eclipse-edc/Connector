/*
 *  Copyright (c) 2024 Mercedes-Benz Tech Innovation GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Mercedes-Benz Tech Innovation GmbH - Implement automatic Hashicorp Vault token renewal
 *
 */

package org.eclipse.edc.vault.hashicorp.client;

import dev.failsafe.Fallback;
import okhttp3.Request;
import okhttp3.Response;
import org.eclipse.edc.http.spi.FallbackFactory;

import static org.eclipse.edc.http.spi.FallbackFactories.retryWhenStatusIsNotIn;

/**
 * Implements a {@link Fallback}factory for requests executed against the Hashicorp Vault.
 *
 * @see <a href="https://developer.hashicorp.com/vault/api-docs">Hashicorp Vault Api</a> for more information on retryable error codes.
 */
public class HashicorpVaultClientFallbackFactory implements FallbackFactory {

    private static final int[] NON_RETRYABLE_STATUS_CODES = { 200, 204, 400, 403, 404, 405 };

    @Override
    public Fallback<Response> create(Request request) {
        return retryWhenStatusIsNotIn(NON_RETRYABLE_STATUS_CODES).create(request);
    }
}
