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

package org.eclipse.edc.security.signature.jws2020;

import java.io.IOException;
import java.util.Objects;

class TestFunctions {

    static String readResourceAsString(String name) {
        try (var stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(name)) {
            return new String(Objects.requireNonNull(stream).readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
