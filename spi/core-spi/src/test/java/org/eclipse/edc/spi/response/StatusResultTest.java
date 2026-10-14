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

package org.eclipse.edc.spi.response;

import org.eclipse.edc.spi.result.StoreResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.spi.response.ResponseStatus.CONFLICT;
import static org.eclipse.edc.spi.response.ResponseStatus.ERROR_RETRY;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;

class StatusResultTest {

    @Test
    void from_shouldMapStoreFailures() {
        assertThat(StatusResult.from(StoreResult.alreadyLeased("leased")).getFailure().status()).isEqualTo(ERROR_RETRY);
        assertThat(StatusResult.from(StoreResult.conflict("conflict")).getFailure().status()).isEqualTo(CONFLICT);
        assertThat(StatusResult.from(StoreResult.notFound("not found")).getFailure().status()).isEqualTo(FATAL_ERROR);
        assertThat(StatusResult.from(StoreResult.success("content")).succeeded()).isTrue();
    }

    @Test
    void conflict_shouldBeTrueOnlyForConflictFailures() {
        assertThat(StatusResult.failure(CONFLICT, "conflict").conflict()).isTrue();
        assertThat(StatusResult.failure(CONFLICT, "conflict").fatalError()).isFalse();
        assertThat(StatusResult.failure(ERROR_RETRY, "retry").conflict()).isFalse();
        assertThat(StatusResult.success().conflict()).isFalse();
    }
}
