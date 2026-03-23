/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
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

package org.eclipse.edc.test.e2e.managementapi.v5;

import jakarta.json.JsonArray;
import org.eclipse.edc.participantcontext.spi.service.ParticipantContextService;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContextState;

import java.util.Arrays;
import java.util.Map;

import static jakarta.json.Json.createArrayBuilder;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2;

public class TestFunction {

    public static final String PARTICIPANT_CONTEXT_ID = "test-participant";

    public static void createParticipant(ParticipantContextService participantContextService, String participantContextId) {
        createParticipant(participantContextService, participantContextId, Map.of());
    }

    public static void createParticipant(ParticipantContextService participantContextService, String participantContextId, Map<String, Object> properties) {
        var pc = participantContext(participantContextId, properties);

        participantContextService.createParticipantContext(pc)
                .orElseThrow(f -> new AssertionError(f.getFailureDetail()));
    }

    public static ParticipantContext participantContext(String participantContextId) {
        return participantContext(participantContextId, participantContextId, Map.of());
    }

    public static ParticipantContext participantContext(String participantContextId, Map<String, Object> properties) {
        return participantContext(participantContextId, participantContextId, properties);
    }

    public static ParticipantContext participantContext(String participantContextId, String identity, Map<String, Object> properties) {
        return ParticipantContext.Builder.newInstance()
                .participantContextId(participantContextId)
                .properties(properties)
                .identity(identity)
                .state(ParticipantContextState.ACTIVATED)
                .build();
    }

    public static JsonArray jsonLdContext() {
        return createArrayBuilder(Arrays.stream(jsonLdContextArray()).toList())
                .build();
    }

    public static String[] jsonLdContextArray() {
        return new String[]{
                EDC_CONNECTOR_MANAGEMENT_CONTEXT_V2,
        };
    }
}
