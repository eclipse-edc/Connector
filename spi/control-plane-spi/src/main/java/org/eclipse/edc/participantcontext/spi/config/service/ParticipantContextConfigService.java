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

package org.eclipse.edc.participantcontext.spi.config.service;

import org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfiguration;
import org.eclipse.edc.spi.result.ServiceResult;

/**
 * Service for managing participant context configurations.
 */
public interface ParticipantContextConfigService {

    /**
     * Saves the configuration for a given participant context.
     *
     * @param config the configuration to save
     * @return a ServiceResult indicating success or failure
     */
    ServiceResult<Void> save(ParticipantContextConfiguration config);

    /**
     * Retrieves the configuration for a given participant context.
     *
     * @param participantContextId the participant context identifier
     * @return a ServiceResult containing the configuration or an error if not found
     */
    ServiceResult<ParticipantContextConfiguration> get(String participantContextId);
}
