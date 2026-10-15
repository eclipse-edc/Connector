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

package org.eclipse.edc.spi.result;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.CONFLICT;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.NOT_FOUND;

class ServiceResultTest {

    @Test
    void verifyFromStorageResult() {
        var f = ServiceResult.from(StoreResult.notFound("test-message"));
        assertThat(f.reason()).isEqualTo(NOT_FOUND);
        assertThat(f.succeeded()).isFalse();

        var f2 = ServiceResult.from(StoreResult.alreadyExists("test-message"));
        assertThat(f2.reason()).isEqualTo(CONFLICT);
        assertThat(f2.succeeded()).isFalse();

        assertThat(ServiceResult.from(StoreResult.success("test-message"))).extracting(ServiceResult::succeeded).isEqualTo(true);
    }

    @Test
    void verifyFromStorageResult_conflicts() {
        assertThat(ServiceResult.from(StoreResult.conflict("test-message")).reason()).isEqualTo(CONFLICT);
        assertThat(ServiceResult.from(StoreResult.alreadyLeased("test-message")).reason()).isEqualTo(CONFLICT);
    }

    @Test
    void fromFailure_conflicts() {
        assertThat(ServiceResult.fromFailure(StoreResult.conflict("test-message")).reason()).isEqualTo(CONFLICT);
        assertThat(ServiceResult.fromFailure(StoreResult.alreadyLeased("test-message")).reason()).isEqualTo(CONFLICT);
        assertThat(ServiceResult.fromFailure(StoreResult.alreadyExists("test-message")).reason()).isEqualTo(CONFLICT);
    }

    @Test
    void fromFailure_withSuccessResult() {
        assertThatThrownBy(() -> ServiceResult.fromFailure(StoreResult.success())).isInstanceOf(IllegalArgumentException.class);
    }
}
