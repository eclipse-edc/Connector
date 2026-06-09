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

package org.eclipse.edc.api.auth.spi;

import org.eclipse.edc.spi.result.Result;

import java.util.Arrays;

import static org.eclipse.edc.spi.result.Result.failure;

/**
 * A parsed access-control scope following the grammar {@code prefix[:resource]:action}, for example
 * {@code management-api:read} (≡ {@code management-api:*:read}) or {@code management-api:policies:write}.
 * <p>
 * The {@code resource} defaults to the {@link #WILDCARD} when omitted. A {@link Scope} carried in a token (the
 * <em>granted</em> scope) satisfies a {@link Scope} required by an endpoint when {@link #satisfies(Scope)} returns
 * {@code true}.
 */
public record Scope(String prefix, String resource, Action action) {

    public static final String WILDCARD = "*";
    public static final String DELIMITER = ":";

    /**
     * Parses a raw scope string. Accepts two- ({@code prefix:action}) or three-segment
     * ({@code prefix:resource:action}) forms; no segment may be blank and the action must be one of {@link Action}.
     */
    public static Result<Scope> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return failure("Scope must not be null or blank");
        }
        var parts = raw.trim().split(DELIMITER, -1);
        if (parts.length < 2 || parts.length > 3) {
            return failure("Scope '%s' must have the form 'prefix[:resource]:action'".formatted(raw));
        }
        if (Arrays.stream(parts).anyMatch(String::isBlank)) {
            return failure("Scope '%s' must not contain blank segments".formatted(raw));
        }

        var prefix = parts[0];
        var resource = parts.length == 3 ? parts[1] : WILDCARD;
        var actionToken = parts[parts.length - 1];

        return Action.parse(actionToken)
                .map(action -> new Scope(prefix, resource, action));
    }

    /**
     * Whether this (granted) scope satisfies the {@code required} scope: the prefixes must be equal, this scope's
     * resource must equal the required resource or be the {@link #WILDCARD}, and this scope's action must be at least as
     * strong as the required action (see {@link Action#satisfies(Action)}).
     */
    public boolean satisfies(Scope required) {
        return prefix.equals(required.prefix) &&
                (resource.equals(required.resource) || resource.equals(WILDCARD)) &&
                action.satisfies(required.action);
    }

    /**
     * The action component, ordered by increasing strength: {@code ADMIN ⊇ WRITE ⊇ READ}.
     */
    public enum Action {
        READ(0),
        WRITE(1),
        ADMIN(2);

        private final int level;

        Action(int level) {
            this.level = level;
        }

        /**
         * Whether this action is at least as strong as the {@code required} one, i.e. {@code admin} satisfies any
         * required action, {@code write} satisfies {@code write} and {@code read}, and {@code read} satisfies only
         * {@code read}.
         */
        public boolean satisfies(Action required) {
            return this.level >= required.level;
        }

        static Result<Action> parse(String token) {
            return Arrays.stream(values())
                    .filter(a -> a.name().equalsIgnoreCase(token))
                    .findFirst()
                    .map(Result::success)
                    .orElseGet(() -> failure("Unknown scope action '%s'".formatted(token)));
        }
    }
}
