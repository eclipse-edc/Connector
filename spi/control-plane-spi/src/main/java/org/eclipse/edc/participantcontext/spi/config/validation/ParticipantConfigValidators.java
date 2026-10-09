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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;

import static org.eclipse.edc.validator.spi.Violation.violation;

/**
 * Common value validators for {@link ParticipantConfigEntry}. The violations they return carry no path: the
 * {@link ParticipantContextConfigValidatorRegistry} sets it to the validated key.
 */
public final class ParticipantConfigValidators {

    private ParticipantConfigValidators() {
    }

    public static Validator<String> notBlank() {
        return value -> value.isBlank() ? failure("must not be blank") : ValidationResult.success();
    }

    public static Validator<String> longValue() {
        return longValue(Long.MIN_VALUE, Long.MAX_VALUE);
    }

    public static Validator<String> longValue(long min, long max) {
        return value -> {
            try {
                var parsed = Long.parseLong(value.trim());
                if (parsed < min || parsed > max) {
                    return failure("must be between %d and %d".formatted(min, max));
                }
                return ValidationResult.success();
            } catch (NumberFormatException e) {
                return failure("must be an integer number");
            }
        };
    }

    public static Validator<String> bool() {
        return value -> "true".equalsIgnoreCase(value.trim()) || "false".equalsIgnoreCase(value.trim())
                ? ValidationResult.success()
                : failure("must be either 'true' or 'false'");
    }

    /**
     * Accepts absolute URLs with an {@code http} or {@code https} scheme.
     */
    public static Validator<String> httpUrl() {
        return value -> {
            try {
                var uri = new URI(value.trim());
                var scheme = uri.getScheme();
                if (!uri.isAbsolute() || uri.getHost() == null || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
                    return failure("must be an absolute http(s) URL");
                }
                return ValidationResult.success();
            } catch (URISyntaxException e) {
                return failure("must be an absolute http(s) URL");
            }
        };
    }

    public static Validator<String> startsWith(String prefix) {
        return value -> value.startsWith(prefix) ? ValidationResult.success() : failure("must start with '%s'".formatted(prefix));
    }

    /**
     * Accepts one of the allowed values, resolved at validation time, so that values registered after the entry are
     * taken into account.
     */
    public static Validator<String> oneOf(Supplier<Set<String>> allowed) {
        return value -> {
            var values = allowed.get();
            return values.contains(value) ? ValidationResult.success() : failure("must be one of %s".formatted(new TreeSet<>(values)));
        };
    }

    private static ValidationResult failure(String message) {
        return ValidationResult.failure(violation(message, null));
    }
}
