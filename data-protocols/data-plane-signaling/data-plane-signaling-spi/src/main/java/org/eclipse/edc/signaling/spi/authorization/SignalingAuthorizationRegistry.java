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

package org.eclipse.edc.signaling.spi.authorization;

import java.util.Collection;

/**
 * Registry for {@link SignalingAuthorization} implementations, keyed by their type identifier.
 * Each registered authorization strategy handles a distinct authentication scheme (e.g. OAuth2, API key).
 */
public interface SignalingAuthorizationRegistry {

    /**
     * Registers a {@link SignalingAuthorization}. If an entry with the same type already exists, it is replaced.
     *
     * @param signalingAuthorization the authorization strategy to register
     */
    void register(SignalingAuthorization signalingAuthorization);

    /**
     * Returns all registered {@link SignalingAuthorization} instances.
     *
     * @return a collection of all registered authorizations, possibly empty
     */
    Collection<SignalingAuthorization> getAll();

    /**
     * Returns the {@link SignalingAuthorization} registered for the given type, or {@code null} if none is found.
     *
     * @param type the authorization type identifier
     * @return the matching authorization, or {@code null}
     */
    SignalingAuthorization findByType(String type);
}
