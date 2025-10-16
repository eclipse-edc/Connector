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

import org.eclipse.edc.participantcontext.spi.config.store.ParticipantContextConfigStore;
import org.eclipse.edc.spi.system.configuration.Config;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryParticipantContextConfigStore implements ParticipantContextConfigStore {

    private final Map<String, Config> store = new ConcurrentHashMap<>();

    @Override
    public void save(String participantContextId, Config config) {
        store.put(participantContextId, config);
    }

    @Override
    public @Nullable Config get(String participantContextId) {
        return store.get(participantContextId);
    }
}
