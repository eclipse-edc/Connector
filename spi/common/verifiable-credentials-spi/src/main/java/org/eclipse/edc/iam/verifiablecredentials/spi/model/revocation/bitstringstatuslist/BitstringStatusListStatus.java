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

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Optional.ofNullable;
import static org.eclipse.edc.iam.verifiablecredentials.spi.VcConstants.BITSTRING_STATUS_LIST_PREFIX;

public class BitstringStatusListStatus {

    public static final String TYPE = "BitstringStatusListEntry";
    public static final String BITSTRING_STATUS_LIST_CREDENTIAL_LITERAL = "statusListCredential";
    public static final String STATUS_LIST_CREDENTIAL = BITSTRING_STATUS_LIST_PREFIX + BITSTRING_STATUS_LIST_CREDENTIAL_LITERAL;
    public static final String BITSTRING_STATUS_LIST_INDEX_LITERAL = "statusListIndex";
    public static final String STATUS_LIST_INDEX = BITSTRING_STATUS_LIST_PREFIX + BITSTRING_STATUS_LIST_INDEX_LITERAL;
    public static final String BITSTRING_STATUS_LIST_PURPOSE_LITERAL = "statusPurpose";
    public static final String STATUS_LIST_PURPOSE = BITSTRING_STATUS_LIST_PREFIX + BITSTRING_STATUS_LIST_PURPOSE_LITERAL;
    public static final String BITSTRING_STATUS_LIST_SIZE_LITERAL = "statusSize";
    public static final String STATUS_LIST_SIZE = BITSTRING_STATUS_LIST_PREFIX + BITSTRING_STATUS_LIST_SIZE_LITERAL;
    public static final String BITSTRING_STATUS_LIST_MESSAGE_LITERAL = "statusMessage";
    public static final String STATUS_LIST_MESSAGES = BITSTRING_STATUS_LIST_PREFIX + BITSTRING_STATUS_LIST_MESSAGE_LITERAL;
    public static final String BITSTRING_STATUS_LIST_REFERENCE_LITERAL = "statusReference";
    public static final String STATUS_LIST_REFERENCE = BITSTRING_STATUS_LIST_PREFIX + BITSTRING_STATUS_LIST_REFERENCE_LITERAL;
    private List<String> statusReference = new ArrayList<>();
    private String statusListPurpose;
    private int statusListIndex;
    private String statusListCredential;
    private int statusSize;
    private List<StatusMessage> statusMessage;

    private BitstringStatusListStatus() {

    }

    public static BitstringStatusListStatus parse(CredentialStatus status) {
        var instance = new BitstringStatusListStatus();

        instance.statusListCredential = ofNullable(getId(status))
                .map(Object::toString)
                .orElseThrow(() -> new IllegalArgumentException(missingProperty(STATUS_LIST_CREDENTIAL)));

        instance.statusListIndex = ofNullable(status.getProperty(BITSTRING_STATUS_LIST_PREFIX, BITSTRING_STATUS_LIST_INDEX_LITERAL))
                .map(Object::toString)
                .map(Integer::parseInt)
                .orElseThrow(() -> new IllegalArgumentException(missingProperty(STATUS_LIST_INDEX)));

        instance.statusListPurpose = ofNullable(status.getProperty(BITSTRING_STATUS_LIST_PREFIX, BITSTRING_STATUS_LIST_PURPOSE_LITERAL))
                .map(Object::toString)
                .orElseThrow(() -> new IllegalArgumentException(missingProperty(STATUS_LIST_PURPOSE)));

        var size = ofNullable(status.getProperty(BITSTRING_STATUS_LIST_PREFIX, STATUS_LIST_SIZE))
                .map(Object::toString)
                .map(Integer::parseInt);
        var statusMessages = status.getProperty(BITSTRING_STATUS_LIST_PREFIX, STATUS_LIST_MESSAGES);

        if (size.isEmpty() && statusMessages != null) {
            throw new IllegalArgumentException("statusSize must be specified and > 1 if statusMessage is present.");
        }
        instance.statusSize = size.orElse(1);

        if (instance.statusSize <= 0) {
            throw new IllegalArgumentException("If present, statusSize must be a positive integer > 0 but was '%d'.".formatted(instance.statusSize));
        }

        instance.statusMessage = extractStatusMessage(statusMessages, instance.statusSize);

        //noinspection unchecked
        instance.statusReference = ofNullable(status.getProperty(BITSTRING_STATUS_LIST_PREFIX, STATUS_LIST_REFERENCE))
                .map(obj -> (List<String>) obj)
                .orElse(Collections.emptyList());

        return instance;

    }

    private static List<StatusMessage> extractStatusMessage(Object statusMessageObject, int statusSize) {
        if (statusMessageObject == null) return List.of();
        //noinspection unchecked
        var msg = (List<StatusMessage>) (statusMessageObject);

        // explicitly check this, because statusSize = 1 and an empty message array would be acceptable
        if (statusSize > 1 && msg.isEmpty()) {
            throw new IllegalArgumentException("If statusSize is > 1, statusMessage must be present.");
        }
        if (Math.pow(2, statusSize) != msg.size()) {
            throw new IllegalArgumentException("If present, statusSize (in bits) must be equal to the number of possible statusMessage entries, e.g. statusSize = 3, statusMessage = 8 entries.");
        }

        return msg;
    }

    private static Object getId(CredentialStatus status) {
        var credentialId = status.getProperty(BITSTRING_STATUS_LIST_PREFIX, BITSTRING_STATUS_LIST_CREDENTIAL_LITERAL);
        if (credentialId instanceof Map<?, ?> map) {
            return map.get("@id");
        }
        return credentialId;
    }

    private static String missingProperty(String property) {
        return "A BitstringStatusList credential must have a credentialStatus object with the '%s' property".formatted(property);
    }

    public String getStatusListCredential() {
        return statusListCredential;
    }

    public int getStatusListIndex() {
        return statusListIndex;
    }

    public String getStatusListPurpose() {
        return statusListPurpose;
    }

    public List<StatusMessage> getStatusMessage() {
        return statusMessage;
    }

    public List<String> getStatusReference() {
        return statusReference;
    }

    public int getStatusSize() {
        return statusSize;
    }
}
