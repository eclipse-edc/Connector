/*
 *  Copyright (c) 2022 Fraunhofer Institute for Software and Systems Engineering
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

package org.eclipse.dataspaceconnector.serializer;

import java.io.IOException;

/**
 * Provides interfaces for custom serializers located at the serializer extension.
 *
 * @param <I> object to serialize.
 * @param <O> object to deserialize.
 */
public interface Serializer<I, O> {

    /**
     * Convert object to string.
     *
     * @param object The object to serialize.
     * @return the result object (e.g., a string).
     * @throws IOException if serialization fails.
     */
    O serialize(I object) throws IOException;

    /**
     * Convert string to java object.
     *
     * @param input input object (e.g., a string).
     * @param type object type.
     * @return deserialization result.
     * @throws IOException if deserialization fails.
     */
    Object deserialize(O input, Class<?> type) throws IOException;
}
