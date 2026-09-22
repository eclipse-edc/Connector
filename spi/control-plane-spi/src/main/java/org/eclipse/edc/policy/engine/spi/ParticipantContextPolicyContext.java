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

package org.eclipse.edc.policy.engine.spi;

import org.jetbrains.annotations.Nullable;

/**
 * A {@link PolicyContext} that knows which participant context the evaluation runs for. Policy functions that need
 * tenant-scoped data (e.g. partners) use it to scope their lookups.
 */
public interface ParticipantContextPolicyContext extends PolicyContext {

    /**
     * The id of the participant context on whose behalf the policy is evaluated. May be null when the context was
     * built through a deprecated constructor that does not carry it.
     */
    @Nullable
    String participantContextId();
}
