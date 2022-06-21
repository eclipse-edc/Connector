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

package org.eclipse.dataspaceconnector.policy.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * The types of {@link Policy}.
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum PolicyType {
    SET("set"), OFFER("offer"), CONTRACT("contract");

    @JsonProperty("@policytype")
    private String type;

    PolicyType(@JsonProperty("@policytype") String type) {
        this.type = type;
    }

    @JsonCreator
    public static PolicyType fromObject(Map<String, Object> object) {
        if (SET.type.equals(object.get("@policytype"))) {
            return SET;
        } else if (OFFER.type.equals(object.get("@policytype"))) {
            return OFFER;
        } else if (CONTRACT.type.equals(object.get("@policytype"))) {
            return CONTRACT;
        }
        throw new IllegalArgumentException("Invalid policy type");
    }

    public String getType() {
        return type;
    }
}
