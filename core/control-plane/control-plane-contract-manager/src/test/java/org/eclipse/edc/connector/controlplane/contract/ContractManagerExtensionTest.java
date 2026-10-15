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

package org.eclipse.edc.connector.controlplane.contract;

import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.junit.extensions.TestExtensionContext;
import org.eclipse.edc.spi.system.health.HealthCheckResult;
import org.eclipse.edc.spi.system.health.HealthCheckService;
import org.eclipse.edc.spi.system.health.LivenessProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(DependencyInjectionExtension.class)
class ContractManagerExtensionTest {

    private final HealthCheckService healthCheckService = mock();

    @Test
    void initialize_shouldRegisterLivenessProviders(TestExtensionContext context, ObjectFactory factory) {
        context.registerService(HealthCheckService.class, healthCheckService);
        var extension = factory.constructInstance(ContractManagerExtension.class);

        extension.initialize(context);

        var captor = ArgumentCaptor.forClass(LivenessProvider.class);
        verify(healthCheckService, times(2)).addLivenessProvider(captor.capture());
        assertThat(captor.getAllValues()).map(LivenessProvider::get)
                .allMatch(HealthCheckResult::succeeded)
                .extracting(HealthCheckResult::getComponent)
                .containsExactlyInAnyOrder("Consumer Contract Negotiation State Machine", "Provider Contract Negotiation State Machine");
    }
}
