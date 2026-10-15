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

package org.eclipse.edc.connector.controlplane.transfer;

import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.junit.extensions.TestExtensionContext;
import org.eclipse.edc.spi.system.health.HealthCheckService;
import org.eclipse.edc.spi.system.health.LivenessProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(DependencyInjectionExtension.class)
class TransferManagerExtensionTest {

    private final HealthCheckService healthCheckService = mock();

    @Test
    void initialize_shouldRegisterLivenessProvider(TestExtensionContext context, ObjectFactory factory) {
        context.registerService(HealthCheckService.class, healthCheckService);
        var extension = factory.constructInstance(TransferManagerExtension.class);

        extension.initialize(context);

        var captor = ArgumentCaptor.forClass(LivenessProvider.class);
        verify(healthCheckService).addLivenessProvider(captor.capture());
        assertThat(captor.getValue().get().succeeded()).isTrue();
        assertThat(captor.getValue().get().getComponent()).isEqualTo("Transfer Process State Machine");
    }
}
