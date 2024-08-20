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

package org.eclipse.edc.iam.verifiablecredentials.spi.model.bitstringstatuslist;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;

import static org.eclipse.edc.iam.verifiablecredentials.spi.VcConstants.BITSTRING_STATUS_LIST_PREFIX;

public class BitstringStatusListCredential extends VerifiableCredential {
    public static final String BITSTRING_STATUS_LIST_CREDENTIAL_LITERAL = "statusListCredential";
    public static final String BITSTRING_STATUS_LIST_INDEX_LITERAL = "statusListIndex";
    public static final String BITSTRING_STATUS_LIST_PURPOSE_LITERAL = "statusPurpose";
    public static final String BITSTRING_STATUS_LIST_SIZE_LITERAL = "statusSize";
    public static final String BITSTRING_STATUS_LIST_MESSAGE_LITERAL = "statusMessage";
    public static final String BITSTRING_STATUS_LIST_REFERENCE_LITERAL = "statusReference";


    public static final String STATUS_LIST_CREDENTIAL = BITSTRING_STATUS_LIST_PREFIX + BITSTRING_STATUS_LIST_CREDENTIAL_LITERAL;
    public static final String STATUS_LIST_INDEX = BITSTRING_STATUS_LIST_PREFIX + BITSTRING_STATUS_LIST_INDEX_LITERAL;
    public static final String STATUS_LIST_PURPOSE = BITSTRING_STATUS_LIST_PREFIX + BITSTRING_STATUS_LIST_PURPOSE_LITERAL;
    public static final String STATUS_LIST_SIZE = BITSTRING_STATUS_LIST_PREFIX + BITSTRING_STATUS_LIST_SIZE_LITERAL;
    public static final String STATUS_LIST_MESSAGES = BITSTRING_STATUS_LIST_PREFIX + BITSTRING_STATUS_LIST_MESSAGE_LITERAL;
    public static final String STATUS_LIST_REFERENCE = BITSTRING_STATUS_LIST_PREFIX + BITSTRING_STATUS_LIST_REFERENCE_LITERAL;
    public static final String TYPE = "BitstringStatusListEntry";
}
