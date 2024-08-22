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

package org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.statuslist2021;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialStatus;

import java.util.Map;

import static java.util.Optional.ofNullable;
import static org.eclipse.edc.iam.verifiablecredentials.spi.VcConstants.STATUSLIST_2021_PREFIX;

/**
 * Specialized {@code credentialStatus}, that contains information mandated by the StatusList2021 standard.
 */
public class StatusList2021Status {

    public static final String TYPE = "StatusList2021Entry";
    public static final String STATUS_LIST_CREDENTIAL_LITERAL = "statusListCredential";
    public static final String STATUS_LIST_CREDENTIAL = STATUSLIST_2021_PREFIX + STATUS_LIST_CREDENTIAL_LITERAL;
    public static final String STATUS_LIST_INDEX_LITERAL = "statusListIndex";
    public static final String STATUS_LIST_INDEX = STATUSLIST_2021_PREFIX + STATUS_LIST_INDEX_LITERAL;
    public static final String STATUS_LIST_PURPOSE_LITERAL = "statusPurpose";
    public static final String STATUS_LIST_PURPOSE = STATUSLIST_2021_PREFIX + STATUS_LIST_PURPOSE_LITERAL;
    private String statusListPurpose;
    private int statusListIndex;
    private String statusListCredential;

    private StatusList2021Status() {
    }

    public static StatusList2021Status from(CredentialStatus status) {
        var instance = new StatusList2021Status();
        instance.statusListCredential = ofNullable(getId(status))
                .map(Object::toString)
                .orElseThrow(() -> new IllegalArgumentException(missingProperty(STATUS_LIST_CREDENTIAL)));

        instance.statusListIndex = ofNullable(status.getProperty(STATUSLIST_2021_PREFIX, STATUS_LIST_INDEX_LITERAL))
                .map(Object::toString)
                .map(Integer::parseInt)
                .orElseThrow(() -> new IllegalArgumentException(missingProperty(STATUS_LIST_INDEX)));

        instance.statusListPurpose = ofNullable(status.getProperty(STATUSLIST_2021_PREFIX, STATUS_LIST_PURPOSE_LITERAL))
                .map(Object::toString)
                .orElseThrow(() -> new IllegalArgumentException(missingProperty(STATUS_LIST_PURPOSE)));

        return instance;
    }

    private static Object getId(CredentialStatus status) {
        var credentialId = status.getProperty(STATUSLIST_2021_PREFIX, STATUS_LIST_CREDENTIAL_LITERAL);
        if (credentialId instanceof Map<?, ?> map) {
            return map.get("@id");
        }
        return credentialId;
    }

    private static String missingProperty(String property) {
        return "A StatusList2021 credential must have a credentialStatus object with the '%s' property".formatted(property);
    }

    public String getStatusListPurpose() {
        return statusListPurpose;
    }

    public int getStatusListIndex() {
        return statusListIndex;
    }

    public String getStatusListCredential() {
        return statusListCredential;
    }


}
