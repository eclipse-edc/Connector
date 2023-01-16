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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.core.base;

import org.eclipse.edc.boot.system.DefaultServiceExtensionContext;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.eclipse.edc.connector.core.base.RetryPolicyFactory.LOG_ON_ABORT;
import static org.eclipse.edc.connector.core.base.RetryPolicyFactory.LOG_ON_FAILED_ATTEMPT;
import static org.eclipse.edc.connector.core.base.RetryPolicyFactory.LOG_ON_RETRIES_EXCEEDED;
import static org.eclipse.edc.connector.core.base.RetryPolicyFactory.LOG_ON_RETRY;
import static org.eclipse.edc.connector.core.base.RetryPolicyFactory.LOG_ON_RETRY_SCHEDULED;
import static org.mockito.Mockito.mock;

class RetryPolicyFactoryTest {

    private final Monitor monitor = mock(Monitor.class);

    @Test
    void shouldDeclareLoggingListeners_whenConfigured() {
        var settings = Map.of(
                LOG_ON_RETRY, "true",
                LOG_ON_RETRY_SCHEDULED, "true",
                LOG_ON_RETRIES_EXCEEDED, "true",
                LOG_ON_FAILED_ATTEMPT, "true",
                LOG_ON_ABORT, "true"
        );
        var context = createContextWithConfig(settings);

        var retryPolicy = RetryPolicyFactory.create(context);

        var config = retryPolicy.getConfig();
        assertThat(config.getRetryListener()).isNotNull();
        assertThat(config.getRetryScheduledListener()).isNotNull();
        assertThat(config.getRetriesExceededListener()).isNotNull();
        assertThat(config.getFailedAttemptListener()).isNotNull();
        assertThat(config.getAbortListener()).isNotNull();
    }

    @NotNull
    private DefaultServiceExtensionContext createContextWithConfig(Map<String, String> config) {
        var context = new DefaultServiceExtensionContext(monitor, List.of(() -> ConfigFactory.fromMap(config)));
        context.initialize();
        return context;
    }
}
