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

import okhttp3.Request;
import org.eclipse.edc.http.spi.FallbackFactories;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mockStatic;

class HashicorpVaultHealthServiceFallbackFactoryTest {

    private static final int[] NON_RETRYABLE_STATUS_CODES = {200, 204, 400, 403, 404, 405};

    @Test
    void create_shouldInitializeWithCorrectStatusCodes() {
        try (var mockedFallbackFactories = mockStatic(FallbackFactories.class)) {
            mockedFallbackFactories.when(() -> FallbackFactories.retryWhenStatusIsNotIn(NON_RETRYABLE_STATUS_CODES)).thenCallRealMethod();

            new HashicorpVaultClientFallbackFactory().create(new Request.Builder().url("http://test.local").get().build());

            mockedFallbackFactories.verify(() -> FallbackFactories.retryWhenStatusIsNotIn(NON_RETRYABLE_STATUS_CODES));
        }
    }

}
