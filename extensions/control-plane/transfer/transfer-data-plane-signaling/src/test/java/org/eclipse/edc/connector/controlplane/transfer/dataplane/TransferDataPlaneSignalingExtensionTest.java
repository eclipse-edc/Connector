/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.controlplane.transfer.dataplane;

import org.eclipse.edc.connector.controlplane.transfer.dataplane.flow.DataPlaneSignalingFlowController;
import org.eclipse.edc.connector.controlplane.transfer.spi.flow.DataFlowManager;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(DependencyInjectionExtension.class)
class TransferDataPlaneSignalingExtensionTest {

    private final DataFlowManager dataFlowManager = mock();

    @BeforeEach
    void setup(ServiceExtensionContext context) {
        context.registerService(DataFlowManager.class, dataFlowManager);
    }

    @Test
    void initialize(ServiceExtensionContext context, TransferDataPlaneSignalingExtension extension) {
        extension.initialize(context);
        verify(dataFlowManager).register(isA(DataPlaneSignalingFlowController.class));
    }
}
