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

import org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfiguration;
import org.eclipse.edc.participantcontext.spi.config.store.ParticipantContextConfigStore;
import org.jetbrains.annotations.Nullable;

public class SingleParticipantContextConfigStore implements ParticipantContextConfigStore {

    private final ParticipantContextConfiguration config;

    public SingleParticipantContextConfigStore(ParticipantContextConfiguration config) {
        this.config = config;
    }

    @Override
    public void save(ParticipantContextConfiguration config) {
        throw new UnsupportedOperationException("SingleParticipantContextConfigStore is read-only");
    }

    @Override
    public @Nullable ParticipantContextConfiguration get(String participantContextId) {
        if (this.config.getParticipantContextId().equals(participantContextId)) {
            return config;
        } else {
            return null;
        }
    }
}
