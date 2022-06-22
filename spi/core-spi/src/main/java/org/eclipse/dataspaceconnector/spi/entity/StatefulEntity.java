/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.spi.entity;

/**
 * Represents an entity, with id and state.
 */
public interface StatefulEntity {

    /**
     * Returns the id of the entity
     *
     * @return id of the entity
     */
    String getId();

    /**
     * Returns the current state.
     *
     * @return The current state.
     */
    int getState();

    /**
     * Returns the current state count.
     *
     * @return The current state count.
     */
    int getStateCount();

    /**
     * Returns the state timestamp.
     *
     * @return The state timestamp.
     */
    long getStateTimestamp();
}
