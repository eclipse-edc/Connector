/*
 *  Copyright (c) 2024 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - Initial API and Implementation
 *
 */

package org.eclipse.edc.connector.secret.spi.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * Describe an Secret deletion, after this has emitted, the Secret represented by the id won't be available anymore.
 */
@JsonDeserialize(builder = SecretDeleted.Builder.class)
public class SecretDeleted extends SecretEvent {

    private SecretDeleted() {
    }

    @Override
    public String name() {
        return "secret.deleted";
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends SecretEvent.Builder<SecretDeleted, Builder> {

        private Builder() {
            super(new SecretDeleted());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        @Override
        public Builder self() {
            return this;
        }
    }

}
