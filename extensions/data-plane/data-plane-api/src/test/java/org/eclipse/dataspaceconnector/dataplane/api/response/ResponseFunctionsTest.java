/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Amadeus - adds tests for success response
 *
 */

package org.eclipse.dataspaceconnector.dataplane.api.response;

import com.github.javafaker.Faker;
import org.eclipse.dataspaceconnector.dataplane.spi.response.TransferErrorResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;
import static org.eclipse.dataspaceconnector.dataplane.api.response.ResponseFunctions.success;
import static org.eclipse.dataspaceconnector.dataplane.api.response.ResponseFunctions.validationErrors;

class ResponseFunctionsTest {

    private static final Faker FAKER = new Faker();

    @Test
    void verifyValidationErrors() {
        var errorMessages = List.of(FAKER.internet().uuid(), FAKER.internet().uuid());
        var response = validationErrors(errorMessages);
        assertThat(response.getStatusInfo()).isEqualTo(BAD_REQUEST);

        var entity = response.getEntity();
        assertThat(response.getEntity()).isInstanceOf(TransferErrorResponse.class);
        var errorResponse = (TransferErrorResponse) entity;
        assertThat(errorResponse.getErrors()).containsExactly(errorMessages.toArray(new String[0]));
    }

    @Test
    void verifySuccess() {
        var data = FAKER.internet().uuid();
        var response = success(data);
        assertThat(response.getStatusInfo()).isEqualTo(OK);
        assertThat(response.getEntity()).asInstanceOf(STRING).isEqualTo(data);
    }
}
