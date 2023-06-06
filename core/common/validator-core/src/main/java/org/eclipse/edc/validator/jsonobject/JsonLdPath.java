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

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Represent a JsonLD path
 *
 * @param parts the path parts.
 */
public record JsonLdPath(String... parts) {

    public static JsonLdPath path(String... parts) {
        return new JsonLdPath(parts);
    }

    public String last() {
        if (parts.length == 0) {
            return "";
        }
        return parts[parts.length - 1];
    }

    public JsonLdPath append(String part) {
        var stream = Stream.concat(
                Arrays.stream(parts),
                Stream.of(part)
        );
        return new JsonLdPath(stream.toArray(String[]::new));
    }

    @Override
    public String toString() {
        return String.join("/", parts);
    }
}
