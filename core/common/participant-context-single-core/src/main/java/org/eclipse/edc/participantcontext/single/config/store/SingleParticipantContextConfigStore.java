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

package org.eclipse.edc.participantcontext.single.config.store;

import org.eclipse.edc.participantcontext.spi.config.store.ParticipantContextConfigStore;
import org.eclipse.edc.spi.system.configuration.Config;
import org.jetbrains.annotations.Nullable;

public class SingleParticipantContextConfigStore implements ParticipantContextConfigStore {

    private final String participantContextId;
    private final Config config;

    public SingleParticipantContextConfigStore(String participantContextId, Config config) {
        this.participantContextId = participantContextId;
        this.config = config;
    }

    @Override
    public void save(String participantContextId, Config config) {
        throw new UnsupportedOperationException("SingleParticipantContextConfigStore is read-only");
    }

    @Override
    public @Nullable Config get(String participantContextId) {
        if (this.participantContextId.equals(participantContextId)) {
            return config;
        } else {
            return null;
        }
    }
}
