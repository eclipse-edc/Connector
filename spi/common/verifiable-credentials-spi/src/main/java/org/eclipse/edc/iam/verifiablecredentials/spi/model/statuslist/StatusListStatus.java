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

package org.eclipse.edc.iam.verifiablecredentials.spi.model.statuslist;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialStatus;

import java.util.Map;

import static java.util.Optional.ofNullable;
import static org.eclipse.edc.iam.verifiablecredentials.spi.VcConstants.STATUSLIST_2021_PREFIX;

/**
 * Specialized {@code credentialStatus}, that contains information mandated by the StatusList2021 standard.
 */
public class StatusListStatus {
    public static final String STATUS_LIST_CREDENTIAL = STATUSLIST_2021_PREFIX + "statusListCredential";
    public static final String STATUS_LIST_INDEX = STATUSLIST_2021_PREFIX + "statusListIndex";
    public static final String STATUS_PURPOSE = STATUSLIST_2021_PREFIX + "statusPurpose";

    private String statusListPurpose;
    private int statusListIndex;
    private String statusListCredential;

    private StatusListStatus() {
    }

    public static StatusListStatus parse(CredentialStatus status) {
        var instance = new StatusListStatus();
        instance.statusListCredential = ofNullable(getId(status))
                .map(Object::toString)
                .orElseThrow(() -> new IllegalArgumentException(missingProperty(STATUS_LIST_CREDENTIAL)));

        instance.statusListIndex = ofNullable(status.additionalProperties().get(STATUS_LIST_INDEX))
                .map(Object::toString)
                .map(Integer::parseInt)
                .orElseThrow(() -> new IllegalArgumentException(missingProperty(STATUS_LIST_INDEX)));

        instance.statusListPurpose = ofNullable(status.additionalProperties().get(STATUS_PURPOSE))
                .map(Object::toString)
                .orElseThrow(() -> new IllegalArgumentException(missingProperty(STATUS_PURPOSE)));

        return instance;
    }

    private static Object getId(CredentialStatus status) {
        var credentialId = status.additionalProperties().get(STATUS_LIST_CREDENTIAL);
        if (credentialId instanceof Map<?, ?> map) {
            return map.get("@id");
        }
        return credentialId;
    }

    private static String missingProperty(String property) {
        return "A StatusList2021 credentialStatus must have a 'credentialStatus.%s' property".formatted(property);
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
