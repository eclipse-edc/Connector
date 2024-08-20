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

package org.eclipse.edc.iam.verifiablecredentials.spi.model.statuslist2021;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;

import static org.eclipse.edc.iam.verifiablecredentials.spi.VcConstants.STATUSLIST_2021_PREFIX;

/**
 * Represents a special {@link VerifiableCredential}, specifically a <a href="https://www.w3.org/TR/2023/WD-vc-status-list-20230427/">W3C StatusList2021</a> credential.
 * That means that the shape of the {@link VerifiableCredential#getCredentialSubject()} is not arbitrary anymore, but must contain specific items.
 * <p>
 * Since every StatusList2021 credential is a valid {@link VerifiableCredential}, way to construct them is via the static factory method
 * {@link StatusList2021Credential#parse(VerifiableCredential)}, which will throw a {@link IllegalArgumentException} if the shape is not correct.
 */
public class StatusList2021Credential extends VerifiableCredential {
    public static final String STATUSLIST_2021_CREDENTIAL = "StatusList2021Credential";

    public static final String STATUS_LIST_ENCODED_LIST_LITERAL = "encodedList";

    public static final String STATUS_LIST_ENCODED_LIST = STATUSLIST_2021_PREFIX + STATUS_LIST_ENCODED_LIST_LITERAL;

    private StatusList2021Credential() {
    }

    public static StatusList2021Credential parse(VerifiableCredential rawCredential) {
        return StatusList2021Credential.Builder.newInstance()
                .credentialStatus(rawCredential.getCredentialStatus())
                .id(rawCredential.getId())
                .credentialSubjects(rawCredential.getCredentialSubject())
                .name(rawCredential.getName())
                .types(rawCredential.getType())
                .description(rawCredential.getDescription())
                .issuanceDate(rawCredential.getIssuanceDate())
                .issuer(rawCredential.getIssuer())
                .expirationDate(rawCredential.getExpirationDate())
                .build();
    }

    public String encodedList() {
        return (String) credentialSubject.get(0).getClaim(STATUSLIST_2021_PREFIX, STATUS_LIST_ENCODED_LIST_LITERAL);
    }

    public String statusPurpose() {
        return (String) credentialSubject.get(0).getClaim(STATUSLIST_2021_PREFIX, StatusList2021Status.STATUS_LIST_PURPOSE_LITERAL);
    }

    public static class Builder extends VerifiableCredential.Builder<StatusList2021Credential, Builder> {
        protected Builder(StatusList2021Credential credential) {
            super(credential);
        }

        public static Builder newInstance() {
            return new Builder(new StatusList2021Credential());
        }

        @Override
        public StatusList2021Credential build() {
            super.build();
            if (!instance.type.contains(STATUSLIST_2021_CREDENTIAL)) {
                throw new IllegalArgumentException("Only %s is supported, but encountered: %s".formatted(STATUSLIST_2021_CREDENTIAL, instance.type));
            }

            if (instance.credentialSubject == null || instance.credentialSubject.isEmpty()) {
                throw new IllegalArgumentException("Status list credentials must contain a 'credentialSubject' property.");
            }
            if (instance.credentialSubject.size() != 1) {
                throw new IllegalArgumentException("Status list credentials must contain exactly 1 'credentialSubject' object, but found %d".formatted(instance.credentialSubject.size()));
            }

            // check mandatory fields of the credentialSubject object
            var subject = instance.credentialSubject.get(0);
            if (subject.getClaim(STATUSLIST_2021_PREFIX, STATUS_LIST_ENCODED_LIST_LITERAL) == null) {
                throw new IllegalArgumentException("Status list credentials must contain a 'credentialSubject.encodedList' field.");
            }
            if (subject.getClaim(STATUSLIST_2021_PREFIX, StatusList2021Status.STATUS_LIST_PURPOSE_LITERAL) == null) {
                throw new IllegalArgumentException("Status list credentials must contain a 'credentialSubject.statusPurpose' field.");
            }
            return instance;
        }

        @Override
        protected Builder self() {
            return this;
        }
    }


}
