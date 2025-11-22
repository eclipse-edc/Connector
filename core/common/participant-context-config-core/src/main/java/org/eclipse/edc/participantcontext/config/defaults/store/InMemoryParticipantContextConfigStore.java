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

package org.eclipse.edc.participantcontext.config.defaults.store;

import org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfiguration;
import org.eclipse.edc.participantcontext.spi.config.store.ParticipantContextConfigStore;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryParticipantContextConfigStore implements ParticipantContextConfigStore {

    private final Map<String, ParticipantContextConfiguration> store = new ConcurrentHashMap<>();

    @Override
    public void save(ParticipantContextConfiguration config) {
        store.put(config.getParticipantContextId(), config);
    }

    @Override
    public @Nullable ParticipantContextConfiguration get(String participantContextId) {
        return store.get(participantContextId);
    }
}
