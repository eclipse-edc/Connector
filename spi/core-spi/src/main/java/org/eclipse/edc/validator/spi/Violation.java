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

package org.eclipse.edc.validator.spi;

/**
 * Represent a single violation in an object validator.
 *
 * @param message error message.
 * @param path the path in the object.
 * @param value the actual value.
 */
public record Violation(String message, String path, Object value) {
    public static Violation violation(String message, String path) {
        return new Violation(message, path, null);
    }

    public static Violation violation(String message, String path, Object value) {
        return new Violation(message, path, value);
    }
}
