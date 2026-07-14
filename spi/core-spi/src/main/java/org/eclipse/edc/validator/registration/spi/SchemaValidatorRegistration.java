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

package org.eclipse.edc.validator.registration.spi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.spi.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * A runtime binding that associates a JSON-LD {@code @type} (identified by {@code validatedType}) with a JSON
 * {@code schema} document for a management API {@code version}. When present it causes management API request
 * bodies of that type to be validated against the referenced schema. The schema document itself must be available
 * to the connector (typically cached as a {@code JSON_SCHEMA} document via the document cache).
 * <p>
 * This is the runtime, API-driven counterpart of the configuration-based custom schema validation.
 */
@JsonDeserialize(builder = SchemaValidatorRegistration.Builder.class)
public class SchemaValidatorRegistration extends Entity {

    public static final String SCHEMA_VALIDATOR_REGISTRATION_TYPE_TERM = "SchemaValidatorRegistration";
    public static final String SCHEMA_VALIDATOR_REGISTRATION_TYPE_IRI = EDC_NAMESPACE + SCHEMA_VALIDATOR_REGISTRATION_TYPE_TERM;
    public static final String SCHEMA_VALIDATOR_REGISTRATION_VERSION_TERM = "version";
    public static final String SCHEMA_VALIDATOR_REGISTRATION_VERSION_IRI = EDC_NAMESPACE + SCHEMA_VALIDATOR_REGISTRATION_VERSION_TERM;
    public static final String SCHEMA_VALIDATOR_REGISTRATION_VALIDATED_TYPE_TERM = "validatedType";
    public static final String SCHEMA_VALIDATOR_REGISTRATION_VALIDATED_TYPE_IRI = EDC_NAMESPACE + SCHEMA_VALIDATOR_REGISTRATION_VALIDATED_TYPE_TERM;
    public static final String SCHEMA_VALIDATOR_REGISTRATION_SCHEMA_TERM = "schema";
    public static final String SCHEMA_VALIDATOR_REGISTRATION_SCHEMA_IRI = EDC_NAMESPACE + SCHEMA_VALIDATOR_REGISTRATION_SCHEMA_TERM;
    public static final String SCHEMA_VALIDATOR_REGISTRATION_PROFILES_TERM = "profiles";
    public static final String SCHEMA_VALIDATOR_REGISTRATION_PROFILES_IRI = EDC_NAMESPACE + SCHEMA_VALIDATOR_REGISTRATION_PROFILES_TERM;
    public static final String SCHEMA_VALIDATOR_REGISTRATION_UPDATED_AT_TERM = "updatedAt";
    public static final String SCHEMA_VALIDATOR_REGISTRATION_UPDATED_AT_IRI = EDC_NAMESPACE + SCHEMA_VALIDATOR_REGISTRATION_UPDATED_AT_TERM;

    private String version;
    private String validatedType;
    private String schema;
    private List<String> profiles = new ArrayList<>();
    private long updatedAt;

    private SchemaValidatorRegistration() {
    }

    public String getVersion() {
        return version;
    }

    public String getValidatedType() {
        return validatedType;
    }

    public String getSchema() {
        return schema;
    }

    public List<String> getProfiles() {
        return profiles;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public Builder toBuilder() {
        return Builder.newInstance()
                .id(id)
                .version(version)
                .validatedType(validatedType)
                .schema(schema)
                .profiles(new ArrayList<>(profiles))
                .createdAt(createdAt)
                .updatedAt(updatedAt);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder extends Entity.Builder<SchemaValidatorRegistration, Builder> {

        private Builder() {
            super(new SchemaValidatorRegistration());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder version(String version) {
            entity.version = version;
            return this;
        }

        public Builder validatedType(String validatedType) {
            entity.validatedType = validatedType;
            return this;
        }

        public Builder schema(String schema) {
            entity.schema = schema;
            return this;
        }

        public Builder profiles(List<String> profiles) {
            entity.profiles = profiles == null ? new ArrayList<>() : profiles;
            return this;
        }

        public Builder updatedAt(long updatedAt) {
            entity.updatedAt = updatedAt;
            return this;
        }

        @Override
        public Builder self() {
            return this;
        }

        @Override
        public SchemaValidatorRegistration build() {
            super.build();
            if (entity.id == null) {
                entity.id = UUID.randomUUID().toString();
            }
            Objects.requireNonNull(entity.version, "version cannot be null");
            Objects.requireNonNull(entity.validatedType, "validatedType cannot be null");
            Objects.requireNonNull(entity.schema, "schema cannot be null");
            if (entity.profiles == null) {
                entity.profiles = new ArrayList<>();
            }
            if (entity.updatedAt == 0) {
                entity.updatedAt = entity.createdAt;
            }
            return entity;
        }
    }
}
