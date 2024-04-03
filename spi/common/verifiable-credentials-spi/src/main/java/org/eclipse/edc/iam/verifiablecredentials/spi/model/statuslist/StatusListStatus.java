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

import static java.util.Optional.ofNullable;

public class StatusListStatus {

    public static final String STATUS_LIST_CREDENTIAL = "statusListCredential";
    public static final String STATUS_LIST_INDEX = "statusListIndex";
    public static final String STATUS_PURPOSE = "statusPurpose";
    private String statusListPurpose;
    private int statusListIndex;
    private String statusListCredential;

    private StatusListStatus() {

    }

    public static StatusListStatus parse(CredentialStatus status) {
        var instance = new StatusListStatus();
        instance.statusListCredential = ofNullable(status.additionalProperties().get(STATUS_LIST_CREDENTIAL))
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
