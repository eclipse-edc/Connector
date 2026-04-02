/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.signaling.logic.authorization;

import org.eclipse.edc.signaling.spi.authorization.SignalingAuthorization;
import org.eclipse.edc.signaling.spi.authorization.SignalingAuthorizationRegistry;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * In-memory implementation of {@link SignalingAuthorizationRegistry} backed by a {@link java.util.HashMap}.
 * Registrations are keyed by {@link SignalingAuthorization#getType()}; registering a second entry with the
 * same type silently replaces the first.
 */
public class SignalingAuthorizationRegistryImpl implements SignalingAuthorizationRegistry {

    private final Map<String, SignalingAuthorization> registry = new HashMap<>();

    @Override
    public void register(SignalingAuthorization signalingAuthorization) {
        registry.put(signalingAuthorization.getType(), signalingAuthorization);
    }

    @Override
    public Collection<SignalingAuthorization> getAll() {
        return registry.values();
    }

    @Override
    public SignalingAuthorization findByType(String type) {
        return registry.get(type);
    }
}
