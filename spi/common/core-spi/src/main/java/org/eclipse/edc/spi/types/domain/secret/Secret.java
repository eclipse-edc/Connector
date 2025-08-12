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

import org.eclipse.edc.spi.entity.Entity;

import java.util.Objects;
import java.util.UUID;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;


/**
 * The {@link Secret} contains the metadata and describes the data itself or a collection of data.
 */
public class Secret extends Entity {
    public static final String EDC_SECRET_TYPE_TERM = "Secret";
    public static final String EDC_SECRET_TYPE = EDC_NAMESPACE + EDC_SECRET_TYPE_TERM;
    public static final String EDC_SECRET_VALUE = EDC_NAMESPACE + "value";

    private String value;

    private Secret() {
    }

    public String getValue() {
        return value;
    }

    public Builder toBuilder() {
        // No key, Id is used as vault key
        return Builder.newInstance()
                .value(value);
    }

    public static final class Builder extends Entity.Builder<Secret, Builder> {

        private Builder(Secret secret) {
            super(secret);
        }

        public static Builder newInstance() {
            return new Builder(new Secret());
        }

        @Override
        public Builder self() {
            return this;
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

            Objects.requireNonNull(entity.value, "'value' is missing");

            return entity;
        }
    }

}
