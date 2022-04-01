/*
 *  Copyright (c) 2021 Copyright Holder (Catena-X Consortium)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Catena-X Consortium - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.logger;

import com.github.javafaker.Faker;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Collection of unit tests for {@link LoggerMonitor}
 */
public class LoggerMonitorTests {

    static Faker faker = new Faker();
    final String message = faker.lorem().sentence();
    final String extraParams = faker.lorem().sentence();
    TestLogHandler handler = new TestLogHandler();
    LoggerMonitor sut = new LoggerMonitor();

    @BeforeEach
    public void setUp() {
        Logger logger = Logger.getLogger(LoggerMonitor.class.getName());
        handler.setLevel(Level.ALL);
        //To prevent forwarding to other handlers.
        logger.setUseParentHandlers(false);
        logger.addHandler(handler);
        logger.setLevel(Level.ALL);
    }

    private static Stream<Arguments> provideLogDataWithErrors() {
        return Stream.of(
                Arguments.of(faker.lorem().sentence(), new Throwable[]{new RuntimeException()}),
                Arguments.of(faker.lorem().sentence(), new Throwable[]{new RuntimeException(), new Exception()})
        );
    }

    @ParameterizedTest(name = "{index} {1}")
    @MethodSource("provideLogDataWithErrors")
    public void loggedOnInfoLevel_WithErrors(String message, Throwable... errors) {

        //Act
        sut.info(() -> message, errors);

        //Assert
        assertLogWithErrors(message, Level.INFO, errors);
    }

    @ParameterizedTest(name = "{index} {1}")
    @MethodSource("provideLogDataWithErrors")
    public void loggedOnWarningLevel_WithErrors(String message, Throwable... errors) {

        //Act
        sut.warning(() -> message, errors);

        //Assert
        assertLogWithErrors(message, Level.WARNING, errors);
    }

    @ParameterizedTest(name = "{index} {1}")
    @MethodSource("provideLogDataWithErrors")
    public void loggedOnSevereLevel_WithErrors(String message, Throwable... errors) {

        //Act
        sut.severe(() -> message, errors);

        //Assert
        assertLogWithErrors(message, Level.SEVERE, errors);
    }

    @ParameterizedTest(name = "{index} {1}")
    @MethodSource("provideLogDataWithErrors")
    public void loggedOnDebugLevel_WithErrors(String message, Throwable... errors) {

        //Act
        sut.debug(() -> message, errors);

        //Assert
        assertLogWithErrors(message, Level.FINE, errors);
    }

    @Test
    public void loggedOnSevereLevel_WithParams() {

        // Arrange
        Map<String, Object> errors = Map.of(message, extraParams);

        //Act
        sut.severe(errors);

        //Assert
        assertThat(handler.getRecords()).extracting(
                        LogRecord::getMessage, LogRecord::getLevel, LogRecord::getThrown, LogRecord::getParameters)
                .containsExactly(
                        tuple(message, Level.SEVERE, null, new Object[]{extraParams}));
    }

    @Test
    public void loggedOnInfoLevel() {

        //Act
        sut.info(() -> message);

        assertLog(message, Level.INFO);
    }

    @Test
    public void loggedOnSevereLevel() {

        //Act
        sut.severe(() -> message);

        //Assert
        assertLog(message, Level.SEVERE);
    }

    @Test
    public void loggedOnWarningLevel() {

        //Act
        sut.warning(() -> message);

        //Assert
        assertLog(message, Level.WARNING);
    }

    @Test
    public void loggedOnDebugLevel() {

        //Act
        sut.debug(() -> message);

        //Assert
        assertLog(message, Level.FINE);
    }

    /**
     * Additional test to verify {@link LoggerMonitor} varargs null check is in place.
     */
    @Test
    public void loggedOnSevereLevel_WithNullVarArgs() {

        //Act
        sut.severe(() -> message, (Throwable) null);

        //Assert
        assertLog(message, Level.SEVERE);
    }

    private void assertLog(String message, Level level) {
        assertThat(handler.getRecords()).extracting(
                        LogRecord::getMessage, LogRecord::getLevel, LogRecord::getThrown)
                .containsExactly(
                        tuple(message, level, null));
    }

    private void assertLogWithErrors(String message, Level level, Throwable... errors) {
        assertThat(handler.getRecords()).extracting(
                        LogRecord::getMessage, LogRecord::getLevel, LogRecord::getThrown)
                .containsExactlyInAnyOrder(
                        Arrays.stream(errors).map(e -> tuple(message, level, e)).toArray(Tuple[]::new));
    }
}
