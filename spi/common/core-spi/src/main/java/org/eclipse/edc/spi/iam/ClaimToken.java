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

package org.eclipse.edc.spi.iam;

import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.time.ZoneOffset.UTC;

/**
 * Models a token containing claims such as a JWT.
 */
public class ClaimToken {
    private final Map<String, Object> claims = new HashMap<>();

    private ClaimToken() {
    }

    /**
     * Returns the claims.
     */
    public Map<String, Object> getClaims() {
        return claims;
    }

    /**
     * Get the claim value by name
     *
     * @param claimName the name of the claim
     * @return the claim value, null if it does not exist
     */
    public Object getClaim(String claimName) {
        return claims.get(claimName);
    }

    /**
     * Get the date claim value by name cast to {@link String}
     *
     * @param claimName the name of the claim
     * @return the claim value, null if it does not exist
     */
    public String getStringClaim(String claimName) {
        return Optional.of(claims).map(it -> it.get(claimName)).map(String.class::cast).orElse(null);
    }

    /**
     * Get the date claim value by name cast to {@link List}
     *
     * @param claimName the name of the claim
     * @return the claim value, null if it does not exist
     */
    public List<?> getListClaim(String claimName) {
        return Optional.of(claims).map(it -> it.get(claimName)).map(List.class::cast).orElse(Collections.emptyList());
    }

    /**
     * Get the NumericDate claim value by name converted to {@link Instant}. The claim must be a long value.
     *
     * @param claimName the name of the claim
     * @return the claim value, null if it does not exist
     */
    public Instant getInstantClaim(String claimName) {
        return Optional.of(claims)
                .map(it -> it.get(claimName))
                .map(o -> {
                    if (o instanceof Long epoch) {
                        return epoch;
                    }
                    if (o instanceof Date d) {
                        return d.toInstant().getEpochSecond();
                    }
                    return null;
                })
                .map(this::convertToUtcTime)
                .orElse(null);
    }

    private Instant convertToUtcTime(Long epochSecond) {
        return Instant.ofEpochSecond(epochSecond).atOffset(UTC).toInstant();
    }

    public static class Builder {
        private final ClaimToken token;

        private Builder() {
            token = new ClaimToken();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder claim(String key, Object value) {
            token.claims.put(key, value);
            return this;
        }

        public Builder claims(Map<String, Object> map) {
            token.claims.putAll(map);
            return this;
        }

        public ClaimToken build() {
            return token;
        }
    }
}
