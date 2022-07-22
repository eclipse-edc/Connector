/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.tooling.module.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enumerates {@link EdcModule} types. SPI modules define extension points and extension modules contribute capabilities to a runtime.
 */
public enum ModuleType {
    SPI("spi"),
    EXTENSION("extension");

    private final String key;

    ModuleType(String key) {
        this.key = key;
    }

    @JsonCreator
    public static ModuleType fromString(String key) {
        return key == null ? null : ModuleType.valueOf(key.toUpperCase());
    }

    @JsonValue
    public String getKey() {
        return key;
    }

}
