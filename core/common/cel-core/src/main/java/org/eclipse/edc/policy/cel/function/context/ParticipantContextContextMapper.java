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

package org.eclipse.edc.policy.cel.function.context;

import org.eclipse.edc.policy.engine.spi.ParticipantContextPolicyContext;
import org.eclipse.edc.spi.result.Result;

import java.util.Map;

/**
 * Exposes the participant context the policy is evaluated for as {@code ctx.partners}, a handle that partner-aware
 * CEL functions (e.g. {@code ctx.partners.byIdentity(ctx.agent.id)}) use to scope their lookups. Contexts built
 * without a participant context id get no handle, so expressions that do not use partners keep working.
 */
public class ParticipantContextContextMapper<C extends ParticipantContextPolicyContext> implements CelContextMapper<C> {

    public static final String PARTNERS_HANDLE = "partners";
    public static final String PARTICIPANT_CONTEXT_ID = "participantContextId";

    @Override
    public Result<Map<String, Object>> mapContext(C context) {
        var participantContextId = context.participantContextId();
        if (participantContextId == null) {
            return Result.success(Map.of());
        }
        return Result.success(Map.of(PARTNERS_HANDLE, Map.of(PARTICIPANT_CONTEXT_ID, participantContextId)));
    }
}
