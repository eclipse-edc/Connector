/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.api.exception.mappers;

import jakarta.ws.rs.core.Response;
import org.eclipse.dataspaceconnector.api.exception.AuthenticationFailedException;
import org.eclipse.dataspaceconnector.api.exception.NotAuthorizedException;
import org.eclipse.dataspaceconnector.api.exception.ObjectExistsException;
import org.eclipse.dataspaceconnector.api.exception.ObjectNotFoundException;
import org.eclipse.dataspaceconnector.api.exception.ObjectNotModifiableException;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class EdcApiExceptionMapperTest {
    private EdcApiExceptionMapper mapper;

    public static Stream<Arguments> getArgs() {
        return Stream.of(Arguments.of(new ObjectNotModifiableException("1234", "test-type"), 423),
                Arguments.of(new AuthenticationFailedException(), 401),
                Arguments.of(new ObjectExistsException("test-id", "test-object-type"), 409),
                Arguments.of(new ObjectNotFoundException("test-object-id", "test-object-type"), 404),
                Arguments.of(new IllegalStateException("foo"), 503),
                Arguments.of(new IllegalArgumentException("foo"), 400),
                Arguments.of(new UnsupportedOperationException("foo"), 501),
                Arguments.of(new NullPointerException("foo"), 400),
                Arguments.of(new EdcException("foo"), 503),
                Arguments.of(new NotAuthorizedException(), 403));
    }

    @BeforeEach
    void setUp() {
        mapper = new EdcApiExceptionMapper();
    }

    @ParameterizedTest
    @MethodSource("getArgs")
    void toResponse(Throwable throwable, int expectedCode) {
        assertThat(mapper.toResponse(throwable)).extracting(Response::getStatus).isEqualTo(expectedCode);
    }
}