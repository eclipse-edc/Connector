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
 * Describe a Secret update, after this has emitted, a Secret with a certain id will still be available.
 */
@JsonDeserialize(builder = SecretUpdated.Builder.class)
public class SecretUpdated extends SecretEvent {

    private SecretUpdated() {
    }

    @Override
    public String name() {
        return "secret.updated";
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends SecretEvent.Builder<SecretUpdated, Builder> {

        private Builder() {
            super(new SecretUpdated());
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
