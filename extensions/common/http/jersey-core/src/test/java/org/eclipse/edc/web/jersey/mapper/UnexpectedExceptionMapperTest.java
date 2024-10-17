/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - Initial implementation
 *
 */

package org.eclipse.edc.web.jersey.mapper;

import jakarta.ws.rs.NotAcceptableException;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.NotSupportedException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class UnexpectedExceptionMapperTest {

    private final Monitor monitor = mock(Monitor.class);
    private final UnexpectedExceptionMapper mapper = new UnexpectedExceptionMapper(monitor);

    @ParameterizedTest
    @ArgumentsSource(JakartaApiExceptions.class)
    void toResponse_jakartaExceptions(Throwable throwable, int expectedCode) {
        try (var response = mapper.toResponse(throwable)) {
            assertThat(response.getStatus()).isEqualTo(expectedCode);
            assertThat(response.getStatusInfo().getReasonPhrase()).isNotBlank();
            assertThat(response.getEntity()).isNull();
            verifyNoInteractions(monitor);
        }
    }

    @ParameterizedTest
    @ArgumentsSource(JavaExceptions.class)
    void toResponse_unexpectedExceptions(Throwable throwable, int expectedCode) {
        try (var response = mapper.toResponse(throwable)) {
            assertThat(response.getStatus()).isEqualTo(expectedCode);
            assertThat(response.getStatusInfo().getReasonPhrase()).isNotBlank();
            assertThat(response.getEntity()).isNull();
            verify(monitor).severe(anyString(), same(throwable));
        }
    }

    private static class JavaExceptions implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    Arguments.of(new IllegalArgumentException("foo"), 400),
                    Arguments.of(new NullPointerException("foo"), 500),
                    Arguments.of(new UnsupportedOperationException("foo"), 501),
                    Arguments.of(new IllegalStateException("foo"), 500)
            );
        }
    }

    private static class JakartaApiExceptions implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    Arguments.of(new NotAcceptableException("Not acceptable"), 406),
                    Arguments.of(new NotFoundException(), 404),
                    Arguments.of(new NotSupportedException(), 415),
                    Arguments.of(new NotAllowedException("any"), 405)
            );
        }
    }

}
