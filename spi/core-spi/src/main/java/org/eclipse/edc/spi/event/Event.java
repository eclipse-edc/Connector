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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.spi.event;

/**
 * The Event base class for all EDC events. This represents the content of the event, and it should
 * not contain any event metadata. All the metadata such as id, timestamp, etc. are in the {@link EventEnvelope}
 */
public abstract class Event {

    /**
     * The name of the event in dot notation.
     *
     * @return the event name.
     */
    public abstract String name();
}
