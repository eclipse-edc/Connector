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

package org.eclipse.edc.runtime.core.command;

import org.eclipse.edc.runtime.core.retry.RetryPolicyConfiguration;
import org.eclipse.edc.runtime.core.retry.RetryPolicyFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;

class RetryPolicyFactoryTest {

    @Test
    void shouldDeclareLoggingListeners_whenConfigured() {
        var configuration = RetryPolicyConfiguration.Builder.newInstance()
                .logOnAbort(true)
                .logOnRetryScheduled(true)
                .logOnRetry(true)
                .logOnRetriesExceeded(true)
                .logOnFailedAttempt(true)
                .build();

        var retryPolicy = RetryPolicyFactory.create(configuration, mock());

        var config = retryPolicy.getConfig();
        assertThat(config.getRetryListener()).isNotNull();
        assertThat(config.getRetryScheduledListener()).isNotNull();
        assertThat(config.getRetriesExceededListener()).isNotNull();
        assertThat(config.getFailedAttemptListener()).isNotNull();
        assertThat(config.getAbortListener()).isNotNull();
    }

}
