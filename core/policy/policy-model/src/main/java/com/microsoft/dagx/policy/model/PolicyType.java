/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.policy.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * The types of {@link Policy}.
 */
public enum PolicyType {
    SET("set"), OFFER("offer"), CONTRACT("contract");

    @JsonProperty("@policytype")
    private String type;

    public String getType() {
        return type;
    }

    PolicyType(@JsonProperty("@policytype") String type) {
        this.type = type;
    }

    @JsonCreator
    public static PolicyType fromObject(final Map<String, Object> object) {
        if (SET.type.equals(object.get("@policytype"))) {
            return SET;
        } else if (OFFER.type.equals(object.get("@policytype"))) {
            return OFFER;
        } else if (CONTRACT.type.equals(object.get("@policytype"))) {
            return CONTRACT;
        }
        throw new IllegalArgumentException("Invalid policy type");
    }
}
