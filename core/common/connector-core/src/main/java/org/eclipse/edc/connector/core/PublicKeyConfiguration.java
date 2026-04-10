/*
 *  Copyright (c) 2025 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.core;

import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.Settings;

/**
 * Configuration for a locally-stored public key entry.
 */
@Settings
public record PublicKeyConfiguration(

        @Setting(
                key = "id",
                description = "ID of the public key.")
        String id,

        @Setting(
                key = "value",
                required = false,
                description = "Value of the public key. Multiple formats are supported, depending on the KeyParsers registered in the runtime")
        String value,

        @Setting(
                key = "path",
                required = false,
                description = "Path to a file that holds the public key, e.g. a PEM file. Multiple formats are supported, depending on the KeyParsers registered in the runtime")
        String path) {
}
