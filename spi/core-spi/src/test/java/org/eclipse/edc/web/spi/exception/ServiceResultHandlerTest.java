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

package org.eclipse.edc.web.spi.exception;

import org.eclipse.edc.spi.result.ServiceResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceResultHandlerTest {

    @Test
    void mapToException_shouldMapUnavailable() {
        var failure = ServiceResult.unavailable("entity is leased").getFailure();

        var exception = ServiceResultHandler.mapToException(failure, Object.class, "id");

        assertThat(exception).isInstanceOf(ServiceUnavailableException.class).hasMessage("entity is leased");
    }

    @Test
    void mapToException_shouldMapConflict() {
        var failure = ServiceResult.conflict("wrong state").getFailure();

        var exception = ServiceResultHandler.mapToException(failure, Object.class, "id");

        assertThat(exception).isInstanceOf(ObjectConflictException.class);
    }
}
