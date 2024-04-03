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

package org.eclipse.edc.iam.identitytrust.spi.model.statuslist;

import org.eclipse.edc.iam.identitytrust.spi.model.VerifiableCredential;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Represents a special {@link VerifiableCredential}, specifically a status list credentials. That means that the shape of the
 * {@link VerifiableCredential#getCredentialSubject()} is not arbitrary anymore, but must contain specific items.
 * Currently supported are BitStringStatusList and the older StatusList2021. Both have similar shapes, but the BitStringStatusList
 * contains more information.
 * <p>
 * Since every status list credential is a valid {@link VerifiableCredential}, way to construct them is via the static factory method
 * {@link StatusListCredential#parse(VerifiableCredential)}, which will throw a {@link IllegalArgumentException} if the shape is not correct.
 */
public class StatusListCredential extends VerifiableCredential {
    public static final String BITSTRING_STATUSLIST_TYPE = "BitStringStatusList";
    public static final String STATUSLIST_2021_TYPE = "StatusList2021";
    public static final String BITSTRING_STATUSLIST_CREDENTIAL = BITSTRING_STATUSLIST_TYPE + "Credential";
    public static final String STATUSLIST_2021_CREDENTIAL = STATUSLIST_2021_TYPE + "Credential";
    public static final String ENCODED_LIST = "encodedList";
    public static final String STATUS_PURPOSE = "statusPurpose";
    public static final String TTL = "ttl";
    public static final String STATUS_REFERENCE = "statusReference";
    public static final String STATUS_SIZE = "statusSize";
    public static final String STATUS_MESSAGE = "statusMessage";

    private StatusListCredential() {
    }

    public static StatusListCredential parse(VerifiableCredential rawCredential) {
        return StatusListCredential.Builder.newInstance()
                .credentialStatus(rawCredential.getCredentialStatus())
                .id(rawCredential.getId())
                .credentialSubject(rawCredential.getCredentialSubject())
                .name(rawCredential.getName())
                .types(rawCredential.getType())
                .description(rawCredential.getDescription())
                .issuanceDate(rawCredential.getIssuanceDate())
                .issuer(rawCredential.getIssuer())
                .expirationDate(rawCredential.getExpirationDate())
                .build();
    }

    public String encodedList() {
        return (String) credentialSubject.get(0).getClaims().get(ENCODED_LIST);
    }

    public String statusPurpose() {
        return (String) credentialSubject.get(0).getClaims().get(STATUS_PURPOSE);
    }

    /**
     * If the status list credential is a <a href="https://www.w3.org/TR/vc-bitstring-status-list/#bitstringstatuslistcredential">BitStringStatusList</a>
     * credential, this will contain the "ttl" field.
     * Will be null for other credential types, such as StatusList2021
     */
    @Nullable
    public Long ttl() {
        return Optional.ofNullable(credentialSubject.get(0).getClaims().get(TTL))
                .map(Object::toString)
                .map(Long::parseLong)
                .orElse(null);
    }

    /**
     * If the status list credential is a <a href="https://www.w3.org/TR/vc-bitstring-status-list/#bitstringstatuslistcredential">BitStringStatusList</a>
     * credential, this will contain the "statusReference" field.
     * Will be null for other credential types, such as StatusList2021
     */
    @Nullable
    public String statusReference() {
        return Optional.ofNullable(credentialSubject.get(0).getClaims().get(STATUS_REFERENCE))
                .map(Object::toString)
                .orElse(null);
    }

    /**
     * If the status list credential is a <a href="https://www.w3.org/TR/vc-bitstring-status-list/#bitstringstatuslistcredential">BitStringStatusList</a>
     * credential, this will contain the "statusSize" field.
     * Will be null for other credential types, such as StatusList2021
     */
    @Nullable
    public Integer statusSize() {
        return Optional.ofNullable(credentialSubject.get(0).getClaims().get(STATUS_SIZE))
                .map(Object::toString)
                .map(Integer::parseInt)
                .orElse(null);
    }

    /**
     * If the status list credential is a <a href="https://www.w3.org/TR/vc-bitstring-status-list/#bitstringstatuslistcredential">BitStringStatusList</a>
     * credential, this will contain the "statusMessage" field. May be empty.
     * Will be null for other credential types, such as StatusList2021
     */
    @Nullable
    public List<StatusMessage> statusMessages() {
        var messages = credentialSubject.get(0).getClaims().get(STATUS_MESSAGE);

        //todo: implement
        return null;
    }

    public static class Builder extends VerifiableCredential.Builder<StatusListCredential, Builder> {
        protected Builder(StatusListCredential credential) {
            super(credential);
        }

        public static Builder newInstance() {
            return new Builder(new StatusListCredential());
        }

        @Override
        public StatusListCredential build() {
            super.build();
            if (!instance.type.contains(BITSTRING_STATUSLIST_CREDENTIAL) && !instance.type.contains(STATUSLIST_2021_CREDENTIAL)) {
                throw new IllegalArgumentException("Status list credentials must be one of %s but was %s"
                        .formatted(String.join(",", BITSTRING_STATUSLIST_CREDENTIAL, STATUSLIST_2021_CREDENTIAL), instance.type));
            }

            if (instance.credentialSubject == null || instance.credentialSubject.isEmpty()) {
                throw new IllegalArgumentException("Status list credentials must contain a 'credentialSubject' property.");
            }
            if (instance.credentialSubject.size() != 1) {
                throw new IllegalArgumentException("Status list credentials must contain exactly 1 `credentialsubject` object, but found %d".formatted(instance.credentialSubject.size()));
            }

            // check mandatory fields of the credentialSubject object
            var subject = instance.credentialSubject.get(0);
            if (!subject.getClaims().containsKey(ENCODED_LIST)) {
                throw new IllegalArgumentException("Status list credentials must contain a 'credentialSubject.encodedList' field.");
            }
            if (!subject.getClaims().containsKey(STATUS_PURPOSE)) {
                throw new IllegalArgumentException("Status list credentials must contain a 'credentialSubject.statusPurpose' field.");
            }
            return instance;
        }

        @Override
        protected Builder self() {
            return this;
        }
    }

    /**
     * this is a status message entry consisting of the binary representation of the status, and an arbitrary text.
     *
     * @param status  the hex value of the status, e.g. "0x0"
     * @param message an arbitrary string
     */
    public record StatusMessage(String status, String message) {
    }
}
