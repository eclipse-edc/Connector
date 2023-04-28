/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.spi.serialization;

import org.eclipse.edc.spi.types.domain.message.RemoteMessage;

/**
 * Serializes {@link RemoteMessage}s to JSON-LD.
 */
public interface JsonLdRemoteMessageSerializer {

    /**
     * Serializes a {@link RemoteMessage} to JSON-LD using the given JSON-LD context.
     *
     * @param message the message to serialize
     * @return the serialized message
     */
    String serialize(RemoteMessage message);

}
