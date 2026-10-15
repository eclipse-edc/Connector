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

import org.eclipse.edc.spi.system.health.HealthCheckResult;
import org.eclipse.edc.spi.system.health.LivenessProvider;

/**
 * Reports a state machine as not live when its loop is no longer running, e.g. because it exited after an
 * unrecoverable error, so that the runtime can be restarted.
 */
public class StateMachineLivenessProvider implements LivenessProvider {

    private final String component;
    private final AbstractStateEntityManager<?, ?> manager;

    public StateMachineLivenessProvider(String component, AbstractStateEntityManager<?, ?> manager) {
        this.component = component;
        this.manager = manager;
    }

    @Override
    public HealthCheckResult get() {
        return HealthCheckResult.Builder.newInstance()
                .component(component)
                .success(manager.isActive(), "state machine is not running")
                .build();
    }
}
