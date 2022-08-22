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

package org.eclipse.dataspaceconnector.boot.system.injection.lifecycle;

import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class StartPhaseTest extends PhaseTest {


    @Test
    void start() {
        ServiceExtension extension = mock(ServiceExtension.class);
        var rp = new StartPhase(new Phase(injector, container, context, monitor) {
        });
        when(container.getInjectionTarget()).thenReturn(extension);

        rp.start();

        verify(extension).start();
        verify(extension).name();
        verifyNoMoreInteractions(extension);

    }
}