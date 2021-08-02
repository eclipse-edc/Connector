/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.ids.spi.domain.iam;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum TokenFormat {
    JWT("idsc:JWT");

    @JsonProperty("@id")
    private String id;

    public String getId() {
        return id;
    }

    TokenFormat(@JsonProperty("@id") String id) {
        this.id = id;
    }

    @JsonCreator
    public static TokenFormat fromObject(final Map<String, Object> object) {
        if (JWT.id.equals(object.get("@id"))) {
            return JWT;
        }
        throw new IllegalArgumentException("Invalid token format");
    }
}
