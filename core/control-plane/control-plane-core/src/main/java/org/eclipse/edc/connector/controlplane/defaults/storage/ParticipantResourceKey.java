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

package org.eclipse.edc.connector.controlplane.defaults.storage;

import java.util.Objects;

/**
 * Identity of a {@link org.eclipse.edc.participantcontext.spi.types.ParticipantResource} in the in-memory stores: resource ids are unique only within a participant
 * context, so the pair {@code (participantContextId, id)} is the key.
 *
 * @param participantContextId the id of the owning participant context.
 * @param id                   the resource id.
 */
public record ParticipantResourceKey(String participantContextId, String id) {

    public ParticipantResourceKey {
        Objects.requireNonNull(participantContextId, "participantContextId");
        Objects.requireNonNull(id, "id");
    }

    public static ParticipantResourceKey of(String participantContextId, String id) {
        return new ParticipantResourceKey(participantContextId, id);
    }
}
