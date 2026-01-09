/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.bitstringstatuslist;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;

import java.time.Duration;
import java.time.Instant;

import static java.util.Optional.ofNullable;
import static org.eclipse.edc.iam.verifiablecredentials.spi.VcConstants.BITSTRING_STATUS_LIST_PREFIX;

public class BitstringStatusListCredential extends VerifiableCredential {
    public static final String BITSTRING_STATUSLIST_CREDENTIAL = "BitstringStatusListCredential";
    public static final String BITSTRING_ENCODED_LIST_LITERAL = "encodedList";
    public static final String BITSTRING_TTL_LITERAL = "ttl";
    public static final String STATUS_LIST_ENCODED_LIST = BITSTRING_STATUS_LIST_PREFIX + BITSTRING_ENCODED_LIST_LITERAL;
    public static final String STATUS_LIST_TTL = BITSTRING_STATUS_LIST_PREFIX + BITSTRING_TTL_LITERAL;

    public String encodedList() {
        return (String) credentialSubject.get(0).getClaim(BITSTRING_STATUS_LIST_PREFIX, BITSTRING_ENCODED_LIST_LITERAL);
    }

    public String statusPurpose() {
        return (String) credentialSubject.get(0).getClaim(BITSTRING_STATUS_LIST_PREFIX, BitstringStatusListStatus.BITSTRING_STATUS_LIST_PURPOSE_LITERAL);
    }

    public Instant validFrom() {
        return getIssuanceDate();
    }

    public Instant validUntil() {
        return getExpirationDate();
    }

    /**
     * The "time-to-live" in milliseconds
     *
     * @see <a href="https://www.w3.org/TR/vc-bitstring-status-list/#bitstringstatuslistcredential">W3C BitStringStatusListCredential</a>
     * @deprecated the "ttl" field is in conflict with the {@link BitstringStatusListCredential#validUntil()} field and may get removed in the future.
     */
    @Deprecated(since = "0.16.0")
    public Duration ttl() {
        var ttl = ofNullable(credentialSubject.get(0).getClaim(BITSTRING_STATUS_LIST_PREFIX, BITSTRING_TTL_LITERAL))
                .map(String::valueOf)
                .map(Long::parseLong)
                .orElse(300_000L);

        return Duration.ofMillis(ttl);
    }

    public static class Builder extends VerifiableCredential.Builder<BitstringStatusListCredential, BitstringStatusListCredential.Builder> {
        protected Builder(BitstringStatusListCredential credential) {
            super(credential);
        }

        public static BitstringStatusListCredential.Builder newInstance() {
            return new BitstringStatusListCredential.Builder(new BitstringStatusListCredential());
        }

        @Override
        public BitstringStatusListCredential build() {
            super.build();
            if (!instance.type.contains(BITSTRING_STATUSLIST_CREDENTIAL)) {
                throw new IllegalArgumentException("Only '%s' type is supported, but encountered: %s".formatted(BITSTRING_STATUSLIST_CREDENTIAL, instance.type));
            }

            if (instance.credentialSubject == null || instance.credentialSubject.isEmpty()) {
                throw new IllegalArgumentException("Status list credentials must contain a 'credentialSubject' property.");
            }
            if (instance.credentialSubject.size() != 1) {
                throw new IllegalArgumentException("Status list credentials must contain exactly 1 'credentialSubject' object, but found %d".formatted(instance.credentialSubject.size()));
            }

            // check mandatory fields of the credentialSubject object
            var subject = instance.credentialSubject.get(0);
            if (subject.getClaim(BITSTRING_STATUS_LIST_PREFIX, BITSTRING_ENCODED_LIST_LITERAL) == null) {
                throw new IllegalArgumentException("Status list credentials must contain a 'credentialSubject.encodedList' field.");
            }
            if (subject.getClaim(BITSTRING_STATUS_LIST_PREFIX, BitstringStatusListStatus.BITSTRING_STATUS_LIST_PURPOSE_LITERAL) == null) {
                throw new IllegalArgumentException("Status list credentials must contain a 'credentialSubject.statusPurpose' field.");
            }
            return instance;
        }

        @Override
        protected BitstringStatusListCredential.Builder self() {
            return this;
        }
    }
}
