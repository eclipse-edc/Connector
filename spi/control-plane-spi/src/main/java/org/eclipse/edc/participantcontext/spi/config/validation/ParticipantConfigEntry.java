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

package org.eclipse.edc.participantcontext.spi.config.validation;

import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Describes a participant context configuration key consumed by an extension, used to validate configurations
 * before they are persisted.
 * <p>
 * An entry declares:
 * <ul>
 *     <li>whether the key is sensitive, in which case it must be submitted as a private entry, otherwise as a public one</li>
 *     <li>a validator for its value, which only sees plain text values: values are validated before being encrypted</li>
 *     <li>when the key is required, as a condition on the effective configuration (e.g. only when another key has a given value)</li>
 * </ul>
 */
public final class ParticipantConfigEntry {

    private String key;
    private String description;
    private boolean sensitive;
    private Validator<String> validator = value -> ValidationResult.success();
    private Predicate<ParticipantConfigView> requiredWhen = view -> false;

    private ParticipantConfigEntry() {
    }

    public String getKey() {
        return key;
    }

    public String getDescription() {
        return description;
    }

    public boolean isSensitive() {
        return sensitive;
    }

    public ValidationResult validate(String value) {
        return validator.validate(value);
    }

    public boolean isRequired(ParticipantConfigView view) {
        return requiredWhen.test(view);
    }

    public static final class Builder {

        private final ParticipantConfigEntry entry = new ParticipantConfigEntry();

        private Builder(String key) {
            entry.key = key;
        }

        public static Builder newInstance(String key) {
            return new Builder(key);
        }

        public Builder description(String description) {
            entry.description = description;
            return this;
        }

        /**
         * Marks the key as sensitive: it must be submitted as a private entry, so that it gets stored encrypted.
         */
        public Builder sensitive() {
            entry.sensitive = true;
            return this;
        }

        public Builder validator(Validator<String> validator) {
            entry.validator = validator;
            return this;
        }

        /**
         * Marks the key as always required.
         */
        public Builder required() {
            return requiredWhen(view -> true);
        }

        /**
         * Marks the key as required when the condition holds on the effective configuration.
         */
        public Builder requiredWhen(Predicate<ParticipantConfigView> condition) {
            entry.requiredWhen = condition;
            return this;
        }

        public ParticipantConfigEntry build() {
            Objects.requireNonNull(entry.key, "key");
            Objects.requireNonNull(entry.validator, "validator");
            Objects.requireNonNull(entry.requiredWhen, "requiredWhen");
            return entry;
        }
    }
}
