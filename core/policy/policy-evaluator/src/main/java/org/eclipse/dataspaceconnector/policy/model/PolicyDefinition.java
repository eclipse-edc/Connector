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
 *       Fraunhofer Institute for Software and Systems Engineering - added method
 *
 */

package org.eclipse.dataspaceconnector.policy.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.Objects;

/**
 * A collection of permissions, prohibitions, and obligations associated with an asset. Subtypes are defined by {@link PolicyType}.
 */
@JsonDeserialize(builder = PolicyDefinition.Builder.class)
public class PolicyDefinition extends Policy {


    private String uid;

    private PolicyDefinition() {
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), uid);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PolicyDefinition policy = (PolicyDefinition) o;
        return super.equals(o) && Objects.equals(uid, policy.uid);
    }

    public String getUid() {
        return uid;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends Policy.Builder<Builder> {

        private Builder() {
            super(new PolicyDefinition());
        }

        public static Builder newInstance() {
            return new Builder();
        }

        @JsonProperty("uid")
        public PolicyDefinition.Builder id(String id) {
            policy.uid = id;
            return this;
        }

        public PolicyDefinition build() {
            return (PolicyDefinition) policy;
        }
    }
}
