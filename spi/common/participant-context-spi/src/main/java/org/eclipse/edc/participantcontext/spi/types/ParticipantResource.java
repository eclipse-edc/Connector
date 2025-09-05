/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.participantcontext.spi.types;

import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;

/**
 * Represents a participant resource for all resources that are owned by a {@link ParticipantContext}.
 */
public interface ParticipantResource {

    static QuerySpec.Builder queryByParticipantContextId(String participantContextId) {
        return QuerySpec.Builder.newInstance().filter(filterByParticipantContextId(participantContextId));
    }

    static Criterion filterByParticipantContextId(String participantContextId) {
        return new Criterion("participantContextId", "=", participantContextId);
    }

    /**
     * The {@link ParticipantContext} that this resource belongs to.
     */
    String getParticipantContextId();
}
