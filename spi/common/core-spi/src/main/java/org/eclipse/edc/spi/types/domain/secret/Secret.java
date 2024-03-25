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

package org.eclipse.edc.spi.types.domain.secret;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.spi.entity.Entity;

import java.util.Objects;
import java.util.UUID;

import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;


/**
 * The {@link Secret} contains the metadata and describes the data itself or a collection of data.
 */
@JsonDeserialize(builder = Secret.Builder.class)
public class Secret extends Entity {
    public static final String PROPERTY_ID = EDC_NAMESPACE + "id";
    public static final String EDC_SECRET_TYPE = EDC_NAMESPACE + "Secret";
    public static final String EDC_SECRET_KEY = EDC_NAMESPACE + "key";
    public static final String EDC_SECRET_VALUE = EDC_NAMESPACE + "value";

    private String key;
    private String value;


    private Secret() {
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public Builder toBuilder() {
        return Builder.newInstance()
                .key(key)
                .value(value);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends Entity.Builder<Secret, Builder> {

        protected Builder(Secret asset) {
            super(asset);
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder(new Secret());
        }

        @Override
        public Builder self() {
            return this;
        }

        public Builder key(String key) {
            entity.key = key;
            return self();
        }

        public Builder value(String value) {
            entity.value = value;
            return self();
        }


        @Override
        public Secret build() {
            super.build();

            if (entity.getId() == null) {
                id(UUID.randomUUID().toString());
            }

            Objects.requireNonNull(entity.key, "`key` is missing");
            Objects.requireNonNull(entity.value, "`value` is missing");

            return entity;
        }
    }

}
