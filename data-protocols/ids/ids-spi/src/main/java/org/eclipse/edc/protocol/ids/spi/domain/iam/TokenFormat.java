/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.edc.protocol.ids.spi.domain.iam;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum TokenFormat {
    JWT("idsc:JWT");

    @JsonProperty("@id")
    private String id;

    TokenFormat(@JsonProperty("@id") String id) {
        this.id = id;
    }

    @JsonCreator
    public static TokenFormat fromObject(Map<String, Object> object) {
        if (JWT.id.equals(object.get("@id"))) {
            return JWT;
        }
        throw new IllegalArgumentException("Invalid token format");
    }

    public String getId() {
        return id;
    }
}
