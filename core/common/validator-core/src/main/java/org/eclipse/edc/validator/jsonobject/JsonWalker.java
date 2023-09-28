/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.validator.jsonobject;

import jakarta.json.JsonObject;

import java.util.stream.Stream;

/**
 * Extract objects from JsonObject sub-path.
 */
public interface JsonWalker {

    /**
     * Extract a {@link Stream} of {@link JsonObject} from the path passed that can then be validated.
     *
     * @param object the {@link JsonObject}.
     * @param path the {@link JsonLdPath}.
     * @return a {@link Stream} of {@link JsonObject} can never be null.
     */
    Stream<JsonObject> extract(JsonObject object, JsonLdPath path);
}
