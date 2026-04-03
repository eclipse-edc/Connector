/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

import java.util.Map;

/**
 * DTO representation of a Data Address.
 */
public final class DataAddressDto extends Typed {
    private final String type;
    @JsonUnwrapped
    private final Map<String, Object> properties;

    public DataAddressDto(String type, Map<String, Object> properties) {
        super("DataAddress");
        this.type = type;
        this.properties = properties;
    }

    public DataAddressDto(String type) {
        this(type, Map.of());
    }

    public String getType() {
        return type;
    }

    @JsonUnwrapped
    public Map<String, Object> getProperties() {
        return properties;
    }

}
