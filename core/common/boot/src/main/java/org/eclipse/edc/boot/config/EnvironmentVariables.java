/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.boot.config;

import java.util.Map;
import java.util.function.Supplier;


/**
 * Wrapping interface that provides Environment Variables
 */
public interface EnvironmentVariables extends Supplier<Map<String, String>> {

    /**
     * Default implementation.
     *
     * @return the EnvironmentVariables default implementation.
     */
    static EnvironmentVariables ofDefault() {
        return System::getenv;
    }

}
