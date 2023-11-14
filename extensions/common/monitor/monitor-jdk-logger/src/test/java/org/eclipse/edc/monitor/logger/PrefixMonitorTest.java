/*
 *  Copyright (c) 2023 Mercedes-Benz Tech Innovation GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Mercedes-Benz Tech Innovation GmbH -  initial implementation
 *
 */

package org.eclipse.edc.monitor.logger;

import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.monitor.PrefixMonitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.function.Supplier;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

public class PrefixMonitorTest {

    private static final String TEST_PREFIX = "Test Prefix";
    private static final String TEST_MESSAGE = "Test Message";
    private static final String MESSAGE_FORMAT = "[%s]: %s";
    private static final String EXPECTED_MESSAGE = format(MESSAGE_FORMAT, TEST_PREFIX, TEST_MESSAGE);

    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<Supplier<String>> supplierParameterCaptor = ArgumentCaptor.forClass(Supplier.class);
    private final ArgumentCaptor<String> stringParameterCaptor = ArgumentCaptor.forClass(String.class);
    private final LoggerMonitor loggerMonitor = mock(LoggerMonitor.class);
    private final Monitor prefixMonitor = new PrefixMonitor(loggerMonitor, TEST_PREFIX);

    @BeforeEach
    public void beforeEach() {
        reset(loggerMonitor);
    }

    @Test
    void logSevere_byMessageSupplier_logIsDelegated() {
        prefixMonitor.severe(() -> TEST_MESSAGE);
        verify(loggerMonitor).severe(supplierParameterCaptor.capture());
        assertMessage(supplierParameterCaptor.getValue());
    }

    @Test
    void logSevere_byMessageString_logIsDelegated() {
        prefixMonitor.severe(TEST_MESSAGE);
        verify(loggerMonitor).severe(stringParameterCaptor.capture());
        assertMessage(stringParameterCaptor.getValue());
    }

    @Test
    void logWarning_byMessageSupplier_logIsDelegated() {
        prefixMonitor.warning(() -> TEST_MESSAGE);
        verify(loggerMonitor).warning(supplierParameterCaptor.capture());
        assertMessage(supplierParameterCaptor.getValue());
    }

    @Test
    void logWarning_byMessageString_logIsDelegated() {
        prefixMonitor.warning(TEST_MESSAGE);
        verify(loggerMonitor).warning(stringParameterCaptor.capture());
        assertMessage(stringParameterCaptor.getValue());
    }

    @Test
    void logInfo_byMessageSupplier_logIsDelegated() {
        prefixMonitor.info(() -> TEST_MESSAGE);
        verify(loggerMonitor).info(supplierParameterCaptor.capture());
        assertMessage(supplierParameterCaptor.getValue());
    }

    @Test
    void logInfo_byMessageString_logIsDelegated() {
        prefixMonitor.info(TEST_MESSAGE);
        verify(loggerMonitor).info(stringParameterCaptor.capture());
        assertMessage(stringParameterCaptor.getValue());
    }

    @Test
    void logDebug_byMessageSupplier_logIsDelegated() {
        prefixMonitor.debug(() -> TEST_MESSAGE);
        verify(loggerMonitor).debug(supplierParameterCaptor.capture());
        assertMessage(supplierParameterCaptor.getValue());
    }

    @Test
    void logDebug_byMessageString_logIsDelegated() {
        prefixMonitor.debug(TEST_MESSAGE);
        verify(loggerMonitor).debug(stringParameterCaptor.capture());
        assertMessage(stringParameterCaptor.getValue());
    }

    private void assertMessage(Supplier<String> messageSupplier) {
        assertMessage(messageSupplier.get());
    }

    private void assertMessage(String messageString) {
        assertThat(messageString).isEqualTo(EXPECTED_MESSAGE);
    }
}
