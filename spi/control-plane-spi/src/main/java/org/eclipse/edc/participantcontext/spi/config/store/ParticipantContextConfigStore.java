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
     * Atomically applies a JSON Merge Patch (RFC 7396) to the configuration of a participant context: an entry with a
     * {@code null} value removes the key, any other value adds or overwrites it. If no configuration exists yet, one is
     * created from the patch, using the patch's {@code createdAt} timestamp.
     * <p>
     * Implementations MUST guarantee that the read-modify-write is atomic with respect to concurrent {@link #merge} and
     * {@link #save} invocations, including from other runtime instances sharing the same backing store. Persistent
     * implementations typically rely on the ambient {@link org.eclipse.edc.transaction.spi.TransactionContext} to hold
     * row locks until commit, so invoking this method outside a transactional context does not guarantee atomicity.
     * <p>
     * Values are stored verbatim: any encryption of private entries must have been applied by the caller beforehand.
     *
     * @param patch the merge patch to apply, its participant context id identifies the target configuration
     * @return the resulting configuration after the patch has been applied
     */
    ParticipantContextConfiguration merge(ParticipantContextConfiguration patch);

    /**
     * Retrieves the configuration for a given participant context.
     *
     * @param participantContextId the participant context identifier
     * @return the configuration, or null if not found
     */
    @Nullable
    ParticipantContextConfiguration get(String participantContextId);
}
