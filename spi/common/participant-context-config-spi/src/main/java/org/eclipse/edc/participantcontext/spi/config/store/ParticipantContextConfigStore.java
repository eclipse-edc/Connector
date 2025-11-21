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

package org.eclipse.edc.participantcontext.spi.config.store;

import org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfiguration;
import org.jetbrains.annotations.Nullable;

/**
 * Store for participant context configurations.
 */
public interface ParticipantContextConfigStore {

    /**
     * Saves the configuration for a given participant context.
     *
     * @param config the configuration to save
     */
    void save(ParticipantContextConfiguration config);

    /**
     * Retrieves the configuration for a given participant context.
     *
     * @param participantContextId the participant context identifier
     * @return the configuration, or null if not found
     */
    @Nullable
    ParticipantContextConfiguration get(String participantContextId);
}
