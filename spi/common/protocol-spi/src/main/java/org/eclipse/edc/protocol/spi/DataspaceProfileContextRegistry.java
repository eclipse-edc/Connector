/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.spi;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * A registry of configured {@link DataspaceProfileContext}.
 * Profile contexts can be of 2 types: default and standard.
 * <p>
 * Default ones are available out of the box from the EDC libraries.
 * Standard ones can be defined and register by adopters following their Dataspace peculiarities.
 * <p>
 * A single "standard" registration will hide all the default ones,
 */
public interface DataspaceProfileContextRegistry {

    /**
     * Register a default profile context.
     *
     * @param context the default profile context.
     */
    void registerDefault(DataspaceProfileContext context);

    /**
     * Register a standard profile context.
     *
     * @param context the standard profile context.
     */
    void register(DataspaceProfileContext context);

    /**
     * get all the protocol versions.
     *
     * @return the protocol versions.
     */
    ProtocolVersions getProtocolVersions();

    /**
     * Get the protocol version for a given protocol.
     *
     * @param protocol The protocol name
     * @return The protocol version, or null if no version is registered for the protocol
     */
    @Nullable
    ProtocolVersion getProtocolVersion(String protocol);

    /**
     * Get the function for participant id extraction for a given protocol.
     *
     * @param protocol The protocol name
     * @return The id extraction function, or null if no function is registered for the protocol
     */
    @Nullable
    ParticipantIdExtractionFunction getIdExtractionFunction(String protocol);

    /**
     * Get all the registered profiles, if a standard profile is registered, only the standard ones are returned, otherwise all the default ones are returned.
     *
     * @return a list of profile contexts. Always not null.
     */
    List<DataspaceProfileContext> getProfiles();

    /**
     * Get a registered profile by its id.
     *
     * @param profileId the profile id
     * @return the matching profile, or null if no profile with that id is registered
     */
    @Nullable
    DataspaceProfileContext getProfile(String profileId);

    /**
     * Adds a callback that is invoked once per profile that is now or will later be registered.
     * The callback is invoked synchronously for every already-registered profile when added,
     * and again whenever a new profile is registered via {@link #register(DataspaceProfileContext)}
     * or {@link #registerDefault(DataspaceProfileContext)}. This lets components attach
     * profile-scoped side effects (JSON-LD context registration, dispatcher binding, validator
     * keying, etc.) without the registry depending on those subsystems.
     *
     * @param callback the callback to invoke per profile
     */
    void addRegistrationCallback(Consumer<DataspaceProfileContext> callback);
}
