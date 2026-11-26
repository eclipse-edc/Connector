/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.runtime.core;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RuntimeCoreServicesExtensionTest {

    private final ServiceExtensionContext context = mock();
    private final RuntimeCoreServicesExtension extension = new RuntimeCoreServicesExtension();

    @BeforeAll
    static void setup() {
        GlobalOpenTelemetry.set(mock(OpenTelemetry.class));
    }

    @Test
    void shouldReportStartupStatus_basedOnContextReadiness() {
        when(context.isReady()).thenReturn(false);
        var healthCheckService = extension.healthCheckService(context);

        assertThat(healthCheckService.getStartupStatus().isHealthy()).isFalse();

        when(context.isReady()).thenReturn(true);

        assertThat(healthCheckService.getStartupStatus().isHealthy()).isTrue();
    }

    @Nested
    class SelectOpenTelemetryImpl {

        @Test
        void whenNoOpenTelemetry() {
            var openTelemetry = RuntimeCoreServicesExtension.selectOpenTelemetryImpl(emptyList());

            assertThat(openTelemetry.get()).isEqualTo(GlobalOpenTelemetry.get());
        }

        @Test
        void whenSingleOpenTelemetry() {
            var customOpenTelemetry = mock(OpenTelemetry.class);

            var openTelemetry = RuntimeCoreServicesExtension.selectOpenTelemetryImpl(List.of(customOpenTelemetry));

            assertThat(openTelemetry.get()).isSameAs(customOpenTelemetry);
        }

        @Test
        void whenSeveralOpenTelemetry() {
            var customOpenTelemetry1 = mock(OpenTelemetry.class);
            var customOpenTelemetry2 = mock(OpenTelemetry.class);

            assertThatThrownBy(() -> RuntimeCoreServicesExtension.selectOpenTelemetryImpl(List.of(customOpenTelemetry1, customOpenTelemetry2)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Found 2 OpenTelemetry implementations. Please provide only one OpenTelemetry service provider.");
        }
    }
}
