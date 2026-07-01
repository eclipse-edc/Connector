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
     * Saves the configuration for a given participant context. Null entry values are rejected with a bad-request
     * result; removal semantics are only supported through {@link #merge(ParticipantContextConfiguration)}.
     *
     * @param config the configuration to save
     * @return a ServiceResult indicating success or failure
     */
    ServiceResult<Void> save(ParticipantContextConfiguration config);

    /**
     * Merges the given configuration into the existing one for the same participant context, following JSON Merge Patch
     * (RFC 7396) semantics. Entries and private entries present in the patch with a non-null value are added or
     * overwritten; entries present with a null value are removed; existing entries that are not part of the patch are
     * preserved. If no configuration currently exists for the participant context, a new one is created from the patch.
     *
     * @param config the configuration containing the entries to merge
     * @return a ServiceResult indicating success or failure
     */
    ServiceResult<Void> merge(ParticipantContextConfiguration config);

    /**
     * Retrieves the configuration for a given participant context.
     *
     * @param participantContextId the participant context identifier
     * @return a ServiceResult containing the configuration or an error if not found
     */
    ServiceResult<ParticipantContextConfiguration> get(String participantContextId);
}
