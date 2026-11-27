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

package org.eclipse.edc.controlplane;

/**
 * The feature that selects how the state machines of contract negotiations and transfer processes are driven. The
 * polling-based managers are bound to {@link #POLLING}, they are also loaded if {@link #FEATURE_KEY} is not configured.
 * The task-based state machine, which uses NATS for message passing, is bound to {@link #TASK} and is only loaded if
 * {@link #FEATURE_KEY} is set to that value.
 */
public final class StateMachineFeature {

    public static final String FEATURE_KEY = "edc.feature.statemachine";
    public static final String POLLING = "polling";
    public static final String TASK = "task";

    private StateMachineFeature() {
    }
}
