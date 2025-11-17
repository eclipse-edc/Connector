/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
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

package org.eclipse.edc.participantcontext.spi.types;

import java.util.Arrays;

/**
 * The state a {@link ParticipantContext} entry is in.
 */
public enum ParticipantContextState {
    /**
     * The {@link ParticipantContext} was created in the database, but is not yet operational.
     */
    CREATED(100),
    /**
     * The {@link ParticipantContext} is operational and can be used.
     */
    ACTIVATED(200),
    /**
     * The {@link ParticipantContext} is disabled and cannot be used currently.
     */
    DEACTIVATED(300);

    private final int code;

    ParticipantContextState(int code) {
        this.code = code;
    }

    public static ParticipantContextState from(int code) {
        return Arrays.stream(values()).filter(pcs -> pcs.code == code).findFirst().orElse(null);
    }

    public int code() {
        return code;
    }
}
