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

package org.eclipse.edc.statemachine;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StateMachineLivenessProviderTest {

    private final AbstractStateEntityManager<?, ?> manager = mock();
    private final StateMachineLivenessProvider provider = new StateMachineLivenessProvider("component", manager);

    @Test
    void shouldSucceed_whenManagerIsActive() {
        when(manager.isActive()).thenReturn(true);

        var result = provider.get();

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getComponent()).isEqualTo("component");
    }

    @Test
    void shouldFail_whenManagerIsNotActive() {
        when(manager.isActive()).thenReturn(false);

        var result = provider.get();

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureDetail()).contains("state machine is not running");
    }
}
